package cz.filipekt.jdcv;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
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
	 * @return The map that is being visualized, coupled with some view parameters.
	 * @see {@link Visualizer#scene}
	 */
	MapScene getScene(){
		return scene;
	}

	/**
	 * Main entry point of the JDEECoVisualizer application.
	 * @param args Program arguments. Ignored by the application.
	 */
	public static void main(String[] args){
		launch(args);
	}
	
	/**
	 * Width and height of the check mark shown next to the items in the View menu.
	 */
	private final double checkBoxSize = 18.0;
	
	/**
	 * Marks whether the application runs in debugging mode
	 */
	private final boolean debug = false;
	
	/**
	 * Sets all the controls contained in the graphics columns to to the default values.
	 * The references to those {@link Node} instances that are affected are stored inside 
	 * this implementation, which prevents the need to store the references in separate 
	 * class variables, which would make the class messy.
	 */
	private Runnable setGraphicsColumnDefaults;
	
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
				scene.getTimeLine().stop();
				scene = null;
				closeThisSceneItem.setDisable(true);
				importSceneItem.setDisable(false);	
				zoomBar.setDisable(true);
				graphicsColumn.setDisable(true);
				setGraphicsColumnDefaults.run();
			}
		});
		fileMenu.getItems().addAll(importSceneItem, closeThisSceneItem);
		Menu editMenu = new Menu("Edit");
		Menu viewMenu = new Menu("View");
		final MenuItem zoomPanel = new MenuItem("Zoom Panel");
		InputStream imageStream;
		InputStream imageStream2;
		if (debug){
			imageStream = Files.newInputStream(Paths.get("C:/diplomka/JDEECoVisualizer/resources/checkmark.png"));
			imageStream2 = Files.newInputStream(Paths.get("C:/diplomka/JDEECoVisualizer/resources/checkmark.png"));
		} else {
			imageStream = getClass().getResourceAsStream("/resources/checkmark.png");
			imageStream2 = getClass().getResourceAsStream("/resources/checkmark.png");
		}
		final ImageView checkBoxImage = new ImageView(new Image(imageStream, checkBoxSize, checkBoxSize, true, true));
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
		final MenuItem graphicsPanel = new MenuItem("Graphics Panel");		
		final ImageView checkBoxImage2 = new ImageView(new Image(imageStream2, checkBoxSize, checkBoxSize, true, true));
		graphicsPanel.setGraphic(checkBoxImage2);
		graphicsPanel.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent arg0) {
				if (panelShown){
					if (middleRow.getChildren().contains(graphicsColumn)){
						middleRow.getChildren().remove(graphicsColumn);
						panelShown = false;
						graphicsPanel.setGraphic(null);
					}
				} else {
					if (!middleRow.getChildren().contains(graphicsColumn)){
						middleRow.getChildren().add(0, graphicsColumn);
						panelShown = true;
						graphicsPanel.setGraphic(checkBoxImage2);
					}
				}
			}
			
			private boolean panelShown = true;
			
		});
		viewMenu.getItems().addAll(zoomPanel, graphicsPanel);
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
		labels.add(new Label("Location of the event log:"));
		final List<TextField> fields = new ArrayList<>();
		List<Button> chooserButtons = new ArrayList<>();
		for (int i = 0; i < labels.size(); i++){
			fields.add(new TextField());
			chooserButtons.add(new Button("Select.."));
		}
		for (TextField field : fields){
			field.setPrefWidth(inputFieldsWidth);
		}
		if (debug){
			fields.get(0).setText("C:/diplomka/output_network.xml");
			fields.get(1).setText("C:/diplomka/events.xml");
		}
		for (final TextField field : fields){
			field.setOnDragOver(new EventHandler<DragEvent>() {
	
				@Override
				public void handle(DragEvent event) {
					event.acceptTransferModes(TransferMode.COPY);
				}
			});
			field.setOnDragDropped(new EventHandler<DragEvent>() {
	
				@Override
				public void handle(DragEvent event) {
					Dragboard dragBoard = event.getDragboard();
					if (dragBoard.hasFiles()){
						for (File file : dragBoard.getFiles()){
							field.setText(file.getAbsolutePath().toString());
						}
						event.setDropCompleted(true);
						event.consume();
					}
				}
			});
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
		
		Label durationLabel = new Label("Target duration (seconds):");
		ComboBox<Integer> durationsBox = new ComboBox<>();
		durationsBox.setEditable(false);
		durationsBox.getItems().addAll(10, 30, 60, 120, 300, 3600);
		durationsBox.setValue(60);
		pane.add(durationLabel, 0, row);
		pane.add(durationsBox, 1, row);		
		row += 1;
		
		Button okButton = new Button("OK");
		okButton.setOnMouseClicked(new SceneBuilder(fields, okButton, pane, Visualizer.this, durationsBox));
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
					scene.changeZoom(1.2);
				}
			}
		});
		Button zoomOutButton = new Button("Zoom OUT");
		zoomOutButton.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event arg0) {
				if (scene != null){
					scene.changeZoom(1/1.2);
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
	 * @return Builds and returns the column, shown on the left side of the map, containing various graphics options
	 */
	private VBox createGraphicsColumn(){
		VBox panel = new VBox();
		panel.setDisable(true);
		panel.setAlignment(Pos.TOP_LEFT);
		final CheckBox showNodesBox = new CheckBox("Show nodes");
		showNodesBox.setSelected(true);
		showNodesBox.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				if (scene != null){
					scene.setNodesVisible(showNodesBox.isSelected());
				}
			}
		});
		panel.getChildren().add(showNodesBox);
		final CheckBox showLinksBox = new CheckBox("Show links");
		showLinksBox.setSelected(true);
		showLinksBox.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent arg0) {
				if (scene != null){
					scene.setLinksVisible(showLinksBox.isSelected());
				}
			}
		});
		panel.getChildren().add(showLinksBox);
		VBox.setMargin(showNodesBox, new Insets(2 * graphicsItemsMargin, 0, graphicsItemsMargin, 2 * graphicsItemsMargin));
		VBox.setMargin(showLinksBox, new Insets(graphicsItemsMargin, 0, graphicsItemsMargin, 2 * graphicsItemsMargin));
		setGraphicsColumnDefaults = new Runnable() {
			
			@Override
			public void run() {
				showNodesBox.setSelected(true);
				showLinksBox.setSelected(true);
			}
		};
		return panel;
	}
	
	/**
	 * The stripe of the application view that contains the {@link Visualizer#graphicsColumn},
	 * {@link MapScene#mapPane} and others
	 */
	private final HBox middleRow = new HBox();
	
	/**
	 * The column, shown on the left side of the map, containing various graphics options
	 */
	private final VBox graphicsColumn;
	
	/**
	 * @return The column, shown on the left side of the map, containing various graphics options
	 * @see {@link Visualizer#graphicsColumn}
	 */
	VBox getGraphicsColumn(){
		return graphicsColumn;
	}
	
	/**
	 * Preferred width of the {@link Visualizer#graphicsColumn}
	 */
	private final double graphicsColumnWidth = 200.0;
	
	/**
	 * Margin of the elements inside the {@link Visualizer#graphicsColumn}
	 */
	private final double graphicsItemsMargin = 10;
	
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
		graphicsColumn.setPrefWidth(graphicsColumnWidth);
		graphicsColumn.setMinWidth(graphicsColumnWidth);
		middleRow.getChildren().addAll(graphicsColumn, mapPane);
		MenuBar menuBar = createMenuBar();	
		vbox.getChildren().clear();
		vbox.getChildren().addAll(menuBar, middleRow, zoomBar);
		Scene fxScene = new Scene(vbox, Color.WHITE);
		fxScene.heightProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> arg0,
					Number arg1, Number arg2) {
				double diff = arg2.doubleValue() - arg1.doubleValue();
				mapPane.setPrefHeight(mapPane.getPrefHeight() + diff);			
			}
		});
		fxScene.widthProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> arg0,
					Number arg1, Number arg2) {
				double diff = arg2.doubleValue() - arg1.doubleValue();
				mapPane.setPrefWidth(mapPane.getPrefWidth() + diff);
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
		mapPane.widthProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> arg0,
					Number arg1, Number arg2) {
				Node child = mapPane.getChildren().get(0);
				if (child instanceof Region){
					Region reg = (Region)child;
					reg.setPrefWidth(arg2.doubleValue());
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
		graphicsColumn = createGraphicsColumn();
	}			
	
}
