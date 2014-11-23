package cz.filipekt.jdcv;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import cz.filipekt.jdcv.network.MyAct;
import cz.filipekt.jdcv.network.MyLeg;
import cz.filipekt.jdcv.network.MyLink;
import cz.filipekt.jdcv.network.MyPerson;
import cz.filipekt.jdcv.network.MyPlan;
import cz.filipekt.jdcv.xml.FacilitiesHandler;
import cz.filipekt.jdcv.xml.LinkHandler;
import cz.filipekt.jdcv.xml.NodeHandler;
import cz.filipekt.jdcv.xml.PopulationHandler;

/**
 * Makes sure that when the OK button is clicked (after specifying input XML files 
 * for the visualization), that the input data is processed and the requested 
 * visualizations are shown.
 */
class OkButtonEventHandler implements EventHandler<Event>{
	
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
	 * The {@link GridPane} which contains {@link OkButtonEventHandler#progIndicator} 
	 * and {@link OkButtonEventHandler#okButton}.
	 */
	private final GridPane pane;
	
	/**
	 * The {@link Visualizer} that will show the network that has been submitted by 
	 * clicking the OK button.
	 */
	private final Visualizer visualizer;

	/**
	 * @param fields Text fields containing the paths to the source XML files.
	 * @param okButton The button with which this {@link EventHandler} is associated. 
	 * @param pane The {@link GridPane} which contains {@link OkButtonEventHandler#progIndicator} 
	 * and {@link OkButtonEventHandler#okButton}.
	 * @param visualizer The {@link Visualizer} that will show the network that has been 
	 * submitted by clicking the OK button.
	 */
	public OkButtonEventHandler(List<TextField> fields, Button okButton,
			GridPane pane, Visualizer visualizer) {
		this.fields = fields;
		this.okButton = okButton;
		this.pane = pane;
		this.visualizer = visualizer;
	}

	/**
	 * Adds {@link OkButtonEventHandler#progIndicator} to the {@link OkButtonEventHandler#pane} container.
	 */
	private void openProgressIndicator(){
		int column = GridPane.getColumnIndex(okButton);
		int row = GridPane.getRowIndex(okButton);				
		pane.add(progIndicator, column, row);	
		
	}
	
	/**
	 * Removes {@link OkButtonEventHandler#progIndicator} from the 
	 * {@link OkButtonEventHandler#pane} container.
	 */
	private void closeProgressIndiciator(){
		pane.getChildren().remove(progIndicator);
	}

	/**
	 * Called when {@link OkButtonEventHandler#okButton} is clicked. Makes sure that the input fields 
	 * contain data. Then, by calling {@link OkButtonEventHandler#importNewScene(String, String, String)},
	 * parses the input files and visualizes the simulation data (in {@link OkButtonEventHandler#visualizer}).
	 */
	@Override
	public void handle(Event arg0) {
		for (TextField field : fields){
			if ((field == null) || (field.getText() == null) || (field.getText().isEmpty())){
				return;		//TODO better handling than no-op
			}
		}
		final String networkFieldText = fields.get(0).getText();
		final String facilitiesFieldText = fields.get(1).getText();
		final String plansFieldText = fields.get(2).getText();
		new Thread(){

			@Override
			public void run() {
				try {
					importNewScene(networkFieldText, facilitiesFieldText, plansFieldText);				
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
	 * (in {@link OkButtonEventHandler#visualizer} ).
	 * @param networkFieldText Path of the XML file with network definitions.
	 * @param facilitiesFieldText Path of the XML file containing facilities definitions.
	 * @param plansFieldText Path of the XML file containing plans definitions.
	 * @throws SAXException When there is any problem when parsing the XML document. 
	 * It is often used as a wrapper for other kinds of exceptions.
	 * @throws IOException If the source XML file, specified by a method parameter, 
	 * does not exist or is inaccessible.
	 */
	private void importNewScene(String networkFieldText, String facilitiesFieldText, String plansFieldText) 
			throws ParserConfigurationException, SAXException, IOException{
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {								
				openProgressIndicator();
			}
		});
		Path networkFile = Paths.get(networkFieldText);
		Path facilitiesFile = Paths.get(facilitiesFieldText);
		Path populationFile = Paths.get(plansFieldText);
		NodeHandler nodeHandler = new NodeHandler();
		XMLextractor.run(networkFile, nodeHandler);
		LinkHandler linkHandler = new LinkHandler(nodeHandler.getNodes());
		XMLextractor.run(networkFile, linkHandler);
		FacilitiesHandler facilitiesHandler = new FacilitiesHandler();
		XMLextractor.run(facilitiesFile, facilitiesHandler);
		final PopulationHandler populationHandler = new PopulationHandler(linkHandler.getLinks(), facilitiesHandler.getFacilities());
		XMLextractor.run(populationFile, populationHandler);
		final MapScene scene = new MapScene(nodeHandler.getNodes(), linkHandler.getLinks(), visualizer.getMapWidth(), visualizer.getMapHeight());
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {																		
				visualizer.setScene(scene);
				ScrollPane mapScrollPane = scene.getMapPane();
				mapScrollPane.setPrefSize(scene.getTotalWidth(), scene.getTotalHeight());
				visualizer.getMapPane().getChildren().clear();
				visualizer.getMapPane().getChildren().add(mapScrollPane);
				visualizer.getZoomBar().setDisable(false);
				closeProgressIndiciator();
			}
		});				
		animate(populationHandler, scene);
	}
	
	/**
	 * Creates a simple animation showing the movements of a single random person on the map.
	 */
	private void animate(PopulationHandler pHandler, final MapScene scene){
		MyPerson aPerson = null;
		for (MyPerson p : pHandler.getPersons().values()){
			if (p.getPlans().size() > 0){
				aPerson = p;
				break;
			}
		}
		MyPlan plan = aPerson.getPlans().get(0);
		final List<PositionAtTime> links = new ArrayList<>();
		int length = plan.getActivities().size() + plan.getLegs().size();
		for (int i = 0; i<length; i++){
			if ((i % 2) == 0){
				MyAct act = plan.getActivities().get(i/2);
				double x,y;
				if (act.getLink() == null){
					x = act.getX();
					y = act.getY();
				} else {
					x = act.getLink().getTo().getX();
					y = act.getLink().getTo().getY();
				}
				Date time = act.getEndTime();
				links.add(new PositionAtTime(x, y, time));
			} else {
				MyLeg leg = plan.getLegs().get(i/2);
				List<MyLink> route = leg.getRoute();
				double x,y;
				Date date = leg.getArrTime();
				MyLink link;
				for (int j = 0; j < route.size()-1; j++){
					link = route.get(j);
					x = link.getTo().getX();
					y = link.getTo().getY();
					links.add(new PositionAtTime(x, y, null));
				}
				link = route.get(leg.getRoute().size()-1);
				x = link.getTo().getX();
				y = link.getTo().getY();
				links.add(new PositionAtTime(x, y, date));
			}
		}
		fillInTimes(links);
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				Timeline timeline = new Timeline();
				List<KeyFrame> keyFrames = buildKeyFrames(links, scene);
				timeline.getKeyFrames().addAll(keyFrames);
				timeline.play();						
			}
		});		
	}
	
	/**
	 * The total duration (in milliseconds) of the visualization.
	 */
	double targetTime = 500 * 1000.0;
	
	/**
	 * @param list Positions in map paired with time instants. 
	 * @param scene Scene where the animations will be done.
	 * @return Representation of data contained in "list" for use by JavaFX {@link Timeline} class.
	 */
	private List<KeyFrame> buildKeyFrames(List<PositionAtTime> list, MapScene scene){
		List<KeyFrame> res = new ArrayList<>();
		long beginning = list.get(0).time.getTime();
		long end = list.get(list.size()-1).time.getTime();
		double ratio = targetTime / (end - beginning);								
		for (PositionAtTime pat : list){
			Circle circle = visualizer.getScene().getPerson();
			KeyValue kv1 = new KeyValue(circle.centerXProperty(), scene.transformX(pat.x));
			KeyValue kv2 = new KeyValue(circle.centerYProperty(), scene.transformY(pat.y));
			double time = (pat.time.getTime() - beginning) * ratio;
			long time2 = Math.round(time);													
			KeyFrame kf = new KeyFrame(new Duration(time2), kv1, kv2);
			res.add(kf);
		}
		return res;
	}
	
	/**
	 * @param list Positions in map paired with time instants. However, sometimes the time instance 
	 * is not specified. This method interpolates their value.
	 */
	private void fillInTimes(List<PositionAtTime> list){
		if ((list==null) || (list.size() < 2)){
			return;
		}
		while ((!list.isEmpty()) && (list.get(0).time == null)){					
			list.remove(0);
		}
		while ((!list.isEmpty()) && (list.get(list.size()-1).time == null)){
			list.remove(list.size()-1);
		}
		//Now the list begins and ends with a position with a well defined time
		for (int i = 0; i < list.size(); i++){
			if ((i < list.size() - 1) && (list.get(i+1).time==null)){
				//The next PositionAtTime does not have a defined time
				double totalDist = 0;
				Date startTime = list.get(i).time;
				Date endTime = null;
				for (int j = i;  j< list.size()-1; j++){
					PositionAtTime a = list.get(j);
					PositionAtTime b = list.get(j+1);
					totalDist += computeDistance(a.x, a.y, b.x, b.y);
					if (b.time != null){
						endTime = b.time;
						break;
					}							
				}
				long durationMillis = endTime.getTime() - startTime.getTime();
				for (int j = i;  j< list.size()-1; j++){
					PositionAtTime a = list.get(j);
					PositionAtTime b = list.get(j+1);
					if (b.time != null){								
						break;
					}
					double localDist = computeDistance(a.x, a.y, b.x, b.y);
					double relativeDist = localDist / totalDist;
					long newTimeMillis = Math.round(relativeDist * durationMillis);		
					long newTime2 = a.time.getTime() + newTimeMillis;
					Date newTime = new Date(newTime2);
					b.time = newTime;
				}
			}
		}
	}
	
	/**
	 * @return The euclidean distance between specified points in plane.
	 */
	private double computeDistance(double x1, double y1, double x2, double y2){
		double xx = (x2-x1)*(x2-x1);
		double yy = (y2-y1)*(y2-y1);
		return Math.sqrt(xx+yy);
	}
	
}

/**
 * Pairs a position in map (defined by x,y coordinates) with a time instant.
 */
class PositionAtTime {	
	
	/**
	 * x-coordinate of a point in map
	 */
	final double x;
	
	/**
	 * y-coordinate of a point in map
	 */
	final double y;
	
	/**
	 * A point in time
	 */
	Date time;
	
	/**
	 * @param x x-coordinate of a point in map
	 * @param y y-coordinate of a point in map
	 * @param time A point in time
	 */
	public PositionAtTime(double x, double y, Date time) {
		this.x = x;
		this.y = y;
		this.time = time;
	}
	
	/**
	 * Used to format the time in {@link PositionAtTime#time} when it is written to an output. 
	 */
	public static final DateFormat format = new SimpleDateFormat("HH:mm:ss");
	
	/**
	 * Produces a human-readable representation of the object. For debugging purposes.
	 */
	@Override
	public String toString() {
		return "Time: " + ( (time==null) ? "null" : format.format(time)) + ", x: " + Double.toString(x) + ", y: " + Double.toString(y);
	}
	
	
}
