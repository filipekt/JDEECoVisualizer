package cz.filipekt.jdcv;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import cz.filipekt.jdcv.CheckPoint.Type;
import cz.filipekt.jdcv.events.EnsembleEvent;
import cz.filipekt.jdcv.events.EnteredOrLeftLink;
import cz.filipekt.jdcv.events.EntersOrLeavesVehicle;
import cz.filipekt.jdcv.events.EventType;
import cz.filipekt.jdcv.events.MatsimEvent;
import cz.filipekt.jdcv.network.MyLink;
import cz.filipekt.jdcv.network.MyNode;
import cz.filipekt.jdcv.xml.EnsembleHandler;
import cz.filipekt.jdcv.xml.EventWithPersonHandler;
import cz.filipekt.jdcv.xml.LinkHandler;
import cz.filipekt.jdcv.xml.NodeHandler;

/**
 * Listener for the event that the user requests a visualization scene to be
 * created. Makes sure that when the OK button (on the import-scene page) is 
 * clicked, the input data is processed and the requested visualization is shown.
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
class SceneBuilder implements EventHandler<javafx.event.Event>{
	
	/**
	 * Text fields containing the paths to the source XML files.
	 */
	private final List<TextField> fields;
	
	/**
	 * The {@link Button} with which this {@link EventHandler} is associated. 
	 */
	private final Button okButton;
	
	/**
	 * The {@link CheckBox} that allows the user to select whether to show just those
	 * elements in the map that correspond to the injected JDEECo components 
	 */
	private final CheckBox onlyComponentsBox;
	
	/**
	 * Shown when a new scene is being loaded.
	 */
	private final ProgressIndicator progIndicator = new ProgressIndicator(-1);
	
	/**
	 * The {@link GridPane} which contains {@link SceneBuilder#progIndicator} 
	 * and {@link SceneBuilder#okButton}.
	 */
	private final GridPane pane;
	
	/**
	 * The {@link Visualizer} that will show the network that has been submitted by 
	 * clicking the OK button.
	 */
	private final Visualizer visualizer;
	
	/**
	 * Called whenever the visualization is started, paused or stopped
	 */
	private final ChangeListener<Status> timeLineStatus;
	
	/**
	 * Called whenever the visualization is sped up or down
	 */
	private final ChangeListener<Number> timeLineRate;

	/**
	 * @param fields Text fields containing the paths to the source XML files.
	 * @param okButton The button with which this {@link EventHandler} is associated.
	 * @param onlyComponents The {@link CheckBox} that allows the user to select whether to show just those
	 * elements in the map that correspond to the injected JDEECo components 
	 * @param pane The {@link GridPane} which contains {@link SceneBuilder#progIndicator} 
	 * and {@link SceneBuilder#okButton}.
	 * @param visualizer The {@link Visualizer} that will show the network that has been 
	 * submitted by clicking the OK button.
	 * @param durationBox The {@link ComboBox} containing the desired duration of the visualization
	 * @param timeLineStatus Called whenever the visualization is started, paused or stopped
	 * @param timeLineRate Called whenever the visualization is sped up or down
	 */
	public SceneBuilder(List<TextField> fields, Button okButton, CheckBox onlyComponents, GridPane pane, Visualizer visualizer, 
			ComboBox<Integer> durationBox, ChangeListener<Status> timeLineStatus, ChangeListener<Number> timeLineRate) {
		this.fields = fields;
		this.okButton = okButton;
		this.onlyComponentsBox = onlyComponents;
		this.pane = pane;
		this.visualizer = visualizer;
		this.durationBox = durationBox;
		this.timeLineStatus = timeLineStatus;
		this.timeLineRate = timeLineRate;
	}
	
	/**
	 * The {@link ComboBox} containing the desired duration of the visualization
	 */
	private final ComboBox<Integer> durationBox;

	/**
	 * Adds {@link SceneBuilder#progIndicator} to the {@link SceneBuilder#pane} container.
	 */
	private void openProgressIndicator(){
		int column = GridPane.getColumnIndex(okButton);
		int row = GridPane.getRowIndex(okButton);				
		pane.add(progIndicator, column, row);	
		
	}
	
	/**
	 * Removes {@link SceneBuilder#progIndicator} from the 
	 * {@link SceneBuilder#pane} container.
	 */
	private void closeProgressIndiciator(){
		pane.getChildren().remove(progIndicator);
	}

	/**
	 * Called when {@link SceneBuilder#okButton} is clicked. Makes sure that the input fields 
	 * contain data. Then, by calling {@link SceneBuilder#importNewScene(String, String, String)},
	 * parses the input files and visualizes the simulation data (in {@link SceneBuilder#visualizer}).
	 */
	@Override
	public void handle(javafx.event.Event arg0) {
		for (TextField field : fields){
			if ((field == null) || (field.getText() == null) || 
					(field.getText().isEmpty()) || (onlyComponentsBox == null)){
				return;		//TODO better handling than no-op
			}
		}
		final String networkFieldText = fields.get(0).getText();
		final String eventsFieldText = fields.get(1).getText();
		final String ensembleFieldText = fields.get(2).getText();
		final boolean onlyComponents = onlyComponentsBox.isSelected();
		new Thread(){

			@Override
			public void run() {
				try {
					importNewScene(networkFieldText, null, eventsFieldText, ensembleFieldText, onlyComponents);				
				} catch (ParserConfigurationException | SAXException | IOException ex){
					ex.printStackTrace();	
					System.exit(1);
					//TODO better exception handling
				}
			}											
		}.start();
	}
	
	/**
	 * Parses the input files and visualizes the simulation data 
	 * (in {@link SceneBuilder#visualizer} ).
	 * @param networkFieldText Path to the XML file with network definitions.
	 * @param facilitiesFieldText Path to the XML file containing facilities definitions.	 
	 * @param eventsFieldText Path to the XML file containing event log.
	 * @param onlyComponents If true, only the events of the injected JDEECo components will be taken into account.
	 * @throws SAXException When there is any problem when parsing the XML document. 
	 * It is often used as a wrapper for other kinds of exceptions.
	 * @throws IOException If the source XML file, specified by a method parameter, 
	 * does not exist or is inaccessible.
	 */
	private void importNewScene(String networkFieldText, String facilitiesFieldText, String eventsFieldText, 
			String ensembleFieldText, boolean onlyComponents) throws ParserConfigurationException, SAXException, IOException{
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {								
				openProgressIndicator();
			}
		});
		Path networkFile = Paths.get(networkFieldText);		
		Path eventsFile = Paths.get(eventsFieldText);
		Path ensembleFile = Paths.get(ensembleFieldText);
		NodeHandler nodeHandler = new NodeHandler();
		XMLextractor.run(networkFile, nodeHandler);
		LinkHandler linkHandler = new LinkHandler(nodeHandler.getNodes());
		XMLextractor.run(networkFile, linkHandler);	
		EnsembleHandler ensembleHandler = new EnsembleHandler();
		XMLextractor.run(ensembleFile, ensembleHandler);
		
		EventWithPersonHandler eventWithPersonHandler = new EventWithPersonHandler(linkHandler.getLinks(), onlyComponents);
		XMLextractor.run(eventsFile, eventWithPersonHandler);
		CheckPointDatabase cdb = buildCheckPointDatabase(eventWithPersonHandler.getEvents());
		Map<String,Shape> personShapes = new HashMap<>();
		Map<MembershipRelation,Shape> ensembleShapes = new HashMap<>();
		final MapScene scene = new MapScene(nodeHandler.getNodes(), linkHandler.getLinks(), visualizer.getMapWidth(), 
				visualizer.getMapHeight(), personShapes, ensembleShapes, timeLineStatus, timeLineRate);
		final List<KeyFrame> keyFrames = buildKeyFrames(cdb, personShapes, scene);
		final List<KeyFrame> keyFrames2 = buildKeyFrames2(ensembleHandler.getEvents(), personShapes, scene, cdb, ensembleShapes);
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				scene.update();
				visualizer.setScene(scene);
				ScrollPane mapScrollPane = scene.getMapPane();
				visualizer.getMapPane().getChildren().clear();
				visualizer.getMapPane().getChildren().add(mapScrollPane);				
				visualizer.getControlsBar().setDisable(false);
				visualizer.getGraphicsColumn().setDisable(false);
				closeProgressIndiciator();
				scene.getTimeLine().getKeyFrames().addAll(keyFrames);
				scene.getTimeLine().getKeyFrames().addAll(keyFrames2);
				scene.addRecordingFrames();
			}
		});				
	}
	
	//TODO take care of the corner cases in this method
	//TODO add javadoc
	private List<KeyFrame> buildKeyFrames2(List<EnsembleEvent> events, Map<String,? extends Shape> personShapes, 
			MapScene scene, CheckPointDatabase cdb, Map<MembershipRelation,Shape> ensembleShapes){
		
		// Assumption: the {@link Shape} instances in {@code personShapes} are of type Circle
		
		List<KeyFrame> res = new ArrayList<>();
		EnsembleDatabase edb = new EnsembleDatabase();
		for (EnsembleEvent eev : events){
			double timeVal = cdb.transformTime(eev.getTime(), durationBox.getValue());
			Duration time = new Duration(timeVal);
			String coord = eev.getCoordinator();
			Circle coordShape = (Circle)personShapes.get(coord);
			String member = eev.getMember();
			Circle memberShape = (Circle)personShapes.get(member);
			if ((coordShape != null) && (memberShape != null)){		
				
//				TODO what if some of them is null? when does it happen?
				
				Shape ensembleShape = edb.getEnsembleShape(eev.getEnsemble(), coord, member, coordShape, memberShape);
				KeyValue kv = new KeyValue(ensembleShape.visibleProperty(), eev.getMembership());
				KeyFrame kf = new KeyFrame(time, kv);
				res.add(kf);
			}
		}
		ensembleShapes.putAll(edb.getEnsembleShapes());
		for (Shape line : ensembleShapes.values()){
			KeyValue kv = new KeyValue(line.visibleProperty(), Boolean.FALSE);
			KeyFrame kf = new KeyFrame(Duration.ZERO, kv);
			res.add(kf);
		}
		return res;
	}
	
	/**
	 * Stores the shapes representing the ensemble membership relation.
	 */
	private static class EnsembleDatabase {
		
		/**
		 * To a membership relation, i.e. a triplet (ensemble,coordinator,member),
		 * it maps a corresponding shape visualizing the relation.
		 */
		private final Map<MembershipRelation,Shape> shapeMappings = new HashMap<>();

		/**
		 * @return To a membership relation, i.e. a triplet (ensemble,coordinator,member),
		 * it maps a corresponding shape visualizing the relation.
		 * @see {@link EnsembleDatabase#shapeMappings} 
		 */
		public Map<MembershipRelation,Shape> getEnsembleShapes(){
			return shapeMappings;
		}
		
		/**
		 * For given ensemble name and coordinator, it stores a color used
		 * for shapes visualizing the membership relations including these
		 * two (ensemble,coordinator).
		 */
		private final Map<CoordinatorRelation,Paint> colors = new HashMap<>();
		
		/**
		 * Given an ensemble name and coordinator ID, this method gives a color
		 * which will be used to visualize the ensemble memberships including
		 * the given ensemble and coordinator.
		 * @param ensembleName Name of an ensemble
		 * @param coordinator ID of an coordinator
		 * @return Color which will be used to visualize the ensemble memberships 
		 * including the given ensemble and coordinator
		 */
		private Paint getColor(String ensembleName, String coordinator){
			CoordinatorRelation pair = new CoordinatorRelation(ensembleName, coordinator);
			if (!colors.containsKey(pair)){
				double red = Math.random();
				double green = Math.random();
				double blue = Math.random();
				Paint color = Color.color(red, green, blue, ensembleLineOpacity);
				colors.put(pair, color);
			}
			return colors.get(pair);
		}
		
		/**
		 * Width of the lines used to visualize the ensemble membership relation
		 */
		private final double ensembleLineWidth = 1.5;
		
		/**
		 * Opacity of the lines used to visualize the ensemble membership relation
		 */
		private final double ensembleLineOpacity = 0.8;
		
		/**
		 * Given the ensemble name and IDs of coordinator and member, this method
		 * gives a geometric shape visualizing this relationship.
		 * @param ensembleName Name of an ensemble
		 * @param coordinator ID of an coordinator
		 * @param member ID of a member
		 * @param coordinatorCircle The shape visualizing the coordinator
		 * @param memberCircle The shape visualizing the member
		 * @return Geometric shape visualizing this relationship specified
		 * by the ensemble name, coordinator and member IDs 
		 */
		public Shape getEnsembleShape(String ensembleName, String coordinator, String member, 
				Circle coordinatorCircle, Circle memberCircle){
			MembershipRelation t = new MembershipRelation(ensembleName, coordinator, member);
			if (!shapeMappings.containsKey(t) && (memberCircle!=null)){
				Line line = new Line();
				line.setVisible(false);
				line.startXProperty().bind(coordinatorCircle.centerXProperty());
				line.startYProperty().bind(coordinatorCircle.centerYProperty());
				line.endXProperty().bind(memberCircle.centerXProperty());
				line.endYProperty().bind(memberCircle.centerYProperty());
				Paint color = getColor(ensembleName, coordinator);
				line.setStroke(color);
				line.setStrokeWidth(ensembleLineWidth);
				shapeMappings.put(t, line);
			}
			return shapeMappings.get(t);
		}
	}
	
	/**
	 * Given the {@link CheckPointDatabase}, this method converts its contents into the format
	 * specified by JavaFX {@link Timeline} animation model. The {@code shapes} collection is 
	 * filled with the individual {@link Shape} objects that represent the persons. 
	 * @param cdb Contains positions of people on the map at specified times.
	 * @param shapes Is filled by this method with {@link Shape} instances representing individual people.
	 * @return {@link KeyFrame} instances corresponding to the positions and times contained in
	 * the {@link CheckPointDatabase} given as a parameter.
	 */
	private List<KeyFrame> buildKeyFrames(CheckPointDatabase cdb, Map<String,Shape> shapes, MapScene scene){
		List<KeyFrame> frames = new ArrayList<>();
		for (String personID : cdb.getKeys()){
			List<CheckPoint> checkPoints = cdb.getList(personID);
			Shape personShape = buildPersonShape(checkPoints, scene);
			if ((personShape == null) || !(personShape instanceof Circle)){
				continue;
			}
			personShape.setVisible(false);
			DoubleProperty centerXProperty = ((Circle)personShape).centerXProperty();
			DoubleProperty centerYProperty = ((Circle)personShape).centerYProperty();
			KeyValue initX = new KeyValue(centerXProperty, centerXProperty.get());
			KeyValue initY = new KeyValue(centerYProperty, centerYProperty.get());
			KeyValue initVis = new KeyValue(personShape.visibleProperty(), false);
			frames.add(new KeyFrame(Duration.ZERO, initX, initY, initVis));
			for (CheckPoint cp : checkPoints){
				Duration actualTime = new Duration(cdb.transformTime(cp.getTime(), durationBox.getValue()));
				KeyFrame frame = null;
				if (cp.getType().equals(Type.POSITION_DEF)){
					double actualX = scene.transformX(cp.getX());
					double actualY = scene.transformY(cp.getY());
					KeyValue xVal = new KeyValue(centerXProperty, actualX);
					KeyValue yVal = new KeyValue(centerYProperty, actualY);
					frame = new KeyFrame(actualTime, xVal, yVal);					
				} else if (cp.getType().equals(Type.PERSON_ENTERS) || cp.getType().equals(Type.PERSON_LEAVES)){
					boolean personEnters = cp.getType().equals(Type.PERSON_ENTERS);
					KeyValue visibleVal = new KeyValue(personShape.visibleProperty(), personEnters);
					frame = new KeyFrame(actualTime, visibleVal);
				} else {
					throw new UnsupportedOperationException();
				}
				frames.add(frame);
			}
			shapes.put(personID, personShape);
		}
		return frames;
	}
	
	/**
	 * Builds a {@link Shape} that represents a moving person/vehicle on the map.
	 * @param checkPoints Checkpoints of the person/vehicle, parsed from the event log.
	 * @param mapScene A {@link MapScene} instance that will hold the returned {@link Shape} instance.
	 * @return A {@link Shape} that represents a moving person/vehicle on the map.
	 */
	private Shape buildPersonShape(List<CheckPoint> checkPoints, MapScene mapScene){
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
			Circle circle = new Circle(mapScene.transformX(x), mapScene.transformY(y), mapScene.getPersonRadius(), mapScene.getPersonColor());			
			return circle;
		}
	}
	
	
	/**
	 * Given the {@link MatsimEvent} instances parsed from the event log, this method extracts the
	 * information about the positions of people on the map at specified times. This information
	 * is then returned packaged in the {@link CheckPointDatabase} instance.
	 * @param events The {@link MatsimEvent} instances parsed from the event log.
	 * @return {@link CheckPointDatabase} instance containing information about the positions of
	 * people on the map at specified times.
	 */
	private CheckPointDatabase buildCheckPointDatabase(List<MatsimEvent> events){
		CheckPointDatabase db = new CheckPointDatabase();
		for (MatsimEvent event : events){
			String personID = event.getPerson();
			double time = event.getTime();
			if (event.getType().equals(EventType.PERSON_ENTERS_VEHICLE)){	
				EntersOrLeavesVehicle elv = (EntersOrLeavesVehicle)event;
				String vehicleID = elv.getVehicleId();
				CheckPoint cp = new CheckPoint(time, personID, vehicleID, Type.PERSON_ENTERS);
				db.add(personID, cp);
				db.setInVehicle(personID, vehicleID);
			} else if (event.getType().equals(EventType.PERSON_LEAVES_VEHICLE)){
				EntersOrLeavesVehicle elv = (EntersOrLeavesVehicle)event;
				String vehicleID = elv.getVehicleId();
				CheckPoint cp = new CheckPoint(time, personID, vehicleID, Type.PERSON_LEAVES);
				db.add(personID, cp);
				db.setInVehicle(personID, null);
			} else if (event.getType().equals(EventType.ENTERED_LINK)){
				String vehicleID = db.getInVehicle(personID);
				EnteredOrLeftLink ell = (EnteredOrLeftLink)event;					
				MyLink link = ell.getLink();
				boolean justDeparted = db.getJustDeparted(personID);
				if (justDeparted && (vehicleID==null)){
					CheckPoint cp = new CheckPoint(time, personID, vehicleID, Type.PERSON_ENTERS);
					db.add(personID, cp);
				}
				MyNode node;
				if (justDeparted){
					node = link.getTo();
				} else {
					node = link.getFrom();
				}
				double x = node.getX();
				double y = node.getY();
				CheckPoint cp = new CheckPoint(x, y, time, personID, vehicleID, Type.POSITION_DEF);
				db.add(personID, cp);
				db.setJustDeparted(personID, false);
			} else if (event.getType().equals(EventType.DEPARTURE)){
				db.setJustDeparted(personID, true);
			} else if (event.getType().equals(EventType.LEFT_LINK)){
				String vehicleID = db.getInVehicle(personID);
				EnteredOrLeftLink ell = (EnteredOrLeftLink)event;
				MyNode node = ell.getLink().getTo();
				double x = node.getX();
				double y = node.getY();
				CheckPoint cp = new CheckPoint(x, y, time, personID, vehicleID, Type.POSITION_DEF);
				db.add(personID, cp);
			} else if (event.getType().equals(EventType.ARRIVAL)){
				CheckPoint cp = new CheckPoint(time, personID, null, Type.PERSON_LEAVES);
				db.add(personID, cp);
			}
		}
		return db;
	}
		
}
