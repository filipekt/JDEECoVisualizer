package cz.filipekt.jdcv;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import cz.filipekt.jdcv.network.MyLink;
import cz.filipekt.jdcv.network.MyNode;

/**
 * The scene that the {@link Visualizer} will visualize. It contains the map, view parameters, event log etc.
 */
class MapScene {
	
	/**
	 * Contains the network nodes. Keys = node IDs, values = {@link MyNode} node representations.
	 */
	private final Map<String,MyNode> nodes;
	
	/**
	 * Contains the network links. Keys = link IDs, values = {@link MyLink} link representations.
	 */
	private final Map<String,MyLink> links;
	
	/**
	 * Maps {@link Circle} to {@link MyNode}. Each {@link Circle} represents a view of the 
	 * corresponding {@link MyNode}.
	 */
	private final Map<Node,MyNode> circles = new HashMap<>();
	
	/**
	 * Maps {@link Line} to {@link MyLink}. Each {@link Line} represents a view of the 
	 * corresponding {@link MyLink}.
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
	 * Radius (in pixels) of the {@link Circle} objects representing the network nodes.
	 */
	private final double nodeRadius = 4.0;
	
	/**
	 * Color of the {@link Circle} objects representing the network nodes.
	 */
	private final Paint nodeColor = Color.FIREBRICK;
	
	/**
	 * Zoom factor used to view the map, relative to the preferred size defined 
	 * by {@link MapScene#preferredMapWidth} and {@link MapScene#preferredMapHeight}.
	 */
	private double zoom = 1.0;
	
