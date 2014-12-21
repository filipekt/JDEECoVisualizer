package cz.filipekt.jdcv;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import cz.filipekt.jdcv.CheckPoint.Type;
import cz.filipekt.jdcv.events.EnteredOrLeftLink;
import cz.filipekt.jdcv.events.EntersOrLeavesVehicle;
import cz.filipekt.jdcv.events.Event;
import cz.filipekt.jdcv.events.EventType;
import cz.filipekt.jdcv.network.MyLink;
import cz.filipekt.jdcv.network.MyNode;
import cz.filipekt.jdcv.xml.LinkHandler;
import cz.filipekt.jdcv.xml.NodeHandler;

/**
 * Makes sure that when the OK button is clicked (after specifying input XML files 
 * for the visualization), that the input data is processed and the requested 
 * visualizations are shown.
 */
class SceneBuilder implements EventHandler<javafx.event.Event>{
	
	/**
	 * Text fields containing the paths to the source XML files.
	 */
	private final List<TextField> fields;
	
	/**
	 * The button with which this {@link EventHandler} is associated. 
	 */
	private final Button okButton;
	
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
	 * @param pane The {@link GridPane} which contains {@link SceneBuilder#progIndicator} 
	 * and {@link SceneBuilder#okButton}.
	 * @param visualizer The {@link Visualizer} that will show the network that has been 
	 * submitted by clicking the OK button.
	 * @param durationBox The {@link ComboBox} containing the desired duration of the visualization
	 * @param timeLineStatus Called whenever the visualization is started, paused or stopped
	 * @param timeLineRate Called whenever the visualization is sped up or down
	 */
	public SceneBuilder(List<TextField> fields, Button okButton, GridPane pane, Visualizer visualizer, 
			ComboBox<Integer> durationBox, ChangeListener<Status> timeLineStatus, ChangeListener<Number> timeLineRate) {
		this.fields = fields;
		this.okButton = okButton;
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
			if ((field == null) || (field.getText() == null) || (field.getText().isEmpty())){
				return;		//TODO better handling than no-op
			}
		}
		final String networkFieldText = fields.get(0).getText();
		final String eventsFieldText = fields.get(1).getText();
		new Thread(){

			@Override
			public void run() {
				try {
					importNewScene(networkFieldText, null, null, eventsFieldText);				
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
	 * @param plansFieldText Path to the XML file containing plans definitions.	 
	 * @param eventsFieldText Path to the XML file containing event log.
	 * @throws SAXException When there is any problem when parsing the XML document. 
	 * It is often used as a wrapper for other kinds of exceptions.
	 * @throws IOException If the source XML file, specified by a method parameter, 
	 * does not exist or is inaccessible.
	 */
	private void importNewScene(String networkFieldText, String facilitiesFieldText, String plansFieldText, 
			String eventsFieldText) throws ParserConfigurationException, SAXException, IOException{
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {								
				openProgressIndicator();
			}
		});
		Path networkFile = Paths.get(networkFieldText);		
		Path eventsFile = Paths.get(eventsFieldText);
		NodeHandler nodeHandler = new NodeHandler();
		XMLextractor.run(networkFile, nodeHandler);
		LinkHandler linkHandler = new LinkHandler(nodeHandler.getNodes());
		XMLextractor.run(networkFile, linkHandler);		
		cz.filipekt.jdcv.xml.EventHandler eh = new cz.filipekt.jdcv.xml.EventHandler(linkHandler.getLinks());
		XMLextractor.run(eventsFile, eh);
		CheckPointDatabase cdb = buildCheckPointDatabase(eh.getEvents());
		Set<Shape> shapes = new HashSet<>();
		final MapScene scene = new MapScene(nodeHandler.getNodes(), linkHandler.getLinks(), visualizer.getMapWidth(), 
				visualizer.getMapHeight(), shapes, timeLineStatus, timeLineRate);
		final List<KeyFrame> keyFrames = buildKeyFrames(cdb, shapes, scene);
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
				scene.addRecordingFrames();
			}
		});				
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
	private List<KeyFrame> buildKeyFrames(CheckPointDatabase cdb, Set<Shape> shapes, MapScene scene){
		List<KeyFrame> frames = new ArrayList<>();
		for (String personID : cdb.getKeys()){
			List<CheckPoint> checkPoints = cdb.getList(personID);
			Circle personShape = buildPersonShape(checkPoints, scene);
			if (personShape == null){
				continue;
			}
			personShape.setVisible(false);
			KeyValue initX = new KeyValue(personShape.centerXProperty(), personShape.getCenterX());
			KeyValue initY = new KeyValue(personShape.centerYProperty(), personShape.getCenterY());
			KeyValue initVis = new KeyValue(personShape.visibleProperty(), false);
			frames.add(new KeyFrame(Duration.ZERO, initX, initY, initVis));
			for (CheckPoint cp : checkPoints){
				Duration actualTime = new Duration(cdb.transformTime(cp.getTime(), durationBox.getValue()));
				KeyFrame frame = null;
				if (cp.getType().equals(Type.POSITION_DEF)){
					double actualX = scene.transformX(cp.getX());
					double actualY = scene.transformY(cp.getY());
					KeyValue xVal = new KeyValue(personShape.centerXProperty(), actualX);
					KeyValue yVal = new KeyValue(personShape.centerYProperty(), actualY);
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
			shapes.add(personShape);
		}
		return frames;
	}
	
	/**
	 * Builds a {@link Circle} that represents a moving person/vehicle on the map.
	 * @param checkPoints Checkpoints of the person/vehicle, parsed from the event log.
	 * @param mapScene A {@link MapScene} instance that will hold the returned {@link Circle} instance.
	 * @return A {@link Circle} that represents a moving person/vehicle on the map.
	 */
	private Circle buildPersonShape(List<CheckPoint> checkPoints, MapScene mapScene){
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
	 * Given the {@link Event} instances parsed from the event log, this method extracts the
	 * information about the positions of people on the map at specified times. This information
	 * is then returned packaged in the {@link CheckPointDatabase} instance.
	 * @param events The {@link Event} instances parsed from the event log.
	 * @return {@link CheckPointDatabase} instance containing information about the positions of
	 * people on the map at specified times.
	 */
	private CheckPointDatabase buildCheckPointDatabase(List<Event> events){
		CheckPointDatabase db = new CheckPointDatabase();
		for (Event event : events){
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
