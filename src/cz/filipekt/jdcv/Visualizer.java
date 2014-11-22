package cz.filipekt.jdcv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
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
import javafx.stage.Stage;

/**
 * Main class of the application. Run it to show the simulation data visualization.
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
	
	/**
	 * Sets the map that will be visualized by this application
	 * @param scene The map that will be visualized by this application
	 */
	void setScene(MapScene scene) {
		this.scene = scene;
	}

	/**
	 * Main entry point of the JDEECoVisualizer application.
	 * @param args Program arguments. Ignored by the application.
	 */
	public static void main(String[] args){
		launch(args);
	}
	
	/**
	 * Constructs the main menu bar of the application.
	 * @return The main menu bar of the application.
	 * @throws IOException Unless the application source folder contents have been changed in 
	 * any way by the user, this exception will never be thrown.
	 */
	private MenuBar createMenuBar() throws IOException {
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
	 * Container for map view, or if no map is currently view, for a dialog for loading a map.
	 */
	private final Pane mapPane = new StackPane();
	
	/** 
	 * @return Preferred width of the map view, in pixels.
	 * @see {@link Visualizer#mapWidth}
	 */
	double getMapWidth() {
		return mapWidth;
	}

	/**
	 * @return Preferred height of the map view, in pixels.
	 * @see {@link Visualizer#mapHeight}
	 */
	double getMapHeight() {
		return mapHeight;
	}

	/**
	 * @return Container for map view, or if no map is currently view, for a dialog for loading a map.
	 * @see {@link Visualizer#mapPane}
	 */
	Pane getMapPane() {
		return mapPane;
	}

	/**
	 * Builds the {@link Visualizer#importSceneGrid}.
	 * @return The {@link GridPane} to be used for importing new scenes.
	 */
	private GridPane createImportSceneGrid(){
		GridPane pane = new GridPane();
		List<Label> labels = new ArrayList<>();
		labels.add(new Label("Location of the network definition XML:"));
		labels.add(new Label("Location of the facilities definition XML:"));
		labels.add(new Label("Location of the population/plans definition XML:"));
		List<TextField> fields = new ArrayList<>();
		List<Button> chooserButtons = new ArrayList<>();
		for (int i = 0; i < labels.size(); i++){
			fields.add(new TextField());
			chooserButtons.add(new Button("Select.."));
		}
		for (TextField field : fields){
			field.setPrefWidth(inputFieldsWidth);
		}
		for (int i = 0; i < chooserButtons.size(); i++){
			Button button = chooserButtons.get(i);
			TextField field = fields.get(i);
			button.setOnMouseClicked(new ButtonXmlChooserHandler(stage, field));
		}
		int row = 0;
		for (int i = 0; i < labels.size(); i++){
			Label label = labels.get(i);
			TextField field = fields.get(i);
			Button button = chooserButtons.get(i);
			pane.add(label, 0, row);
			pane.add(field, 1, row);
			pane.add(button, 2, row);
			row += 1;
		}
		Button okButton = new Button("OK");
		okButton.setOnMouseClicked(new OkButtonEventHandler(fields, okButton, pane, Visualizer.this));
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
	 * @return The toolbar containing zooming options.
	 */
	private HBox createZoomBar(){
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
	 * @return The toolbar containing the zooming options.
	 * @see {@link Visualizer#zoomBar}
	 */
	HBox getZoomBar() {
		return zoomBar;
	}

	/**
	 * The root element of the main {@link Scene} of the application.
	 */
	private final VBox vbox = new VBox();
	
	/**
	 * Builds the GUI, should only be called by the JavaFX runtime.
	 * @throws IOException Unless the application source folder contents have been changed in 
	 * any way by the user, this exception will never be thrown.
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
		MenuBar menuBar = createMenuBar();	
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

	/**
	 * Initializes some of the panels and tool bars.
	 */
	public Visualizer() {		
		importSceneGrid = createImportSceneGrid();
		zoomBar = createZoomBar();
	}			
	
}