	/**
	 * Scrollable container for {@link MapScene#mapContainer}
	 */
	private final ScrollPane mapPane;

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
	void setRecordingDirectory(File recordingDirectory) {
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
	void setRecordingInProgress(boolean recordingInProgress) {
		this.recordingInProgress = recordingInProgress;
	}

	/**
	 * @return If true, the visualization is currently being recorded. If false, it is not.
	 */
	boolean isRecordingInProgress() {
		return recordingInProgress;
	}
	
	/**
	 * Makes sure that the {@link KeyFrame} instances allowing for recording of
	 * snapshots are added to the keyframe list of {@link MapScene#timeLine}.
	 */
	void addRecordingFrames(){
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
	void flushRecordedFrames(){
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
	List<WritableImage> recordedFrames = new ArrayList<>();
	
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
	 * After some changes have been made to {@link Visualizer#nodes}, {@link Visualizer#zoom} or
	 * other fields that have influence on the map view, call this method to get an updated
	 * collection of the {@link Circle} objects. These represent the {@link cz.filipekt.jdcv.network.MyNode}
	 * objects for purposes of visualization.
	 * @return Updated collection of {@link Circle} objects, representing the 
	 * {@link cz.filipekt.jdcv.network.MyNode} objects for purposes of visualization. 
	 */
	private Map<Node,MyNode> generateCircles(){
		Map<Node,MyNode> res = new HashMap<>();
		for (MyNode node : nodes.values()){
			double x = node.getX();
			x -= minx;
			x *= (widthFactor * zoom);
			x += (constantMargin / 2);
			double y = node.getY();
			y -= miny;
			y *= (heightFactor * zoom);
			y += (constantMargin / 2);
			Circle circle = new Circle(x, y, nodeRadius, nodeColor);
			circle.setEffect(new BoxBlur());
			res.put(circle, node);
		}
		return res;
	}
	
	/**
	 * After some changes have been made to {@link Visualizer#links}, {@link Visualizer#zoom} or
	 * other fields that have influence on the map view, call this method to get an updated
	 * collection of the {@link Line} objects. These represent the {@link cz.filipekt.jdcv.network.MyLink}
	 * objects for purposes of visualization.
	 * @return Updated collection of {@link Line} objects, representing the 
	 * {@link cz.filipekt.jdcv.network.MyLink} objects for purposes of visualization. 
	 */
	private Map<Node,MyLink> generateLines(){
		Map<Node,MyLink> res = new HashMap<>();
		for (MyLink link : links.values()){
			double fromx = link.getFrom().getX();
			fromx = transformX(fromx);
			double fromy = link.getFrom().getY();
			fromy = transformY(fromy);
			double tox = link.getTo().getX();
			tox = transformX(tox);
			double toy = link.getTo().getY();
			toy = transformY(toy);
			Line line = new Line(fromx, fromy, tox, toy);
			line.setStroke(linkDefaultColor);
			line.setStrokeWidth(linkWidth);		
			res.put(line, link);
		}
		return res;
	}
	
	/**
	 * Width of the {@link Line} instances that represent links, in pixels.
	 */
	private final double linkWidth = 1.5;
	
	/**
	 * Default color of the {@link Line} instances that represent links
	 */
	private final Paint linkDefaultColor = Color.SILVER;
	
	/**
	 * @param x An x-coordinate as given in an XML element such event, link, etc. 
	 * @return The x-coordinate converted to the value used in the map visualization, where the
	 * coordinates correspond to the actual pixels on the screen (before zooming). 
	 */
	double transformX(double x){
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
	double transformY(double y){
		y -= miny;
		y *= (heightFactor * zoom);
		y += (constantMargin / 2);
		return y;
	}
	
	/**
	 * {@link Shape} instances representing individual people (as they move through the map).
	 */
	private final Set<Shape> personShapes;
	
	/**
	 * Radius (in pixels) of the {@link Circle} objects representing the people on the map.
	 */
	private final double personRadius = 1.5;
	
	/**
	 * @return Radius (in pixels) of the {@link Circle} objects representing the people on the map.
	 * @see {@link MapScene#personRadius}
	 */
	double getPersonRadius() {
		return personRadius;
	}
	
	/**
	 * Color of the {@link Circle} objects representing the people on the map.
	 */
	private final Paint personColor = Color.LIME;
	
	/**
	 * @return Color of the {@link Circle} objects representing the people on the map.
	 * @see {@link MapScene#personColor}
	 */
	Paint getPersonColor() {
		return personColor;
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
	 * Updates the collections of {@link Shape} instances that represent the map elements,
	 * both mobile (vehicles) and immobile (nodes,links).
	 * Also, the {@link MapScene#mapContainer} holding these {@link Shape} instances for
	 * visualizing purposes is updated with the new values.
	 */
	void update(){
		Map<Node,MyNode> newCircles = generateCircles();
		Map<Node,MyLink> newLines = generateLines();
		circles.clear();
		circles.putAll(newCircles);
		lines.clear();
		lines.putAll(newLines);
		mapContainer.getChildren().clear();
		mapContainer.getChildren().addAll(lines.keySet());
		mapContainer.getChildren().addAll(circles.keySet());
		mapContainer.getChildren().addAll(personShapes);
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
		for (Node node : lines.keySet()){
			node.setVisible(visible);
		}
	}
	
	/**
	 * Ensures that the {@link Shape} instances making up the map are drawn in the correct
	 * background-foreground manner, for example that there is no incorrect overlapping 
	 */
	private void moveShapesToFront(){
		for (Node node : circles.keySet()){
			((Circle)node).toFront();
		}
		for (Shape person : personShapes){
			person.toFront();
		}
	}
	
	/**
	 * Zooms in or zooms out the map view.
	 * @param factor By this value the current zoom factor {@link MapScene#zoom} will be multiplied.
	 */
	void changeZoom(double factor){
		zoom *= factor;				
		Scale scale = new Scale(factor, factor, 0, 0);		
		mapContainer.getTransforms().add(scale);
		mapContainer.setPrefHeight(mapContainer.getPrefHeight() * factor);
		mapContainer.setPrefWidth(mapContainer.getPrefWidth() * factor);
	}
	
	/**
	 * Container for the {@link Node} instances that represent the map elements, 
	 * such as links, nodes, vehicles.
	 */
	private final Pane mapContainer = new Pane();
	
	/**
	 * @return Container for the {@link Node} instances that represent the map elements, 
	 * such as links, nodes, vehicles.
	 * @see {@link MapScene#mapContainer}
	 */
	Pane getMapContainer() {
		return mapContainer;
	}

	/**
	 * Double of the width of the white margin that is added on each side of the map.
	 */
	private final double constantMargin = 25.0;
	
	/**
	 * @param nodes The network nodes. Keys = node IDs, values = {@link MyNode} node representations.
	 * @param links The network links. Keys = link IDs, values = {@link MyLink} link representations.
	 * @param mapWidth Preferred width of the map view, in pixels
	 * @param mapHeight Preferred height of the map view, in pixels
	 * @param shapes {@link Shape} instances representing individual people
	 */
	MapScene(Map<String,MyNode> nodes, Map<String,MyLink> links, double mapWidth, double mapHeight, 
			Set<Shape> shapes, ChangeListener<? super Status> timeLineStatus, ChangeListener<? super Number> timeLineRate) {
		this.personShapes = shapes;
		this.nodes = nodes;
		this.links = links;
		double[] borders = getMapBorders();
		this.minx = borders[0];
		this.miny = borders[1];
		this.maxx = borders[2];
		this.maxy = borders[3];
		widthFactor = (mapWidth - constantMargin) / (maxx - minx);
		heightFactor = (mapHeight - constantMargin) / (maxy - miny);
		mapPane = new ScrollPane();		
		mapPane.setContent(mapContainer);
		mapContainer.setPrefSize(mapWidth, mapHeight);
		mapContainer.setId("mapContainer");
		timeLine.statusProperty().addListener(timeLineStatus);
		timeLine.rateProperty().addListener(timeLineRate);
	}

}
