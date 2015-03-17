package cz.filipekt.jdcv;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javafx.animation.Animation.Status;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import cz.filipekt.jdcv.gui_logic.CloseSceneHandler;
import cz.filipekt.jdcv.gui_logic.ConfigFileLoader;
import cz.filipekt.jdcv.gui_logic.ControlsBarItemHandler;
import cz.filipekt.jdcv.gui_logic.FileChooserButton;
import cz.filipekt.jdcv.gui_logic.GraphicsPanelHandler;
import cz.filipekt.jdcv.gui_logic.ImportSceneHandler;
import cz.filipekt.jdcv.gui_logic.PlayButtonHandler;
import cz.filipekt.jdcv.gui_logic.RecordingHandler;
import cz.filipekt.jdcv.gui_logic.ScreenShotHandler;
import cz.filipekt.jdcv.gui_logic.TimeLineRateChanged;
import cz.filipekt.jdcv.gui_logic.TimeLineStatusHandler;
import cz.filipekt.jdcv.gui_logic.ZoomingHandler;
import cz.filipekt.jdcv.plugins.InfoPanel;
import cz.filipekt.jdcv.plugins.Plugin;
import cz.filipekt.jdcv.plugins.PluginWithPreferences;
import cz.filipekt.jdcv.prefs.GlobalPrefs;
import cz.filipekt.jdcv.util.CharsetNames;
import cz.filipekt.jdcv.util.Debug;
import cz.filipekt.jdcv.util.Resources;

