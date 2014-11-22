package cz.filipekt.jdcv;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

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
		PopulationHandler populationHandler = new PopulationHandler(linkHandler.getLinks(), facilitiesHandler.getFacilities());
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
	}
	
}