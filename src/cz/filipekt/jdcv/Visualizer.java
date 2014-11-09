package cz.filipekt.jdcv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * Run this class to show the map visualizer.
 * @author Tomas Filipek
 *
 */
public class Visualizer extends Application {
	
	/**
	 * Preferred width of the map view, in pixels.
	 */
	private final double mapWidth = 800.0;
	
	/**
	 * Preferred height of the map view, in pixels.
	 */
	private final double mapHeight = 600.0;
	
	/**
	 * The map that is being visualized, coupled with some view parameters.
	 */
	private MapScene scene;
	
	public static void main(String[] args){
		launch(args);
	}
	
	/**
	 * Constructs the main menu bar of the application.
	 * @return  
	 * @throws IOException 
	 */
	private MenuBar getMenuBar() throws IOException {
		MenuBar menuBar = new MenuBar();
		Menu fileMenu = new Menu("File");
		final MenuItem importSceneItem = new MenuItem("Import Scene"); 
		importSceneItem.setDisable(false);
		final MenuItem closeThisSceneItem = new MenuItem("Close This Scene");
		closeThisSceneItem.setDisable(true);
		importSceneItem.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent arg0) {					
				mapPane.getChildren().clear();
				mapPane.getChildren().add(importSceneGrid);				
				scene = null;
	            closeThisSceneItem.setDisable(false);
	            importSceneItem.setDisable(true);					           
			}
		});
		closeThisSceneItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				mapPane.getChildren().clear();
				mapPane.getChildren().add(noMapNode);
				scene = null;
				closeThisSceneItem.setDisable(true);
				importSceneItem.setDisable(false);	
				zoomBar.setDisable(true);
			}
		});
		fileMenu.getItems().addAll(importSceneItem, closeThisSceneItem);
		Menu editMenu = new Menu("Edit");
		Menu viewMenu = new Menu("View");
		final MenuItem zoomPanel = new MenuItem("Zoom Panel");
		final ImageView checkBoxImage = new ImageView(new Image(Files.newInputStream(Paths.get("checkmark01.png")), 20, 20, true, true));
		zoomPanel.setGraphic(checkBoxImage);
		zoomPanel.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent arg0) {
				if (barIsShown){
					if (vbox.getChildren().contains(zoomBar)){
						vbox.getChildren().remove(zoomBar);
						barIsShown = false;
						zoomPanel.setGraphic(null);
					}
				} else {
					if (!vbox.getChildren().contains(zoomBar)){
						vbox.getChildren().add(zoomBar);
						barIsShown = true;
						zoomPanel.setGraphic(checkBoxImage);
					}
				}
			}
			
			private boolean barIsShown = true;
		});
		viewMenu.getItems().addAll(zoomPanel);
		menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu);
		return menuBar;
	}
	
	/**
	 * This {@link Node} is shown whenever no map scene is open.
	 */
	private final Pane noMapNode = new VBox();
	
	/**
	 * Contains the controls where users can specify input XML files for the visualization. 
	 */
	private final GridPane importSceneGrid;

	/**
	 * Container for map view, or if no map is currently view, a dialog for loading a map.
	 */
	private final Pane mapPane = new StackPane();
	
	/**
	 * Makes sure that when the OK button is clicked (after specifying input XML files for the visualization),
	 * that the input data is processed and the requested visualizations are shown.
	 * @author Tom
	 *
	 */
	private class OkButtonEventHandler implements EventHandler<Event>{
		
		/**
		 * Field containing the path to the map XML definition file. 
		 */
		private final TextField mapField;
		
		/**
		 * The button with which this {@link EventHandler} is associated. 
		 */
		private final Button okButton;
		
		/**
		 * Shown when a new scene is being loaded.
		 */
		private final ProgressIndicator progIndicator = new ProgressIndicator(-1);
		
		/**
		 * The {@link Pane} which contains {@link OkButtonEventHandler#progIndicator} and {@link OkButtonEventHandler#okButton}.
		 */
		final GridPane pane;

		OkButtonEventHandler(TextField mapField, Button okButton,
				GridPane pane) {
			super();
			this.mapField = mapField;
			this.okButton = okButton;
			this.pane = pane;
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
		 * Removes {@link OkButtonEventHandler#progIndicator} from the {@link OkButtonEventHandler#pane} container.
		 */
		private void closeProgressIndiciator(){
			pane.getChildren().remove(progIndicator);
		}

		@Override
		public void handle(Event arg0) {
			if ((mapField.getText() == null) || (mapField.getText().isEmpty())){
				return;
			}	
			final String mapFieldText = mapField.getText();

			new Thread(){

				@Override
				public void run() {
					try {
						Platform.runLater(new Runnable() {
							
							@Override
							public void run() {								
								openProgressIndicator();
							}
						});
						
						Path mapXmlFile = Paths.get(mapFieldText);
						XMLextractor extractor = new XMLextractor(mapXmlFile);
						final XMLresult parsedXml = extractor.doExtraction();
						final MapScene scene = new MapScene(parsedXml, mapWidth, mapHeight);
						Platform.runLater(new Runnable() {
							
							@Override
							public void run() {									
								Visualizer.this.scene = scene;
								final ScrollPane mapScrollPane = scene.getMapPane();
								mapScrollPane.setPrefSize(scene.getTotalWidth(), scene.getTotalHeight());
								mapPane.getChildren().clear();
								mapPane.getChildren().add(mapScrollPane);	
								zoomBar.setDisable(false);
								closeProgressIndiciator();									
							}
						});
						Thread.sleep(1000);							
					} catch (InterruptedException | ParserConfigurationException | SAXException | IOException ex){
						ex.printStackTrace();	
						System.exit(1);
					}
				}											
			}.start();
		}		
		
	}
	
	/**
	 * Builds the {@link Visualizer#importSceneGrid}.
	 * @return
	 */
	private GridPane createImportSceneGrid(){
		final GridPane pane = new GridPane();
		int row = 0;
		Label mapLabel = new Label("Location of the map definition XML:");
		pane.add(mapLabel, 0, row);
		final TextField mapField = new TextField();
		mapField.setPrefWidth(inputFieldsWidth);
		pane.add(mapField, 1, row);
		Button fileChooserButton = new Button("Select..");
		fileChooserButton.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event arg0) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Open XML File");
				File res = fileChooser.showOpenDialog(stage);
				if (res != null){
					mapField.setText(res.getAbsolutePath().toString());
				}
			}
		});
		pane.add(fileChooserButton, 2, row);
		row += 1;
		final Button okButton = new Button("OK");
		okButton.setOnMouseClicked(new OkButtonEventHandler(mapField, okButton, pane));								
		pane.add(okButton, 1, row);
		pane.setAlignment(Pos.CENTER);
		pane.setHgap(importSceneGridHGap);
		pane.setVgap(importSceneGridVGap);
		return pane;
	}
	
	/**
	 * Value for the HGap parameter of {@link Visualizer#importSceneGrid}
	 */
	private final double importSceneGridHGap = 20;
	
	/**
	 * Value for the VGap parameter of {@link Visualizer#importSceneGrid}
	 */
	private final double importSceneGridVGap = 10;
	
	/**
	 * Width of the input fields used in {@link Visualizer#importSceneGrid}
	 */
	private final double inputFieldsWidth = 300;

	/**
	 * Constructs the toolbar for zooming in/out the map view.
	 * @return
	 */
	private HBox getZoomBar(){
		Button zoomInButton = new Button("Zoom IN");
		zoomInButton.setOnMouseClicked(new  EventHandler<Event>() {

			@Override
			public void handle(Event arg0) {
				if (scene != null){
					scene.changeZoom(0.2);
				}
			}
		});
		Button zoomOutButton = new Button("Zoom OUT");
		zoomOutButton.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event arg0) {
				if (scene != null){
					scene.changeZoom(-0.2);
				}
			}
		});
		HBox hbox = new HBox(zoomInButton, zoomOutButton); 	
		hbox.setAlignment(Pos.CENTER_RIGHT);
		return hbox;
	}
	
	/**
	 * The stage used by this application.
	 */
	private Stage stage;
	
	/**
	 * The toolbar containing the zooming options. 
	 */
	private final HBox zoomBar;
	
	/**
	 * The root element of the main {@link Scene} of the application.
	 */
	private final VBox vbox = new VBox();
	
	/**
	 * Builds the GUI, should only be called by the JavaFX runtime.
	 * @throws IOException 
	 */
	@Override 
	public void start(Stage stage) throws IOException {	
		this.stage = stage;
		noMapNode.setPrefSize(mapWidth, mapHeight);
		zoomBar.setDisable(true);
		Label helpLabel = new Label("To import new simulation data, click File -> Import Scene");
		helpLabel.setPadding(new Insets(10, 10, 10, 10));
		noMapNode.getChildren().add(helpLabel);	
		mapPane.getChildren().add(noMapNode);
		MenuBar menuBar = getMenuBar();	
		vbox.getChildren().clear();
		vbox.getChildren().addAll(menuBar, mapPane, zoomBar);
		Scene fxScene = new Scene(vbox, Color.WHITE);
		fxScene.heightProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> arg0,
					Number arg1, Number arg2) {
				double diff = arg2.doubleValue() - arg1.doubleValue();
				mapPane.setPrefHeight(mapPane.getPrefHeight() + diff);			
			}
		});
		mapPane.heightProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> arg0,
					Number arg1, Number arg2) {								
				Node child = mapPane.getChildren().get(0);
				if (child instanceof Region){
					Region reg = (Region)child;
					reg.setPrefHeight(arg2.doubleValue());
				} 
			}
		});
	    stage.setScene(fxScene);
	    stage.setTitle("Map Visualizer");
	    stage.show();
	}

	public Visualizer() throws ParserConfigurationException, SAXException, IOException {		
		importSceneGrid = createImportSceneGrid();
		zoomBar = getZoomBar();
	}			
	
}
