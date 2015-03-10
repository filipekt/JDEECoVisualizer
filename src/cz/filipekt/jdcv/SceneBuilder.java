package cz.filipekt.jdcv;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation.Status;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import cz.filipekt.jdcv.CheckPoint.Type;
import cz.filipekt.jdcv.events.EnteredOrLeftLink;
import cz.filipekt.jdcv.events.EntersOrLeavesVehicle;
import cz.filipekt.jdcv.events.EventType;
import cz.filipekt.jdcv.events.MatsimEvent;
import cz.filipekt.jdcv.network.MyLink;
import cz.filipekt.jdcv.network.MyNode;
import cz.filipekt.jdcv.util.Dialog;
import cz.filipekt.jdcv.util.Resources;
import cz.filipekt.jdcv.xml.EnsembleHandler;
import cz.filipekt.jdcv.xml.LinkHandler;
import cz.filipekt.jdcv.xml.MatsimEventHandler;
import cz.filipekt.jdcv.xml.NodeHandler;
import cz.filipekt.jdcv.xml.XMLextractor;

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
								Dialog.show(cz.filipekt.jdcv.util.Dialog.Type.ERROR,
										"Could not read from one of the input files:",
										ex.getMessage());								
							}
						});
					} catch (final ParserConfigurationException ex) {
						Platform.runLater(new Runnable() {
							
							@Override
							public void run() {
								Dialog.show(cz.filipekt.jdcv.util.Dialog.Type.ERROR,
										"A problem with XML parser configuration has been encountered:",
										ex.getMessage());								
							}
						});
					} catch (final SAXException ex) {
						Platform.runLater(new Runnable() {
							
							@Override
							public void run() {
								Dialog.show(cz.filipekt.jdcv.util.Dialog.Type.ERROR,
										"A problem in syntax of one of the XML input files has been encountered:",
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
		boolean onlyAgents = onlyAgentsBox.isSelected();
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
		Integer startAt = startAtVal;
		String endAtText = endAtField.getText();
		Integer endAtVal = null;
		try {
			if ((endAtText != null) && (!endAtText.isEmpty())){
				endAtVal = Integer.valueOf(endAtText);
			}
		} catch (NumberFormatException ex) {
			problems.add("The \"End at\" field may only contain an integer number or nothing.");
		}
		Integer endAt = endAtVal;
		String durationText = durationField.getText();
		int durationVal = -1;
		try {
			durationVal = Integer.parseInt(durationText);
		} catch (NumberFormatException ex){
			problems.add("The \"Duration\" field must contain an integer number.");
		}
		int duration = durationVal;
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
		try {
			Console.getInstance().getWriter().write(Paths.get(".").toAbsolutePath().toString());
			Path networkFile = Paths.get(pathFields.get(0).getText());		
			Path eventsFile = Paths.get(pathFields.get(1).getText());
			Path ensembleFile = Paths.get(pathFields.get(2).getText());
			NodeHandler nodeHandler = new NodeHandler();
			XMLextractor.run(networkFile, nodeHandler);
			LinkHandler linkHandler = new LinkHandler(nodeHandler.getNodes());
			XMLextractor.run(networkFile, linkHandler);	
			EnsembleHandler ensembleHandler = new EnsembleHandler(startAt, endAt);
			XMLextractor.run(ensembleFile, ensembleHandler);
			MatsimEventHandler eventWithPersonHandler = new MatsimEventHandler(linkHandler.getLinks(), onlyAgents, startAt, endAt);
			XMLextractor.run(eventsFile, eventWithPersonHandler);
			final CheckPointDatabase cdb = buildCheckPointDatabase(eventWithPersonHandler.getEvents());
			double minTime = startAt==null ? cdb.getMinTime() : (startAt * 1.0);
			double maxTime = endAt==null ? cdb.getMaxTime() : (endAt * 1.0);
			final MapScene scene = new MapScene(nodeHandler.getNodes(), linkHandler.getLinks(), visualizer.getMapWidth(), 
					visualizer.getMapHeight(), timeLineStatus, timeLineRate, minTime, 
					maxTime, duration, cdb, ensembleHandler.getEvents());
			ShapeProvider circleProvider = new CircleProvider(scene.getPersonCircleRadius(), scene.getPersonCircleColor());
			scene.update(circleProvider, false, null);
			Platform.runLater(new Runnable() {
				
				@Override
				public void run() {					
					visualizer.setScene(scene, cdb.getMinTime(), cdb.getMaxTime());										
				}
			});		
		} finally {
			
			Platform.runLater(new Runnable() {
				
				@Override
				public void run() {
					closeProgressIndiciator();
				}
			});
		}
				
	}
	
	/**
	 * Producer of {@link Node} instances, later used for visualizing persons or cars 
	 */
	public static interface ShapeProvider {
		
		/**
		 * @return A new instance of {@link Node}, 
		 * later used for visualizing persons or cars 
		 * @throws IOException When the shape could not be loaded for any reason
		 */
		Node getNewShape() throws IOException;
	}
	
	/**
	 * Producer of {@link ImageView} instances,
	 * later used for visualizing persons or cars 
	 */
	static class ImageProvider implements ShapeProvider {
		
		/**
		 * Name of the resource file containing the image
		 */
		private final String image;
		
		/**
		 * Width (a also height) of the provided image
		 */
		private final double imageWidth;
		
		/**
		 * If true, the specified image is looked for in the application resources.
		 * Otherwise the image is looked for in the filesystem. 
		 */
		private final boolean isResource;

		/**
		 * @param isResource If true, the image specified by the second parameter, is looked 
		 * for in the application resources. 
		 * @param image If the first parameter holds, this parameter specifies the resource name. Otherwise,
		 * this parameter contains a full path to the specified image.
		 * @param imageWidth Width (a also height) of the provided image
		 * @throws FileNotFoundException When the image could not be found
		 */
		public ImageProvider(boolean isResource, String image, double imageWidth) throws FileNotFoundException {
			this.image = image;
			this.imageWidth = imageWidth;
			this.isResource = isResource;
			try {
				getNewShape();
			} catch (IOException ex){
				throw new FileNotFoundException();
			}
		}

		/**
		 * @return The specified image, or null if the image was not found.
		 * @throws IOException When the shape could not be loaded for any reason
		 */
		@Override
		public Node getNewShape() throws IOException {
			final ImageView res;
			if (isResource){
				res = Resources.getImageView(image, imageWidth);
			} else {
				InputStream stream = Files.newInputStream(Paths.get(image));
				Image image = new Image(stream, imageWidth, imageWidth, true, false);
				res = new ImageView(image);
			}
			res.setLayoutX(-(imageWidth/2));
			res.setLayoutY(-(imageWidth/2));
			res.setOnMouseEntered(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent arg0) {
					res.setScaleX(1.5);
					res.setScaleY(1.5);
				}
			});
			res.setOnMouseExited(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent arg0) {
					res.setScaleX(1);
					res.setScaleY(1);
				}
			});
			return res;
		}
		
	}
	
	/**
	 * Producer of {@link Circle} instances,
	 * later used for visualizing persons or cars 
	 */
	static class CircleProvider implements ShapeProvider {
		
		/**
		 * Radius of the circle
		 */
		private final double radius;
		
		/**
		 * Color of the circle
		 */
		private final Paint color;

		/**
		 * @param radius Radius of the circle
		 * @param color Color of the circle
		 */
		public CircleProvider(double radius, Paint color) {
			this.radius = radius;
			this.color = color;
		}
		
		/**
		 * @return A new instance of {@link Circle}, 
		 * later used for visualizing persons or cars 
		 */
		@Override
		public Node getNewShape() {
			final Circle circle = new Circle(0, 0, radius, color);
			circle.setOnMouseEntered(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent arg0) {
					circle.setRadius(radius * 2);
				}
			});
			circle.setOnMouseExited(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent arg0) {
					circle.setRadius(radius);
				}
			});
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
