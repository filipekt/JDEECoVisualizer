package cz.filipekt.jdcv;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import javax.imageio.ImageIO;

import cz.filipekt.jdcv.CheckPoint.Type;
import cz.filipekt.jdcv.SceneBuilder.ImageProvider;
import cz.filipekt.jdcv.SceneBuilder.ShapeProvider;
import cz.filipekt.jdcv.ensembles.CoordinatorRelation;
import cz.filipekt.jdcv.ensembles.EnsembleDatabase;
import cz.filipekt.jdcv.ensembles.MembershipRelation;
import cz.filipekt.jdcv.events.EnsembleEvent;
import cz.filipekt.jdcv.network.MyLink;
import cz.filipekt.jdcv.network.MyNode;
import cz.filipekt.jdcv.plugins.InfoPanel;
import cz.filipekt.jdcv.prefs.PreferencesBuilder;

/**
 * The scene that the {@link Visualizer} makes a graphic view of. 
 * It contains the map, view parameters, event log etc.
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
public class MapScene {
	
	/**
	 * Contains the network nodes. Keys = node IDs, values = {@link MyNode} node representations.
	 */
	private final Map<String,MyNode> nodes;
	
	/**
	 * Contains the network links. Keys = link IDs, values = {@link MyLink} link representations.
	 */
	private final Map<String,MyLink> links;
	
	/**
	 * Maps visual representations of nodes to the corresponding parsed node XML elements.
	 */
	private final Map<Node,MyNode> circles = new HashMap<>();
	
	/**
	 * Maps visual representations of links to the corresponding parsed link XML elements.
	 */
	private final Map<Node,MyLink> lines = new HashMap<>();
	
	/**
	 * Minimal value of x-coordinate among all the nodes in {@link MapScene#nodes}
	 */
	private final double minx;
	
	/**
	 * Minimal value of y-coordinate among all the nodes in {@link MapScene#nodes}
	 */
	private final double miny;
	
	/**
	 * Maximal value of x-coordinate among all the nodes in {@link MapScene#nodes}
	 */
	private final double maxx;
	
	/**
	 * Maximal value of y-coordinate among all the nodes in {@link MapScene#nodes}
	 */
	private final double maxy;
	
	/**
	 * Factor by which the x-coordinate scale should be multiplied so that the network view
	 * fits nicely into the window of preferred width {@link MapScene#preferredMapWidth}.
	 */
	private final double widthFactor;
	
	/**
	 * Factor by which the y-coordinate scale should be multiplied so that the network view
	 * fits nicely into the window of preferred height {@link MapScene#preferredMapHeight}.
	 */
	private final double heightFactor;

	/**
	 * Radius (in pixels) of the {@link Shape} objects representing the network nodes.
	 */
	private final double nodeRadius = 4.0;
	
	/**
	 * Color of the {@link Shape} instances representing the network nodes.
	 */
	private final Paint nodeColor = Color.FIREBRICK;
	
	/**
	 * Zoom factor used to view the map, relative to the preferred size defined 
	 * by {@link MapScene#preferredMapWidth} and {@link MapScene#preferredMapHeight}.
	 */
	private double zoom = 1.0;

	/**
	 * @return Scrollable container for the network(map) components such as nodes and links.
	 * @see {@link MapScene#mapPane}
	 */
	ScrollPane getMapPane() {
		return mapPane;
	}	
	
	/**
	 * Interval (in miliseconds) at which a screenshot is taken when recording the visualization
	 */
	private final double recordingFreqency = 200;
	
	/**
	 * Directory to which the recorded images will be stored
	 */
	private File recordingDirectory;
	
	/**
	 * @param recordingDirectory Directory to which the recorded images will be stored
	 * @see {@link MapScene#recordingDirectory}
	 */
	public void setRecordingDirectory(File recordingDirectory) {
		this.recordingDirectory = recordingDirectory;
	}
	
	/**
	 * If true, the visualization is currently being recorded. If false, it is not.
	 */
	private boolean recordingInProgress = false;

	/**
	 * @param recordingInProgress If true, the visualization will from now be recorded. If false, it will not.
	 * @see {@link MapScene#recordingInProgress}
	 */
	public void setRecordingInProgress(boolean recordingInProgress) {
		this.recordingInProgress = recordingInProgress;
	}

	/**
	 * @return If true, the visualization is currently being recorded. If false, it is not.
	 */
	public boolean isRecordingInProgress() {
		return recordingInProgress;
	}
	
	/**
	 * Makes sure that the {@link KeyFrame} instances allowing for recording of
	 * snapshots are added to the keyframe list of {@link MapScene#timeLine}.
	 */
	private void addRecordingFrames(){
		if (!recordingFramesAdded){			
			List<KeyFrame> frames = createRecordingFrames();
			timeLine.getKeyFrames().addAll(frames);
			recordingFramesAdded = true;
		}
	}
	
	/**
	 * After recording of snapshots has been stopped, this method is called
	 * to flush the recorded snapshots (stored in {@link MapScene#recordedFrames})
	 * to disk.
	 */
	public void flushRecordedFrames(){
		//TODO use a single ImageWriter for all images
		if (recordedFrames.size() > 0){
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					BufferedImage bim;
					File file;
					int i = 0;
					DateFormat df = new SimpleDateFormat("yyyy-MMM-dd-HH-mm-ss");
					String datePrefix = df.format(new Date());
					for (WritableImage wi : recordedFrames){
						bim = SwingFXUtils.fromFXImage(wi, null);
						file = new File(recordingDirectory, datePrefix + "_" + i + ".png");
						try {
							ImageIO.write(bim, "png", file);
						} catch (IOException ex) {}
						i += 1;
					}
					recordedFrames.clear();
				}
			}).start();
			
		}
	}
	
	/**
	 * Temporary storage for recorded snapshots when recording is underway.
	 */
	private List<WritableImage> recordedFrames = new ArrayList<>();
	
	/**
	 * If true, the {@link KeyFrame} instances produced by {@link MapScene#createRecordingFrames()}
	 * have already been added to the list of keyframes of {@link MapScene#timeLine}.
	 */
	private boolean recordingFramesAdded = false;
	
	/**
	 * @return {@link KeyFrame} instances that are later inserted into the {@link MapScene#timeLine} 
	 * keyframes, to allow possible recording requests.
	 */
	private List<KeyFrame> createRecordingFrames(){
		double totalTime = timeLine.getTotalDuration().toMillis();
		List<KeyFrame> res = new ArrayList<>();
		int count = (int)Math.floor(totalTime / recordingFreqency);
		for (int i = 0; i<count; i++){
			double time = recordingFreqency * i;
			Duration timeVal = new Duration(time);
			KeyFrame frame = new KeyFrame(timeVal, new RecordingAction());
			res.add(frame);
		}		
		return res;
	}
	
	/**
	 * Called whenever the visualization is being recorded and the {@link MapScene#recordingFreqency}
	 * interval just passed. Saves a current snapshot into {@link MapScene#recordedFrames}.
	 */
	private class RecordingAction implements EventHandler<ActionEvent>{

		@Override
		public void handle(ActionEvent event) {
			if (recordingInProgress){
				WritableImage image = mapContainer.snapshot(null, null);
				recordedFrames.add(image);
			}
		}
		
	}

	/**
	 * Determines the minimal and maximal x,y coordinates across all of the map nodes.
	 * @return Array consisting of (minimal x, minimal y, maximal x, maximal y) coordinates of map nodes.
	 */
	private double[] getMapBorders(){
		double minx = Double.MAX_VALUE;
		double miny = Double.MAX_VALUE;
		double maxx = 0.0;
		double maxy = 0.0;
		for (MyNode node : nodes.values()){
			if (node.getX() < minx){
				minx = node.getX();
			}
			if (node.getX() > maxx){
				maxx = node.getX();
			}
			if (node.getY() < miny){
				miny = node.getY();
			}
			if (node.getY() > maxy){
				maxy = node.getY();
			}
		}
		return new double[]{minx, miny, maxx, maxy};
	}
	
	/**
	 * Constructs the visualizations of the map nodes. Makes sure that proper
	 * magnification and metric is used.
	 * @return The visualizations of the map nodes mapped to corresponding node 
	 * element representations.
	 */
	private Map<Shape,MyNode> generateCircles(){
		Map<Shape,MyNode> res = new HashMap<>();
		for (MyNode node : nodes.values()){
			double x = node.getX();
			x -= minx;
			x *= (widthFactor * zoom);
			x += (constantMargin / 2);
			double y = node.getY();
			y -= miny;
			y *= (heightFactor * zoom);
			y += (constantMargin / 2);
			final Circle circle = new Circle(x, y, nodeRadius, nodeColor);
			circle.setEffect(new BoxBlur());
			res.put(circle, node);
			
			final Map<String,String> data = new LinkedHashMap<>();
			data.put("Node ID", node.getId());
			data.put("x-coordinate", node.getX() + "");
			data.put("y-coordinate", node.getY() + "");
			circle.setOnMouseClicked(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent arg0) {
					InfoPanel.getInstance().setInfo("Node selected:", data);
				}
			});
			circle.setOnMouseEntered(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent arg0) {
					circle.setRadius(nodeRadius * 2);
				}
			});
			circle.setOnMouseExited(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent arg0) {
					circle.setRadius(nodeRadius);
				}
			});
		}
		return res;
	}
	
	/**
	 * Constructs the visualizations of the map links. Makes sure that proper
	 * magnification and metric is used.
	 * @return The visualizations of the map links mapped to corresponding link 
	 * element representations.
	 */
	private Map<Shape,MyLink> generateLines(){
		Map<Shape,MyLink> res = new HashMap<>();
		for (MyLink link : links.values()){
			double fromx = link.getFrom().getX();
			fromx = transformX(fromx);
			double fromy = link.getFrom().getY();
			fromy = transformY(fromy);
			double tox = link.getTo().getX();
			tox = transformX(tox);
			double toy = link.getTo().getY();
			toy = transformY(toy);
			final Line line = new Line(fromx, fromy, tox, toy);
			line.setStroke(linkDefaultColor);
			line.setStrokeWidth(linkWidth);		
			res.put(line, link);
			
			final Map<String,String> data = new LinkedHashMap<>();
			data.put("Link ID", link.getId());
			data.put("From Node", link.getFrom().getId());
			data.put("From x-coordinate", Double.toString(link.getFrom().getX()));
			data.put("From y-coordinate", Double.toString(link.getFrom().getY()));
			data.put("To Node", link.getTo().getId());
			data.put("To x-coordinate", Double.toString(link.getTo().getX()));
			data.put("To y-coordinate", Double.toString(link.getTo().getY()));
			line.setOnMouseClicked(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent arg0) {
					InfoPanel.getInstance().setInfo("Link Selected:", data);
				}
			});
			line.setOnMouseEntered(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent arg0) {
					line.setStrokeWidth(linkWidth * 3);
				}
			});
			line.setOnMouseExited(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent arg0) {
					line.setStrokeWidth(linkWidth);
				}
			});
		}
		return res;
	}
	
	/**
	 * Width of the {@link Line} instances that represent links, in pixels.
	 */
	private final double linkWidth = 2.0;
	
	/**
	 * Default color of the {@link Line} instances that represent links
	 */
	private final Paint linkDefaultColor = Color.SILVER;
	
	/**
	 * @param x An x-coordinate as given in an XML element such event, link, etc. 
	 * @return The x-coordinate converted to the value used in the map visualization, where the
	 * coordinates correspond to the actual pixels on the screen (before zooming). 
	 */
	private double transformX(double x){
		x -= minx;
		x *= (widthFactor * zoom);
		x += (constantMargin / 2);
		return x;
	}
	
	/**
	 * @param y A y-coordinate as given in an XML element such event, link, etc.
	 * @return The y-coordinate converted to the value used in the map visualization, where the
	 * coordinates correspond to the actual pixels on the screen (before zooming). 
	 */
	private double transformY(double y){
		y -= miny;
		y *= (heightFactor * zoom);
		y += (constantMargin / 2);
		return y;
	}
	
	/**
	 * The simulation time at which we start the visualization
	 */
	private final double minTime;
	
	/**
	 * The simulation time at which we end the visualization
	 */
	private final double maxTime;
	
	/**
	 * The actual intended duration of the visualization (i.e. in visualization time)
	 */
	private final int duration;
	
	/**
	 * Converter from visualization to simulation time
	 * @param visualizationTime A time in visualization time format
	 * @return The time in simulation time format 
	 */
	public double convertToSimulationTime(double visualizationTime){
		double ratio = duration * 1000 / (maxTime - minTime);
		return (visualizationTime / ratio) + minTime;
	}
	
	/**
	 * Converter from simulation to visualization time
	 * @param simulationTime A time in simulation time format
	 * @return The time in visualization time format
	 */
	public double convertToVisualizationTime(double simulationTime){
		double ratio = duration * 1000 / (maxTime - minTime);
		double diff = simulationTime - minTime;
		return diff * ratio;
	}
	
	/**
	 * Maps each person's ID to the person's graphical representation
	 */
	private final Map<String,Node> personShapes = new HashMap<>();
	
	/**
	 * Radius of the circle representing a person in the visualization
	 */
	private final double personCircleRadius = 2.5;
	
	/**
	 * @return Radius of the circle representing a person in the visualization
	 * @see {@link MapScene#personCircleRadius}
	 */
	double getPersonCircleRadius() {
		return personCircleRadius;
	}
	
	/**
	 * Both width and height of the image that represents a person/car in the visualization
	 */
	private final double personImageWidth = 8 * personCircleRadius;
	
	/**
	 * Color of the circle representing a person in the visualization
	 */
	private final Paint personCircleColor = Color.LIME;
	
	/**
	 * @return Color of the circle representing a person in the visualization
	 * @see {@link MapScene#personCircleColor}
	 */
	Paint getPersonCircleColor() {
		return personCircleColor;
	}

	/**
	 * Timeline used for animation of the simulation output 
	 */
	private final Timeline timeLine = new Timeline();

	/**
	 * @return Timeline used for animation of the simulation output
	 * @see {@link Visualizer#timeLine}
	 */
	public Timeline getTimeLine() {
		return timeLine;
	}
	
	/**
	 * Updates the collections of node instances that represent the map elements,
	 * both mobile (agents, ensemble memberships) and immobile (nodes,links).
	 * Also, the {@link MapScene#mapContainer} holding these {@link Shape} instances for
	 * visualizing purposes is updated with the new values.
	 * 
	 * @param shapeProvider Used for generating the visualizations of people
	 * @param justMovables If true, only the moveable objects (people,ensembles) will be updated 
	 * @param selectedPeople People whose visualizations will be updated
	 * @throws IOException  When a person shape could not be loaded for any reason
	 */
	public void update(ShapeProvider shapeProvider, boolean justMovables, String[] selectedPeople) throws IOException{
		timeLine.stop();
		timeLine.getKeyFrames().clear();
		mapContainer.getChildren().clear();
		produceShapes(shapeProvider, selectedPeople);
		addRecordingFrames();
		if (!justMovables){
			Map<Shape,MyNode> newCircles = generateCircles();
			Map<Shape,MyLink> newLines = generateLines();
			circles.clear();
			circles.putAll(newCircles);
			lines.clear();
			lines.putAll(newLines);
		}
		mapContainer.getChildren().addAll(lines.keySet());
		mapContainer.getChildren().addAll(circles.keySet());
		mapContainer.getChildren().addAll(personShapes.values());
		mapContainer.getChildren().addAll(ensembleShapes.values());
	    moveShapesToFront();	  
	    
	}
	
	/**
	 * Sets the visibility of the {@link Shape} instances representing the map nodes,
	 * depending on the value of the parameter.
	 * @param visible If true, it makes the {@link Shape} instances representing the map nodes visible.
	 * Otherwise it makes them invisible.
	 */
	void setNodesVisible(boolean visible){
		for (Node node : circles.keySet()){
			node.setVisible(visible);
		}
	}
	
	/**
	 * Sets the visibility of the {@link Shape} instances representing the map links,
	 * depending on the value of the parameter.
	 * @param visible If true, it makes the {@link Shape} instances representing the map links visible.
	 * Otherwise it makes them invisible.
	 */
	void setLinksVisible(boolean visible){
		for (Node link : lines.keySet()){
			link.setVisible(visible);
		}
	}
	
	/**
	 * Ensures that the {@link Shape} instances making up the map are drawn in the correct
	 * background-foreground manner, for example that there is no incorrect overlapping 
	 */
	private void moveShapesToFront(){
		for (Node node : circles.keySet()){	
			node.toFront();
		}
		for (Node person : personShapes.values()){
			person.toFront();
		}
	}
	
	/**
	 * Zooms in or zooms out the map view.
	 * @param factor By this value the current zoom factor {@link MapScene#zoom} will be multiplied.
	 */
	public void changeZoom(double factor){
		zoom *= factor;				
		Scale scale = new Scale(factor, factor, 0, 0);		
		mapContainer.getTransforms().add(scale);
		mapContainer.setPrefHeight(mapContainer.getPrefHeight() * factor);
		mapContainer.setPrefWidth(mapContainer.getPrefWidth() * factor);
	}
	
	/**
	 * Container for the {@link Shape} instances that represent the map elements, 
	 * such as links, nodes, vehicles.
	 */
	private final Pane mapContainer = new Pane();
	
	/**
	 * @return Container for the {@link Shape} instances that represent the map elements, 
	 * such as links, nodes, vehicles.
	 * @see {@link MapScene#mapContainer}
	 */
	public Pane getMapContainer() {
		return mapContainer;
	}
	
	/**
	 * Scrollable container for {@link MapScene#mapContainer}
	 */
	private final ScrollPane mapPane = new ScrollPane(mapContainer);

	/**
	 * Double of the width of the white margin that is added on each side of the map.
	 */
	private final double constantMargin = 25.0;
	
	/**
	 * Maps each ensemble membership relation to the graphical representation of this relation.
	 */
	private final Map<MembershipRelation,Node> ensembleShapes = new HashMap<>();
	
	/**
	 * Fills the timeline with keyframes that enable the right movements of 
	 * people and ensembles visualizations. On top of that, it updates the 
	 * collections containing the visualizations of the map elements
	 * @param shapeProvider Used for generating the visualizations of people
	 * @param selectedPeople People whose visualizations will be updated
	 * @throws IOException When a person shape could not be loaded for any reason
	 */
	private void produceShapes(ShapeProvider shapeProvider, String[] selectedPeople) throws IOException{
		Collection<KeyFrame> keyFrames = buildFramesForPeople(shapeProvider, selectedPeople);
		Collection<KeyFrame> keyFrames2 = buildFramesForEnsembles();
		timeLine.getKeyFrames().addAll(keyFrames);
		timeLine.getKeyFrames().addAll(keyFrames2);
	}
	
	/**
	 * Given a person ID and the checkpoints associated with the person's movement, this method
	 * creates a collection of key-value pairs for use by the info side-panel, with each pair
	 * containing a piece of relevant information about the person.
	 * @param personID ID of the person
	 * @param checkPoints Checkpoints associated with the person, describing its movements
	 * @return Collection of key-value pairs for use by the info side-panel
	 */
	private Map<String,String> getInfoForPerson(String personID, List<CheckPoint> checkPoints){
		Map<String,String> res = new LinkedHashMap<>();
		res.put("Person ID", personID);
		for (CheckPoint cp : checkPoints){
			String key = "Time " + cp.getTime();
			String value = null;
			switch (cp.getType()){
				case PERSON_ENTERS:
					value = "person enters vehicle";
					break;
				case PERSON_LEAVES:
					value = "persons leaves vehicle";
					break;
				case POSITION_DEF:
					value = "location is x=" + cp.getX() + ", y=" + cp.getY();
					break;
			}
			if (value != null){
				res.put(key, value);
			}
		}
		return res;
	}
	
	/**
	 * Using the {@link MapScene#checkpointDb}, this method converts its contents into the format
	 * specified by JavaFX {@link Timeline} animation model. The people shapes collection is 
	 * filled with the individual nodes that represent the persons.  
	 * @param shapeProvider Used for generating the visualizations of people
	 * @param selectedPeople People whose visualizations will be updated
	 * @return {@link KeyFrame} instances describing the movements of people on the map.
	 * @throws IOException When a person shape could not be loaded for any reason
	 */
	private Collection<KeyFrame> buildFramesForPeople(SceneBuilder.ShapeProvider shapeProvider, String[] selectedPeople) throws IOException{
		Collection<KeyFrame> frames = new ArrayList<>();
		Collection<String> selectedPeopleCol = null;
		if ((selectedPeople != null) && (selectedPeople.length != 0)){
			selectedPeopleCol = Arrays.<String>asList(selectedPeople);
		}
		for (String personID : checkpointDb.getKeys()){
			if ((selectedPeople == null) || (selectedPeople.length == 0) || (selectedPeopleCol.contains(personID))){
				Collection<KeyFrame> personFrames = new ArrayList<>();
				List<CheckPoint> checkPoints = checkpointDb.getList(personID);
				Node personShape = buildPersonShape(checkPoints, shapeProvider);
				if (personShape == null){
					continue;
				}
				final Map<String,String> personInfo = getInfoForPerson(personID, checkPoints);
				personShape.setOnMouseClicked(new EventHandler<MouseEvent>() {
	
					@Override
					public void handle(MouseEvent arg0) {
						InfoPanel.getInstance().setInfo("Person/car selected:", personInfo);
					}
				});
				personShape.setVisible(false);
				DoubleProperty xProperty = personShape.translateXProperty();
				DoubleProperty yProperty = personShape.translateYProperty();
				BooleanProperty visibleProperty = personShape.visibleProperty();
				KeyValue initX = new KeyValue(xProperty, xProperty.get());
				KeyValue initY = new KeyValue(yProperty, yProperty.get());
				KeyValue initVis = new KeyValue(visibleProperty, false);
				personFrames.add(new KeyFrame(Duration.ZERO, initX, initY, initVis));
				for (CheckPoint cp : checkPoints){				
					Duration actualTime = new Duration(convertToVisualizationTime(cp.getTime()));
					KeyFrame frame = null;
					if (cp.getType().equals(Type.POSITION_DEF)){
						double actualX = transformX(cp.getX());
						double actualY = transformY(cp.getY());					
						KeyValue xVal = new KeyValue(xProperty, actualX);
						KeyValue yVal = new KeyValue(yProperty, actualY);
						KeyValue visibleVal = new KeyValue(visibleProperty, true);
						frame = new KeyFrame(actualTime, xVal, yVal, visibleVal);					
					} else if (cp.getType().equals(Type.PERSON_ENTERS) || cp.getType().equals(Type.PERSON_LEAVES)){
						boolean personEnters = cp.getType().equals(Type.PERSON_ENTERS);
						KeyValue visibleVal = new KeyValue(visibleProperty, personEnters);
						frame = new KeyFrame(actualTime, visibleVal);
					} else {
						throw new UnsupportedOperationException();
					}
					personFrames.add(frame);
				}
				personShapes.put(personID, personShape);
				frames.addAll(personFrames);
				keyFramesForPeople.put(personID, personFrames);
			} else {
				frames.addAll(keyFramesForPeople.get(personID));
			}
		}
		return frames;
	}
	
	/**
	 * For each person ID, stores a collection of all the key frames that capture the
	 * movements of the person visualization
	 */
	private final Map<String,Collection<KeyFrame>> keyFramesForPeople = new HashMap<>();

	/**
	 * Builds a {@link Node} that represents a moving person/vehicle on the map.
	 * @param checkPoints Checkpoints of the person/vehicle, parsed from the event log.
	 * @param provider Used for generating the visualizations of people
	 * @return A {@link Shape} that represents a moving person/vehicle on the map.
	 * @throws IOException When the shape could not be loaded for any reason
	 */
	private Node buildPersonShape(List<CheckPoint> checkPoints, SceneBuilder.ShapeProvider provider) throws IOException{
		double x = Double.MIN_VALUE;
		double y = Double.MIN_VALUE;
		for (CheckPoint cp : checkPoints){
			if (cp.getType().equals(Type.POSITION_DEF)){
				x = cp.getX();
				y = cp.getY();
				break;
			}
		}
		if (x == Double.MIN_VALUE){
			return null;
		} else {
			Node shape = provider.getNewShape();
			if (shape != null){
				shape.setTranslateX(transformX(x));
				shape.setTranslateY(transformY(y));
			}
			return shape;
		}
	}
	
	/**
	 * Contains methods to retrieve preferences objects for various elements of simulated situation
	 * @see {@link PreferencesBuilder}
	 * @see {@link MapScene#getPreferences()}
	 */
	private final PreferencesBuilder preferences = new PreferencesBuilder(circles, lines, ensembleShapes);
	
	/**
	 * @return A valid instance of the object providing access to the preferences objects.
	 * Contains methods to retrieve preferences objects for various elements of simulated situation.
	 */
	public PreferencesBuilder getPreferences(){
		return preferences;
	}
	
	/**
	 * Holds the colors used for ensemble memberships
	 */
	private final Map<CoordinatorRelation,Paint> ensembleColors = new HashMap<>();

	/**
	 * Given all the ensemble events, this method creates their graphical representations in the form of 
	 * JavaFX nodes and prepares the correct movements of these nodes by binding them in the right way
	 * to the movements of the corresponding coordinators and members.  
	 * @return These key frames capture the varying visibility of the ensemble membership representations,
	 * as they disappear whenever the membership condition ceases to hold and vice versa.
	 */
	private Collection<KeyFrame> buildFramesForEnsembles(){
		Collection<KeyFrame> res = new ArrayList<>();
		EnsembleDatabase edb = new EnsembleDatabase(ensembleColors);
		for (EnsembleEvent eev : ensembleEvents){
			double timeVal = convertToVisualizationTime(eev.getTime());
			Duration time = new Duration(timeVal);
			final String coord = eev.getCoordinator();
			Node coordShape = personShapes.get(coord);
			final String member = eev.getMember();
			Node memberShape = personShapes.get(member);
			if ((coordShape != null) && (memberShape != null)){  						
				Node ensembleShape = edb.getEnsembleShape(eev.getEnsemble(), coord, member, coordShape, memberShape);				
				KeyValue kv = new KeyValue(ensembleShape.visibleProperty(), eev.getMembership());
				KeyFrame kf = new KeyFrame(time, kv);
				res.add(kf);
			} else {
				/*
				Some of the agents had no corresponding event in the interval of matsim event log which 
				we loaded so we don't know where the agent is on the map and we move on.
				*/				 
			}
		}
		ensembleShapes.clear();
		ensembleShapes.putAll(edb.getEnsembleShapes());
		for (Node line : ensembleShapes.values()){
			KeyValue kv = new KeyValue(line.visibleProperty(), Boolean.FALSE);
			KeyFrame kf = new KeyFrame(Duration.ZERO, kv);
			res.add(kf);
		}
		return res;
	}

	/**
	 * The checkpoints (position of people) as encountered when 
	 * parsing the input XML files. Contains positions of people 
	 * on the map at specified times.
	 */
	private final CheckPointDatabase checkpointDb;
	
	/**
	 * The ensemble events as parsed from the ensemble event log file.
	 */
	private final List<EnsembleEvent> ensembleEvents;
	
	/**
	 * Changes the image which represents each person in the visualization
	 * @param imageName Name of the new image, or (if the next parameter is false) a path to the image. 
	 * @param isResource If true, the previous parameter specifies a resource name, else it specifies
	 * a path to a file.
	 * @param selectedPeople People whose visualizations will be updated
	 * @throws IOException When the specified image couldn't be found or read from
	 */
	public void changePeopleImage(String imageName, boolean isResource, String[] selectedPeople) throws IOException{
		if (timeLine != null){
			Duration time = timeLine.getCurrentTime();
			Status status = timeLine.getStatus();
			timeLine.stop();
			ShapeProvider provider;
			if (imageName == null){
				provider = new SceneBuilder.CircleProvider(personCircleRadius, personCircleColor);
			} else {
				provider = new ImageProvider(isResource, imageName, personImageWidth);
			}
			update(provider, false, selectedPeople);
			if (status == Status.RUNNING){
				timeLine.playFrom(time);
			} else if (status == Status.PAUSED){
				timeLine.playFrom(time);
				timeLine.pause();
			} else {
				timeLine.stop();			
			}
		}
	}
	
	/**
	 * @param nodes The network nodes. Keys = node IDs, values = {@link MyNode} node representations.
	 * @param links The network links. Keys = link IDs, values = {@link MyLink} link representations.
	 * @param mapWidth Preferred width of the map view, in pixels
	 * @param mapHeight Preferred height of the map view, in pixels
	 * @param timeLineStatus Called whenever the visualization is started, paused or stopped
	 * @param timeLineRate Called whenever the visualization is sped up or down
	 * @param minTime The simulation time at which we start the visualization
	 * @param maxTime The simulation time at which we end the visualization
	 * @param duration The actual intended duration of the visualization (i.e. in visualization time)
	 * @param checkpointDb The checkpoints (position of people) as encountered when 
	 * parsing the input XML files. Contains positions of people on the map at specified times.
	 * @param ensembleEvents The ensemble events as parsed from the ensemble event log file.
	 */
	MapScene(Map<String,MyNode> nodes, Map<String,MyLink> links, double mapWidth, double mapHeight,  
			ChangeListener<? super Status> timeLineStatus, ChangeListener<? super Number> timeLineRate,
			double minTime, double maxTime, int duration, CheckPointDatabase checkpointDb, 
			List<EnsembleEvent> ensembleEvents) {
		this.checkpointDb = checkpointDb;
		this.ensembleEvents = ensembleEvents;
		this.minTime = minTime;
		this.maxTime = maxTime;
		this.duration = duration;
		this.nodes = nodes;
		this.links = links;
		double[] borders = getMapBorders();
		this.minx = borders[0];
		this.miny = borders[1];
		this.maxx = borders[2];
		this.maxy = borders[3];
		widthFactor = (mapWidth - constantMargin) / (maxx - minx);
		heightFactor = (mapHeight - constantMargin) / (maxy - miny);
		mapContainer.setPrefSize(mapWidth, mapHeight);
		mapContainer.setId("mapContainer");
		timeLine.statusProperty().addListener(timeLineStatus);
		timeLine.rateProperty().addListener(timeLineRate);
	}

}
