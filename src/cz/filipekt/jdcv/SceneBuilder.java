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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
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
import cz.filipekt.jdcv.util.Dialog;
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
	 * The three fields containing the paths to the source XML files.
	 * The first one holds paths to the network definition, the second 
	 * to the event log, the third to the ensemble event log.
	 */
	private final List<TextField> pathFields;
	
	/**
	 * The {@link Button} with which this {@link EventHandler} is associated. 
	 */
	private final Button okButton;
	
	/**
	 * The {@link CheckBox} that allows the user to select whether to show just those
	 * elements in the map that correspond to the injected JDEECo agents 
	 */
	private final CheckBox onlyAgentsBox;
	
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
	 * @param pathFields Text fields containing the paths to the source XML files.
	 * @param okButton The button with which this {@link EventHandler} is associated.
	 * @param onlyAgents The {@link CheckBox} that allows the user to select whether to show just those
	 * elements in the map that correspond to the injected JDEECo agents 
	 * @param pane The {@link GridPane} which contains {@link SceneBuilder#progIndicator} 
	 * and {@link SceneBuilder#okButton}.
	 * @param visualizer The {@link Visualizer} that will show the network that has been 
	 * submitted by clicking the OK button.
	 * @param durationField The field specifying the duration of the visualization
	 * @param timeLineStatus Called whenever the visualization is started, paused or stopped
	 * @param timeLineRate Called whenever the visualization is sped up or down
	 * @param startAtField The field specifying the time (simulation time) at which the 
	 * visualization should begin.
	 * @param endAtField The field specifying the time (simulation time) at which the 
	 * visualization should end.
	 * @throws NullPointerException When any of the parameters if null
	 */
	public SceneBuilder(List<TextField> pathFields, Button okButton, CheckBox onlyAgents, GridPane pane, Visualizer visualizer, 
			TextField durationField, ChangeListener<Status> timeLineStatus, ChangeListener<Number> timeLineRate,
			TextField startAtField, TextField endAtField) throws NullPointerException {
		if ((pathFields == null) || (okButton == null) || (onlyAgents == null) || (pane == null) ||
				(visualizer == null) || (durationField == null) || (timeLineStatus == null) ||
				(timeLineRate == null) || (startAtField == null) || (endAtField == null)){
			throw new NullPointerException();
		}
		this.pathFields = pathFields;
		this.okButton = okButton;
		this.onlyAgentsBox = onlyAgents;
		this.pane = pane;
		this.visualizer = visualizer;
		this.durationField = durationField;
		this.timeLineStatus = timeLineStatus;
		this.timeLineRate = timeLineRate;
		this.startAtField = startAtField;
		this.endAtField = endAtField;
	}
	
	/**
	 * The {@link TextField} specifying the time (simulation time) at which the 
	 * visualization should begin.
	 */
	private final TextField startAtField;
	
	/**
	 * The {@link TextField} specifying the time (simulation time) at which the 
	 * visualization should end.
	 */
	private final TextField endAtField;
	
	/**
	 * The field specifying the duration of the visualization
	 */
	private final TextField durationField;

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
	 * When run, this piece of code stops the JVM.
	 */
	private final Runnable exit = new Runnable() {
		
		@Override
		public void run() {
			System.exit(1);
		}
	};
	
	/**
	 * Reports any problems encountered by the {@link SceneBuilder#handle} method and then tries to
	 * create a new {@link MapScene} by calling {@link SceneBuilder#prepareNewScene}. If any problem
	 * is encountered, details are shown to the user and the application is terminated.
	 * @param problems Problems encountered by {@link SceneBuilder#handle}
	 * @param onlyAgents Value of the checkbox specifying whether only JDEECo agents should be shown
	 * @param startAt Value of the field specifying the simulation time at which visualization should start
	 * @param endAt Value of the field specifying the simulation time at which visualization should end
	 * @param duration Value of the field specifying the total duration of the visualization
	 */
	private void reportProblemsForScene(List<String> problems, final boolean onlyAgents, final Integer startAt,
			final Integer endAt, final int duration){
		if (problems.size() > 0){
			StringBuilder sb = new StringBuilder();
			for (String problem : problems){
				sb.append(problem);
				sb.append("\n");
			}
			Dialog.show(cz.filipekt.jdcv.util.Dialog.Type.ERROR, "Some problems were encountered:", sb.toString());
		} else {
			new Thread(){

				@Override
				public void run() {
					try {	
						prepareNewScene(onlyAgents, startAt, endAt, duration);
					} catch (final IOException ex){
						Platform.runLater(new Runnable() {
							
							@Override
							public void run() {
								Dialog.show(cz.filipekt.jdcv.util.Dialog.Type.ERROR, exit,
										"Could not read from one of the input files:",
										ex.getMessage());								
							}
						});
					} catch (final ParserConfigurationException ex) {
						Platform.runLater(new Runnable() {
							
							@Override
							public void run() {
								Dialog.show(cz.filipekt.jdcv.util.Dialog.Type.ERROR, exit,
										"A problem with XML parser configuration has been encountered:",
										ex.getMessage());								
							}
						});
					} catch (final SAXException ex) {
						Platform.runLater(new Runnable() {
							
							@Override
							public void run() {
								Dialog.show(cz.filipekt.jdcv.util.Dialog.Type.ERROR, exit,
										"A problem in one of the XML input files has been encountered:",
										ex.getMessage());								
							}
						});
					}
				}
				
			}.start();
		}
	}
	
	/**
	 * Called when {@link SceneBuilder#okButton} is clicked. Makes sure that the input fields 
	 * contain data. Further processing is delegated to {@link SceneBuilder#reportProblemsForScene}
	 */
	@Override
	public void handle(javafx.event.Event event) {
		for (TextField field : pathFields){
			if ((field == null) || (field.getText() == null) || (field.getText().isEmpty())){
				Dialog.show(cz.filipekt.jdcv.util.Dialog.Type.INFO, 
						"You must specify paths to all of the three source XML files.");
				return;
			}
		}
		final boolean onlyAgents = onlyAgentsBox.isSelected();
		List<String> problems = new ArrayList<>();
		String startAtText = startAtField.getText();
		Integer startAtVal = null;
		try {
			if ((startAtText != null) && (!startAtText.isEmpty())){
				startAtVal = Integer.valueOf(startAtText);
			}

		} catch (NumberFormatException ex){
			problems.add("The \"Start at\" field may only contain an integer number or nothing.");
		}
		final Integer startAt = startAtVal;
		String endAtText = endAtField.getText();
		Integer endAtVal = null;
		try {
			if ((endAtText != null) && (!endAtText.isEmpty())){
				endAtVal = Integer.valueOf(endAtText);
			}
		} catch (NumberFormatException ex) {
			problems.add("The \"End at\" field may only contain an integer number or nothing.");
		}
		final Integer endAt = endAtVal;
		String durationText = durationField.getText();
		int durationVal = -1;
		try {
			durationVal = Integer.parseInt(durationText);
		} catch (NumberFormatException ex){
			problems.add("The \"Duration\" field must contain an integer number.");
		}
		final int duration = durationVal;
		reportProblemsForScene(problems, onlyAgents, startAt, endAt, duration);
	}
	
	/**
	 * Creates a {@link MapScene} representation of the data provided by the input
	 * files. The instance is then given to the {@link Visualizer} instance which
	 * takes care of the actual visualization.
	 * @param onlyAgents Value of the checkbox specifying whether only JDEECo agents should be shown
	 * @param startAt Value of the field specifying the simulation time at which visualization should start
	 * @param endAt Value of the field specifying the simulation time at which visualization should end
	 * @param duration Value of the field specifying the total duration of the visualization
	 * @throws ParserConfigurationException Should never happen
	 * @throws SAXException When there is any problem when parsing the XML document. 
	 * It is generally used as a wrapper for other kinds of exceptions.
	 * @throws IOException If the source XML file, specified by a method parameter, 
	 * does not exist or is inaccessible.
	 */
	private void prepareNewScene(boolean onlyAgents, Integer startAt, Integer endAt, int duration) 
			throws ParserConfigurationException, SAXException, IOException{
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {								
				openProgressIndicator();
			}
		});
		Path networkFile = Paths.get(pathFields.get(0).getText());		
		Path eventsFile = Paths.get(pathFields.get(1).getText());
		Path ensembleFile = Paths.get(pathFields.get(2).getText());
		NodeHandler nodeHandler = new NodeHandler();
		XMLextractor.run(networkFile, nodeHandler);
		LinkHandler linkHandler = new LinkHandler(nodeHandler.getNodes());
		XMLextractor.run(networkFile, linkHandler);	
		EnsembleHandler ensembleHandler = new EnsembleHandler(startAt, endAt);
		XMLextractor.run(ensembleFile, ensembleHandler);
		EventWithPersonHandler eventWithPersonHandler = new EventWithPersonHandler(linkHandler.getLinks(), onlyAgents, startAt, endAt);
		XMLextractor.run(eventsFile, eventWithPersonHandler);
		final CheckPointDatabase cdb = buildCheckPointDatabase(eventWithPersonHandler.getEvents());
		Map<String,Node> personShapes = new HashMap<>();
		Map<MembershipRelation,Node> ensembleShapes = new HashMap<>();
		double minTime = startAt==null ? cdb.getMinTime() : (startAt * 1.0);
		double maxTime = endAt==null ? cdb.getMaxTime() : (endAt * 1.0);
		final MapScene scene = new MapScene(nodeHandler.getNodes(), linkHandler.getLinks(), visualizer.getMapWidth(), 
				visualizer.getMapHeight(), personShapes, ensembleShapes, timeLineStatus, timeLineRate, minTime, maxTime, duration);
		final List<KeyFrame> keyFrames = buildKeyFrames(cdb, personShapes, scene, duration);
		scene.getTimeLine().getKeyFrames().addAll(keyFrames);
		final List<KeyFrame> keyFrames2 = buildKeyFrames2(ensembleHandler.getEvents(), personShapes, scene, ensembleShapes);
		scene.getTimeLine().getKeyFrames().addAll(keyFrames2);
		scene.addRecordingFrames();
		scene.update();
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				visualizer.setScene(scene, cdb.getMinTime(), cdb.getMaxTime());
				closeProgressIndiciator();
			}
		});				
	}
	
	/**
	 * Given all the ensemble events, this method creates their graphical representations in the form of 
	 * JavaFX nodes and prepares the correct movements of these nodes by binding them in the right way
	 * to the movements of the corresponding coordinators and members.  
	 * @param events All the ensemble events, as parsed from the input file
	 * @param personShapes Maps each person's ID to the person's graphical representation
	 * @param scene The simulated situation to be visualized
	 * @param ensembleShapes Is cleared and filled by this method. The new contents map each ensemble 
	 * membership relation to the graphical representation of this relation.
	 * @return These key frames capture the varying visibility of the ensemble membership representations,
	 * as they disappear whenever the membership condition ceases to hold.
	 */
	private List<KeyFrame> buildKeyFrames2(List<EnsembleEvent> events, Map<String,Node> personShapes, 
			MapScene scene, Map<MembershipRelation,Node> ensembleShapes){
		List<KeyFrame> res = new ArrayList<>();
		EnsembleDatabase edb = new EnsembleDatabase();
		for (EnsembleEvent eev : events){
			double timeVal = scene.convertToVisualizationTime(eev.getTime());
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
	 * Given the {@link CheckPointDatabase}, this method converts its contents into the format
	 * specified by JavaFX {@link Timeline} animation model. The {@code shapes} collection is 
	 * filled with the individual nodes that represent the persons. 
	 * @param cdb Contains positions of people on the map at specified times.
	 * @param shapes Is cleared and filled by this method with {@link Node} instances representing individual people.
	 * @param scene The new scene for which the key frames will be created
	 * @param duration The total intended duration of the visualization 
	 * @return {@link KeyFrame} instances corresponding to the positions and times contained in
	 * the {@link CheckPointDatabase} given as a parameter.
	 */
	private List<KeyFrame> buildKeyFrames(CheckPointDatabase cdb, Map<String,Node> shapes, MapScene scene, int duration){
		List<KeyFrame> frames = new ArrayList<>();
		shapes.clear();
		for (final String personID : cdb.getKeys()){
			List<CheckPoint> checkPoints = cdb.getList(personID);
			Node personShape = buildPersonShape(checkPoints, scene);
			if (personShape == null){
				continue;
			}
			personShape.setVisible(false);
			DoubleProperty xProperty = personShape.translateXProperty();
			DoubleProperty yProperty = personShape.translateYProperty();
			BooleanProperty visibleProperty = personShape.visibleProperty();
			KeyValue initX = new KeyValue(xProperty, xProperty.get());
			KeyValue initY = new KeyValue(yProperty, yProperty.get());
			KeyValue initVis = new KeyValue(visibleProperty, false);
			frames.add(new KeyFrame(Duration.ZERO, initX, initY, initVis));
			for (CheckPoint cp : checkPoints){				
				Duration actualTime = new Duration(scene.convertToVisualizationTime(cp.getTime()));
				KeyFrame frame = null;
				if (cp.getType().equals(Type.POSITION_DEF)){
					double actualX = scene.transformX(cp.getX());
					double actualY = scene.transformY(cp.getY());					
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
				frames.add(frame);
			}
			shapes.put(personID, personShape);
		}
		return frames;
	}
	
	/**
	 * Builds a {@link Node} that represents a moving person/vehicle on the map.
	 * @param checkPoints Checkpoints of the person/vehicle, parsed from the event log.
	 * @param mapScene A {@link MapScene} instance that will hold the returned {@link Shape} instance.
	 * @return A {@link Shape} that represents a moving person/vehicle on the map.
	 */
	private Node buildPersonShape(List<CheckPoint> checkPoints, MapScene mapScene){
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
			Circle shape = new Circle(0, 0, mapScene.getPersonRadius(), mapScene.getPersonColor());
			shape.setTranslateX(mapScene.transformX(x));
			shape.setTranslateY(mapScene.transformY(y));
			return shape;
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