/**
 * Main class of the application. Run it to show the visualized simulation data.
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
public class Visualizer extends Application {
	
	/**
	 * Preferred width of the map view, in pixels.
	 */
	private final double mapWidth = 1000.0;
	
	/**
	 * Preferred height of the map view, in pixels.
	 */
	private final double mapHeight = 800.0;
	
	/**
	 * Well prepared simulation data and settings, i.e. this is what will be visualized
	 */
	private MapScene scene;
	
	/**
	 * This method is used to specify what will be visualized. The {@link MapScene} instance
	 * given in the parameter contains a well prepared simulation data and settings.
	 * Further, this method takes care of some basic GUI operations associated with the
	 * above actions - such as making the relevant GUI parts accessible or setting up
	 * the slider at the bottom. 
	 * @param newScene Well prepared simulation data and settings
	 * @param min Starting simulation time
	 * @param max Ending simulation time
	 */
	public void setScene(final MapScene newScene, double min, double max) {
		if (newScene == null){
			showNoMap();
			controlsBar.setDisable(true);
			graphicsColumn.setDisable(true);
			switchablePanel.setDisable(true);
			timelineSlider.setDisable(true);
			if ((scene != null) && (scene.getTimeLine() != null)){
				scene.getTimeLine().stop();
				scene.getTimeLine().currentTimeProperty().removeListener(timelineToSliderListener);
				timelineSlider.valueProperty().removeListener(sliderToTimelineListener);
			}
		} else {
			ScrollPane mapScrollPane = newScene.getMapPane();
			mapPane.getChildren().clear();
			mapPane.getChildren().add(mapScrollPane);
			controlsBar.setDisable(false);
			graphicsColumn.setDisable(false);
			switchablePanel.setDisable(false);
			timelineSlider.setDisable(false);
			setSliderParameters(min, max);
			timelineToSliderListener = new ChangeListener<Duration>() {

				@Override
				public void changed(ObservableValue<? extends Duration> arg0,
						Duration oldValue, Duration newValue) {
					double millis = newValue.toMillis();
					timelineSlider.setValue(newScene.convertToSimulationTime(millis));
				}
			};
			newScene.getTimeLine().currentTimeProperty().addListener(timelineToSliderListener);
			sliderToTimelineListener = new ChangeListener<Number>() {

				@Override
				public void changed(ObservableValue<? extends Number> arg0,
						Number oldValue, Number newValue) {
					Duration time = new Duration(newScene.convertToVisualizationTime(newValue.doubleValue()));
					newScene.getTimeLine().jumpTo(time);
				}
			};
			timelineSlider.valueProperty().addListener(sliderToTimelineListener);
		}
		this.scene = newScene;
		providePreferencesToPlugins();
	}
	
	/**
	 * Provides the preferences objects to all available plugins that 
	 * extend {@link PluginWithPreferences} 
	 */
	private void providePreferencesToPlugins(){
		for (Plugin plugin : plugins){
			if (plugin instanceof PluginWithPreferences){
				PluginWithPreferences plugin2 = (PluginWithPreferences)plugin;
				if (scene == null){
					plugin2.setPreferences(null);
					plugin2.setGlobalPrefs(null);
				} else {
					plugin2.setPreferences(scene.getPreferences());					
					plugin2.setGlobalPrefs(new GlobalPrefs(scene, null));
				}
			}
		}
	}
	
	/**
	 * Sets the parameters of the slider appearing in the bottom of the application window.
	 * @param min Starting simulation time
	 * @param max Ending simulation time
	 */
	private void setSliderParameters(double min, double max){
		double diff = max - min;
		timelineSlider.setMin(min);
		timelineSlider.setMax(max);
		timelineSlider.setValue(0);
		timelineSlider.setShowTickLabels(true);
		timelineSlider.setShowTickMarks(true);
		timelineSlider.setMajorTickUnit(diff/4);
		timelineSlider.setMinorTickCount(6);
		timelineSlider.setBlockIncrement(diff/10);
	}
	
	/**
	 * Makes sure the changes in visualization timeline current time are projected to
	 * the position of the slider in the main window
	 */
	private ChangeListener<Duration> timelineToSliderListener;
	
	/**
	 * Makes sure that the changes in the value of the slider (in the main window) are
	 * projected to the value of the curren time of the visualization timeline
	 */
	private ChangeListener<Number> sliderToTimelineListener;
	
	/**
	 * @return The map that is being visualized, coupled with some view parameters.
	 * @see {@link Visualizer#scene}
	 */
	public MapScene getScene(){
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
	 * Width and height of the play/pause icon shown inside the corresponding button 
	 * in the controls tool bar.
	 */
	private final double playIconSize = 20.0;
	
	/**
	 * Sets all the controls contained in the graphics columns to to the default values.
	 * The references to those {@link Node} instances that are affected are stored inside 
	 * this implementation, which prevents the need to store the references in separate 
	 * class variables, which would make the class messy.
	 */
	private Runnable graphicsColumnDefaults;
	
	/**
	 * Sets all the controls contained in the graphics columns to to the default values.
	 * The references to those {@link Node} instances that are affected are stored inside 
	 * this implementation, which prevents the need to store the references in separate 
	 * class variables, which would make the class messy.
	 * @see {@link Visualizer#graphicsColumnDefaults}
	 */
	public void setGraphicsColumnDefaults() {
		graphicsColumnDefaults.run();
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
		MenuItem importSceneItem = new MenuItem("Import Scene"); 
		importSceneItem.setDisable(false);
		MenuItem closeThisSceneItem = new MenuItem("Close This Scene");
		closeThisSceneItem.setDisable(true);
		importSceneItem.setOnAction(new ImportSceneHandler(importSceneItem, closeThisSceneItem, this));
		closeThisSceneItem.setOnAction(new CloseSceneHandler(importSceneItem, closeThisSceneItem, this));
		fileMenu.getItems().addAll(importSceneItem, closeThisSceneItem);
		Menu optionsMenu = new Menu("Options");
		MenuItem scriptingPanel = new MenuItem("Open Console");
		scriptingPanel.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent arg0) {
				Console.getInstance().showScriptingConsole(Visualizer.this);
			}
		});
		optionsMenu.getItems().addAll(scriptingPanel);
		Menu viewMenu = new Menu("View");
		MenuItem controlsPanel = new MenuItem("Controls Panel");
		ImageView checkBoxImage = Resources.getImageView("checkmark.png", checkBoxSize);
		controlsPanel.setGraphic(checkBoxImage);	
		controlsPanel.setOnAction(new ControlsBarItemHandler(controlsPanel, checkBoxImage, this));
		final MenuItem graphicsPanel = new MenuItem("Graphics Panel");	
		final ImageView checkBoxImage2 = Resources.getImageView("checkmark.png", checkBoxSize);
		graphicsPanel.setGraphic(checkBoxImage2);
		graphicsPanel.setOnAction(new GraphicsPanelHandler(graphicsPanel, checkBoxImage2, this));
		viewMenu.getItems().addAll(controlsPanel, graphicsPanel);
		menuBar.getMenus().addAll(fileMenu, optionsMenu, viewMenu);
		return menuBar;
	}
	
	/**
	 * The {@link Node} that is shown whenever no map scene is open.
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
	 * Shows the "import scene" menu in the central part of the application window
	 * @see {@link Visualizer#importSceneGrid}
	 */
	public void showImportScene(){
		mapPane.getChildren().clear();
		mapPane.getChildren().add(importSceneGrid);
	}
	
	/**
	 * Shows the "empty" panel (which actually contains a few basic instructions) in the
	 * central part of the application window
	 */
	public void showNoMap(){
		mapPane.getChildren().clear();
		mapPane.getChildren().add(noMapNode);
	}
	
	/**
	 * Sets up the drag&drop functionality for the input fields where user defines the input files
	 * @param fields The fields where user defines the input files
	 */
	private void setUpDragNDrop(List<TextField> fields){
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
	}
	
	/**
	 * This encoding is the default selection in the combo-boxes on the scene-import page.
	 */
	private final String preferredEncoding = "UTF-8";
	
	/**
	 * The preferred width of the combo-boxes for selecting the input file encoding
	 */
	private final double encodingBoxWidth = 90.0;

	/**
	 * Builds the {@link Visualizer#importSceneGrid}.
	 * @param timeLineStatus Called whenever the visualization is started, paused or stopped
	 * @return The {@link GridPane} to be used for importing new scenes.
	 */
	private GridPane createImportSceneGrid(ChangeListener<Status> timeLineStatus, 
			ChangeListener<Number> timeLineRate){
		GridPane pane = new GridPane();
		List<Label> labels = new ArrayList<>();
		labels.add(new Label("Network definition XML:"));
		labels.add(new Label("Event log:"));
		labels.add(new Label("Ensemble event log:"));
		final List<TextField> fields = new ArrayList<>();
		List<ComboBox<String>> charsets = new ArrayList<>();
		List<Button> chooserButtons = new ArrayList<>();
		for (int i = 0; i < labels.size(); i++){
			fields.add(new TextField());
			chooserButtons.add(new Button("Select.."));
			ComboBox<String> charsetBox = new ComboBox<String>();
			charsetBox.getItems().addAll(CharsetNames.get());
			charsetBox.getSelectionModel().select(preferredEncoding);
			charsetBox.setPrefWidth(encodingBoxWidth);
			charsets.add(charsetBox);
		}
		for (TextField field : fields){
			field.setPrefWidth(inputFieldsWidth);
		}
		setUpDragNDrop(fields);
		for (int i = 0; i < chooserButtons.size(); i++){
			Button button = chooserButtons.get(i);
			TextField field = fields.get(i);
			button.setOnMouseClicked(new FileChooserButton(stage, field, "Select XML file"));
		}
		int row = 0;
		for (int i = 0; i < labels.size(); i++){
			Label label = labels.get(i);
			TextField field = fields.get(i);
			ComboBox<String> charsetBox = charsets.get(i);
			Button button = chooserButtons.get(i);
			pane.add(label, 0, row);
			pane.add(field, 1, row);
			pane.add(charsetBox, 2, row);
			pane.add(button, 3, row);
			row += 1;
		}
		
		Label durationLabel = new Label("Target duration (seconds):");
		TextField durationField = new TextField();
		pane.add(durationLabel, 0, row);
		pane.add(durationField, 1, row);		
		row += 1;
		
		Label onlyComponentsLabel = new Label("Show only the injected JDEECo components:");
		CheckBox onlyComponentsBox = new CheckBox();
		onlyComponentsBox.setSelected(true);
		pane.add(onlyComponentsLabel, 0, row);
		pane.add(onlyComponentsBox, 1, row);
		row += 1;
		
		Label startAtLabel = new Label("Start at time (seconds):");
		TextField startAtField = new TextField();
		pane.add(startAtLabel, 0, row);
		pane.add(startAtField, 1, row);
		row += 1;
		
		Label endAtLabel = new Label("End at time (seconds):");
		TextField endAtField = new TextField();
		pane.add(endAtLabel, 0, row);
		pane.add(endAtField, 1, row);
		row += 1;
		
		row += 1;
		String line = "----------";
		Label orLabel = new Label(line + " OR " + line);
		pane.add(orLabel, 0, row, 2, 1);
		GridPane.setHalignment(orLabel, HPos.CENTER);
		row += 2;
		
		Label configFileLabel = new Label("Specify Configuration File:");
		final TextField configFileField = new TextField();
		configFileField.setPrefWidth(inputFieldsWidth);
		setUpDragNDrop(Arrays.asList(configFileField));
		final ComboBox<String> configFileCharsets = new ComboBox<String>();
		configFileCharsets.getItems().addAll(CharsetNames.get());
		configFileCharsets.getSelectionModel().select(preferredEncoding);
		configFileCharsets.setPrefWidth(encodingBoxWidth);
		Button configFileSelect = new Button("Select..");
		configFileSelect.setOnMouseClicked(new FileChooserButton(stage, configFileField, "Select Configuration File"));
		Button configFileLoad = new Button("Load!");
		configFileLoad.setOnAction(new ConfigFileLoader(configFileField, configFileCharsets, fields, 
				charsets, durationField));
		pane.add(configFileLabel, 0, row);
		pane.add(configFileField, 1, row);
		pane.add(configFileCharsets, 2, row);
		pane.add(configFileSelect, 3, row);
		pane.add(configFileLoad, 4, row);
		row += 2;
		
		if (Debug.debugModeOn){
			configFileField.setText("C:/diplomka/JDEECoVisualizer-master/example_data/config.txt");
		}
		
		Button okButton = new Button("OK");
		okButton.setOnMouseClicked(new SceneBuilder(fields, okButton, onlyComponentsBox, pane, 
				Visualizer.this, durationField, timeLineStatus, timeLineRate, startAtField, endAtField));
		pane.add(okButton, 1, row);
		pane.setAlignment(Pos.CENTER);
		pane.setHgap(importSceneGridHGap);
		pane.setVgap(importSceneGridVGap);
		return pane;
	}
	
	/**
	 * Called whenever the visualization is started, paused or stopped
	 */
	private ChangeListener<Status> timeLineStatus;
	
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
	 * Difference by which the {@link Timeline#rateProperty()} will be changed when
	 * the fast forward or rewind buttons are clicked.
	 */
	private final double timeLineRateStep = 0.5;
	
	/**
	 * @return Difference by which the {@link Timeline#rateProperty()} will be changed when
	 * the fast forward or rewind buttons are clicked.
	 * @see {@link Visualizer#timeLineRateStep}
	 */
	public double getTimeLineRateStep() {
		return timeLineRateStep;
	}

	/**
	 * Called whenever the visualization is sped up or down
	 */
	private ChangeListener<Number> timeLineRate;
	
	/**
	 * Constructs the tool bar for zooming, pausing, forwarding etc. the simulation visualization.
	 * It is shown at the bottom of the main window.
	 * @return The tool bar containing the various zooming, pausing, forwarding etc. options
	 * @throws IOException Unless the application source folder contents have been changed in 
	 * any way by the user, this exception will never be thrown.
	 */
	private HBox createControlsBar() throws IOException{
		ImageView pauseImage = Resources.getImageView("video-pause.png", playIconSize);
		ImageView playImage = Resources.getImageView("video-play.png", playIconSize);
		Button playButton = new Button();
		final Button stopButton = new Button();
		playButton.setGraphic(playImage);
		playButton.setOnMouseClicked(new PlayButtonHandler(this));
		timeLineStatus = new TimeLineStatusHandler(playButton, stopButton, playImage, pauseImage);
		final Label speedLabel = new Label("Speed: 1.0x");
		speedLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent arg0) {
				if (scene != null){
					
				}
			}
		});
		timeLineRate = new ChangeListener<Number>() {
			
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1,
					Number arg2) {
				speedLabel.setText("Speed: " + arg2.doubleValue() + "x");
			}
		};
		ImageView ffdImage = Resources.getImageView("fast-forward.png", playIconSize);
		Button ffdButton = new Button("Speed up", ffdImage);
		ImageView rwImage = Resources.getImageView("rewind.png", playIconSize);
		Button rwButton = new Button("Speed down", rwImage);
		ffdButton.setOnMouseClicked(new TimeLineRateChanged(true, this));
		rwButton.setOnMouseClicked(new TimeLineRateChanged(false, this));
		ImageView zoomInImage = Resources.getImageView("zoom-in.png", playIconSize);
		Button zoomInButton = new Button("Zoom IN", zoomInImage);
		zoomInButton.setOnMouseClicked(new ZoomingHandler(1.2, this));
		ImageView zoomOutImage = Resources.getImageView("zoom-out.png", playIconSize);
		Button zoomOutButton = new Button("Zoom OUT", zoomOutImage);
		zoomOutButton.setOnMouseClicked(new ZoomingHandler(1/1.2, this));
		ImageView stopImage = Resources.getImageView("stop-black.png", playIconSize);
		stopButton.setDisable(true);
		stopButton.setGraphic(stopImage);
		stopButton.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event arg0) {
				Timeline timeline = scene.getTimeLine();
				if ((timeline.getStatus() == Status.RUNNING) || (timeline.getStatus() == Status.PAUSED)){
					timeline.stop();
					stopButton.setDisable(true);
				}
			}
		});
		
		HBox hbox = new HBox(speedLabel, rwButton, playButton, stopButton, ffdButton, zoomInButton, zoomOutButton); 	
		hbox.setSpacing(10);
		hbox.setAlignment(Pos.CENTER_RIGHT);
		return hbox;
	}
	
	/**
	 * The stage used by this application.
	 */
	private Stage stage;
	
	/**
	 * @return The stage used by this application.
	 * @see {@link Visualizer#stage}
	 */
	public Stage getStage() {
		return stage;
	}

	/**
	 * The tool bar containing the various zooming, pausing, forwarding etc. options
	 */
	private final HBox controlsBar;
	
	/**
	 * The tool bar shown on the top of the application, allowing various operations such as
	 * loading a scene, setting the visibility of other tool bars, etc.
	 */
	private final MenuBar menuBar;
	
	/**
	 * @return The tool bar containing the various zooming, pausing, forwarding etc. options
	 * @see {@link Visualizer#controlsBar}
	 */
	public HBox getControlsBar() {
		return controlsBar;
	}

	/**
	 * The root element of the main {@link Scene} of the application.
	 */
	private final VBox rootPane = new VBox();
	
	/**
	 * @return The root element of the main {@link Scene} of the application.
	 * @see {@link Visualizer#rootPane}
	 */
	public VBox getRootPane(){
		return rootPane;
	}
	
	/**
	 * @return Builds and returns the column, shown next to the map on the left side, containing various graphics options.
	 * @throws IOException Unless the application source folder contents have been changed in 
	 * any way by the user, this exception will never be thrown. 
	 */
	private VBox createGraphicsColumn() throws IOException{
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
		ImageView snapShotImage = Resources.getImageView("screenshot.png", playIconSize);
		Button screenShotButton = new Button("Snapshot", snapShotImage);
		screenShotButton.setOnMouseClicked(new ScreenShotHandler(this));
		panel.getChildren().add(screenShotButton);		
		ImageView recordStartImage = Resources.getImageView("record.png", playIconSize);
		ImageView recordStopImage = Resources.getImageView("stop.png", playIconSize);
		Button recordButton = new Button("Record", recordStartImage);
		recordButton.setDisable(true);	// TODO fix the recording functionality
		recordButton.setOnMouseClicked(new RecordingHandler(recordButton, recordStartImage, recordStopImage, this));
		panel.getChildren().add(recordButton);
		for (Node node : panel.getChildren()){
			VBox.setMargin(node, new Insets(graphicsItemsMargin, 0, graphicsItemsMargin, 2 * graphicsItemsMargin));
		}
		graphicsColumnDefaults = new Runnable() {
			
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
	 * @return The stripe of the application view that contains the {@link Visualizer#graphicsColumn},
	 * {@link MapScene#mapPane} and others
	 * @see {@link Visualizer#middleRow}
	 */
	public HBox getMiddleRow() {
		return middleRow;
	}

	/**
	 * The column, shown on the left side of the map, containing various graphics options
	 */
	private final VBox graphicsColumn;
	
	/**
	 * @return The column, shown on the left side of the map, containing various graphics options
	 * @see {@link Visualizer#graphicsColumn}
	 */
	public VBox getGraphicsColumn(){
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
	 * Shows the current position on a timeline, like any slider in most media players.
	 */
	private final Slider timelineSlider = new Slider();
	
	/**
	 * Parent container for {@link Visualizer#timelineSlider}
	 */
	private final Pane sliderWrapper;
	
	/**
	 * Initializer for {@link Visualizer#sliderWrapper}.
	 * @return A {@link Pane} holding the {@link Visualizer#sliderWrapper} and, 
	 * possibly, some belonging controls 
	 */
	private Pane createSliderWrapper(){
		HBox res =  new HBox(timelineSlider);
		HBox.setHgrow(timelineSlider, Priority.ALWAYS);
		HBox.setMargin(timelineSlider, new Insets(0, 50, 0, 50));
		return res;
	}
	
	/**
	 * The panel on the right side of the window, allowing various sub-panels to be viewed.
	 * The panel which has been selected by clicking the corresponding button is viewed.
	 */
	private final Node switchablePanel;
	
	/**
	 * Preferred width of the side panel
	 */
	private final double sidePanelWidth = 300.0;
	
	/**
	 * @return Plugins loaded from the plugins directory
	 */
	private Collection<Plugin> loadPlugins(){
		Collection<Plugin> res = new HashSet<>();
		ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class);
		for (Plugin plugin : loader){
			res.add(plugin);
		}
		return res;
	}
	
	/**
	 * Initializes the {@link Visualizer#switchablePanel}. 
	 * @return Newly constructed switchable side-panel
	 */
	private Node createSwitchablePanel(){	
		plugins.add(InfoPanel.getInstance());
//		plugins.add(FilterPanel.getInstance());
		Collection<Plugin> loadedPlugins = loadPlugins();
		plugins.addAll(loadedPlugins);
		
		final Pane mainPanel = new StackPane();
		final ToolBar toolBar = new ToolBar();
		toolBar.setPrefWidth(sidePanelWidth);
		mainPanel.setPrefWidth(sidePanelWidth);
		VBox pane = new VBox(toolBar, mainPanel);
		VBox.setVgrow(toolBar, Priority.NEVER);
		VBox.setVgrow(mainPanel, Priority.ALWAYS);
		pane.setFillWidth(true);
		
		plugins2.clear();
		for (Plugin plugin : plugins){
			Button button = createPluginButton(plugin);
			plugins2.put(button, plugin.getPanel());
		}
		for (final Button button : plugins2.keySet()){
			button.setStyle("-fx-background-color: white");
			button.setOnAction(new EventHandler<ActionEvent>() {
				
				@Override
				public void handle(ActionEvent arg0) {
					for (Button button : plugins2.keySet()){
						button.setStyle("-fx-background-color: white");
					}
					button.setStyle("-fx-background-color: grey");
					mainPanel.getChildren().clear();
					mainPanel.getChildren().add(plugins2.get(button));
				}
			});			
			toolBar.getItems().add(button);
		}
		return pane;
	}
	
	/**
	 * Width (and height) of the images shown inside the panel-switching buttons
	 */
	private final double pluginButtonImageWidth = 20;
	
	/**
	 * For the given plugin, creates the corresponding button used for switching the plugin on.
	 * @param plugin Plugin to be turned on/off by switching the returned button
	 * @return Button for switching on/off the specified plugin
	 */
	private Button createPluginButton(Plugin plugin){
		InputStream imageStream = plugin.getThumbnail();
		ImageView thumbNail;
		try {
			Image image = new Image(imageStream, pluginButtonImageWidth, pluginButtonImageWidth, false, false);
			thumbNail = new ImageView(image);
		} catch (Exception ex){
			thumbNail = new ImageView();
		}
		Button res = new Button(plugin.getName(), thumbNail);		
		return res;
	}
	
	/**
	 * The plugins available for the application
	 */
	private final Collection<Plugin> plugins = new HashSet<>();
	
	/**
	 * Each button is associated with a plugin (contained in {@link Visualizer#plugins}).
	 * The button is mapped here to the main panel of the associated plugin.
	 */
	private final Map<Button,Node> plugins2 = new HashMap<>();
	
	/**
	 * A simple dummy implementation of {@link Plugin}
	 */
	static class PluginExpl implements Plugin {
		
		@Override
		public InputStream getThumbnail() {
			return Resources.getResourceInputStream("success.png");
		}
		
		@Override
		public Node getPanel() {
			return new FlowPane(new Label("Lorem ipsum"));
		}
		
		@Override
		public String getName() {
			return "Test";
		}
	};
	
	/**
	 * Selects the info-panel in the panel switching menu on the right side of the 
	 * application window
	 */
	private void selectInfoPanel(){
		for (Button button : plugins2.keySet()){
			if (plugins2.get(button) == InfoPanel.getInstance().getPanel()){
				button.fire();
				break;
			}
		}
	}
	
	/**
	 * Builds the GUI, should only be called by the JavaFX runtime.
	 */
	@Override
	public void start(Stage stage) throws IOException {	
		this.stage = stage;
		selectInfoPanel();
		timelineSlider.setDisable(true);
		noMapNode.setPrefSize(mapWidth, mapHeight);
		importSceneGrid.setPrefSize(mapWidth, mapHeight);
		controlsBar.setDisable(true);
		switchablePanel.setDisable(true);
		Label helpLabel = new Label("To import new simulation data, click File -> Import Scene");
		helpLabel.setPadding(new Insets(10, 10, 10, 10));
		noMapNode.getChildren().add(helpLabel);	
		mapPane.getChildren().add(noMapNode);
		graphicsColumn.setPrefWidth(graphicsColumnWidth);
		graphicsColumn.setMinWidth(graphicsColumnWidth);
		middleRow.setFillHeight(true);
		middleRow.getChildren().addAll(graphicsColumn, mapPane, switchablePanel);
		VBox.setVgrow(menuBar, Priority.NEVER);
		VBox.setVgrow(middleRow, Priority.ALWAYS);
		VBox.setVgrow(sliderWrapper, Priority.NEVER);
		VBox.setVgrow(controlsBar, Priority.NEVER);
		HBox.setHgrow(graphicsColumn, Priority.NEVER);
		HBox.setHgrow(mapPane, Priority.ALWAYS);
		HBox.setHgrow(switchablePanel, Priority.NEVER);
		rootPane.getChildren().clear();
		rootPane.getChildren().addAll(menuBar, middleRow, sliderWrapper, controlsBar);
		Scene fxScene = new Scene(rootPane, Color.WHITE);
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
	    stage.setTitle("JDEECo Visualizer");
	    InputStream iconStream = Resources.getResourceInputStream("cup.png");
	    if (iconStream != null){
	    	Image icon = new Image(iconStream);
	    	stage.getIcons().add(icon);
	    }
	    stage.show();
	}

	/**
	 * Initializes some of the panels and tool bars.
	 * @throws IOException Unless the application source folder contents have been changed in 
	 * any way by the user, this exception will never be thrown.
	 */
	public Visualizer() throws IOException {				
		menuBar = createMenuBar();	
		controlsBar = createControlsBar();
		graphicsColumn = createGraphicsColumn();
		importSceneGrid = createImportSceneGrid(timeLineStatus, timeLineRate);
		sliderWrapper = createSliderWrapper();
		switchablePanel = createSwitchablePanel();
	}			
	
}
