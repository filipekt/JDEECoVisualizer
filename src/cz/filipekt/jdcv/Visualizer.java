package cz.filipekt.jdcv;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation.Status;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
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
import javafx.scene.image.WritableImage;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;

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
	 * Width and height of the play/pause icon shown inside the corresponding button 
	 * in the controls tool bar.
	 */
	private final double playIconSize = 20.0;
	
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
	 * @param resourceName A resource to be loaded
	 * @return An {@link InputStream} instance reading from the specified resource
	 * @throws IOException When the specified resource could not be found or opened
	 */
	private InputStream getResourceInputStream(String resourceName) throws IOException{
		if (debug){
			return Files.newInputStream(Paths.get("C:/diplomka/JDEECoVisualizer/resources", resourceName));
		} else {
			return getClass().getResourceAsStream("/resources/" + resourceName);
		}
	}
	
	/**
	 * @param resourceFileName Filename of the desired image
	 * @param size Preferred width and height of the {@link ImageView} 
	 * @return The desired image wrapped in a {@link ImageView}
	 */
	private ImageView getImageView(String resourceFileName, double size){
		try {
			if (resourceFileName == null){
				throw new NullPointerException();
			}
			InputStream stream = getResourceInputStream(resourceFileName);
			return new ImageView(new Image(stream, size, size, true, true));
		} catch (Exception ex) { // if the specified image is not found
			return new ImageView();
		}
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
				scene.getTimeLine().stop();
				scene = null;
				closeThisSceneItem.setDisable(true);
				importSceneItem.setDisable(false);	
				controlsBar.setDisable(true);
				graphicsColumn.setDisable(true);
				setGraphicsColumnDefaults.run();
			}
		});
		fileMenu.getItems().addAll(importSceneItem, closeThisSceneItem);
		Menu editMenu = new Menu("Options");
		Menu viewMenu = new Menu("View");
		final MenuItem zoomPanel = new MenuItem("Controls Panel");
		final ImageView checkBoxImage = getImageView("checkmark.png", checkBoxSize);
		zoomPanel.setGraphic(checkBoxImage);				
		zoomPanel.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent arg0) {
				if (barIsShown){
					if (vbox.getChildren().contains(controlsBar)){
						vbox.getChildren().remove(controlsBar);
						barIsShown = false;
						zoomPanel.setGraphic(null);
					}
				} else {
					if (!vbox.getChildren().contains(controlsBar)){
						vbox.getChildren().add(controlsBar);
						barIsShown = true;
						zoomPanel.setGraphic(checkBoxImage);
					}
				}
			}
			
			private boolean barIsShown = true;
		});
		final MenuItem graphicsPanel = new MenuItem("Graphics Panel");	
		final ImageView checkBoxImage2 = getImageView("checkmark.png", checkBoxSize);
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
	 * @param timeLineStatus Called whenever the visualization is started, paused or stopped
	 * @return The {@link GridPane} to be used for importing new scenes.
	 */
	private GridPane createImportSceneGrid(ChangeListener<Status> timeLineStatus, ChangeListener<Number> timeLineRate){
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
		okButton.setOnMouseClicked(new SceneBuilder(fields, okButton, pane, Visualizer.this, durationsBox, timeLineStatus, timeLineRate));
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
		final ImageView pauseImage = getImageView("video-pause.png", playIconSize);
		final ImageView playImage = getImageView("video-play.png", playIconSize);
		final Button playButton = new Button();
		playButton.setGraphic(playImage);
		playButton.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event arg0) {
				if (scene != null){
					Timeline timeLine = scene.getTimeLine();
					if (timeLine.getStatus() == Status.RUNNING){
						timeLine.pause();
					} else if (timeLine.getStatus() == Status.STOPPED){
						timeLine.playFromStart();
					} else {
						timeLine.play();
					}
				}
			}
		});
		timeLineStatus = new ChangeListener<Status>() {
			
			@Override
			public void changed(ObservableValue<? extends Status> arg0, Status oldSTatus,
					Status newStatus) {
				if (newStatus == Status.RUNNING){
					playButton.setGraphic(pauseImage);
				} else {
					playButton.setGraphic(playImage);
				}
			}

		};
		final Label speedLabel = new Label("Speed: 1.0x");
		timeLineRate = new ChangeListener<Number>() {
			
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1,
					Number arg2) {
				speedLabel.setText("Speed: " + arg2.doubleValue() + "x");
			}
		};
		ImageView ffdImage = getImageView("fast-forward.png", playIconSize);
		Button ffdButton = new Button("Speed up", ffdImage);
		ImageView rwImage = getImageView("rewind.png", playIconSize);
		Button rwButton = new Button("Speed down", rwImage);
		
		/**
		 * Listener for the {@link Event} that the user clicks a fast forward or
		 * rewind button. Makes sure that the visualization is accordingly
		 * sped up or down.
		 */
		class TimeLineRateChanged implements EventHandler<Event>{

			/**
			 * If true, the rate changes to a higher value.
			 * If false, the rate changes to a lower value.
			 */
			private final boolean speedUp;
			
			/**
			 * Makes sure that the visualization is accordingly sped up or down.
			 */
			@Override
			public void handle(Event arg0) {
				MapScene scene = Visualizer.this.scene;
				if (scene != null){
					Timeline timeLine = scene.getTimeLine();
					double old = timeLine.getRate();
					double diff = speedUp ? timeLineRateStep : (-1*timeLineRateStep);
					timeLine.setRate(old + diff);
				}
			}

			/**
			 * @param speedUp If true, the rate changes to a higher value.
			 * If false, the rate changes to a lower value.
			 */
			TimeLineRateChanged(boolean speedUp) {
				this.speedUp = speedUp;
			}
		};
		ffdButton.setOnMouseClicked(new TimeLineRateChanged(true));
		rwButton.setOnMouseClicked(new TimeLineRateChanged(false));
		ImageView zoomInImage = getImageView("zoom-in.png", playIconSize);
		Button zoomInButton = new Button("Zoom IN", zoomInImage);
		zoomInButton.setOnMouseClicked(new  EventHandler<Event>() {

			@Override
			public void handle(Event arg0) {
				if (scene != null){
					scene.changeZoom(1.2);
				}
			}
		});
		ImageView zoomOutImage = getImageView("zoom-out.png", playIconSize);
		Button zoomOutButton = new Button("Zoom OUT", zoomOutImage);
		zoomOutButton.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event arg0) {
				if (scene != null){
					scene.changeZoom(1/1.2);
				}
			}
		});
		HBox hbox = new HBox(speedLabel, rwButton, playButton, ffdButton, zoomInButton, zoomOutButton); 	
		hbox.setSpacing(10);
		hbox.setAlignment(Pos.CENTER_RIGHT);
		return hbox;
	}
	
	/**
	 * The stage used by this application.
	 */
	private Stage stage;
	
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
	HBox getControlsBar() {
		return controlsBar;
	}

	/**
	 * The root element of the main {@link Scene} of the application.
	 */
	private final VBox vbox = new VBox();
	
	/**
	 * @return Builds and returns the column, shown on the left side of the map, containing various graphics options
	 * @throws IOException Unless the application source folder contents have been changed in 
	 * any way by the user, this exception will never be thrown. 
	 */
	private VBox createGraphicsColumn() throws IOException{
		//TODO break this method into smaller pieces of code
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
		ImageView snapShotImage = getImageView("screenshot.png", playIconSize);
		Button screenShotButton = new Button("Snapshot", snapShotImage);
		screenShotButton.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event arg0) {
				if (scene != null){
					Timeline timeLine = scene.getTimeLine();
					boolean paused = false;
					if (timeLine.getStatus() == Status.RUNNING){
						timeLine.pause();
						paused = true;
					}
					WritableImage image = scene.getMapContainer().snapshot(null, null);
					FileChooser fileChooser = new FileChooser();
					fileChooser.setTitle("Specify a PNG file");
					FileChooser.ExtensionFilter filter = new ExtensionFilter("Just PNG files", "png");
					fileChooser.getExtensionFilters().add(filter);
					File file = fileChooser.showSaveDialog(stage);
					if (file != null){
						BufferedImage bim = SwingFXUtils.fromFXImage(image, null);
						boolean success = false;
						try {
							success = ImageIO.write(bim, "png", file);															
						} catch (IOException ex) {}
						if (success){
							showDialog(DialogType.SUCCESS, "The snapshot has been saved to", 
									file.getAbsoluteFile().toString());
						} else {
							showDialog(DialogType.ERROR, "The snapshot could not be saved.");
						}
					}
					if (paused){
						timeLine.play();
					}
				}
			}
		});
		panel.getChildren().add(screenShotButton);		
		final ImageView recordStartImage = getImageView("record.png", playIconSize);
		final ImageView recordStopImage = getImageView("stop.png", playIconSize);
		final Button recordButton = new Button("Record", recordStartImage);
		recordButton.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event arg0) {
				if (scene != null){			
					if (scene.isRecordingInProgress()){
						scene.setRecordingInProgress(false);
						scene.flushRecordedFrames();
						recordButton.setText("Record");
						recordButton.setGraphic(recordStartImage);
					} else {
						Timeline timeLine = scene.getTimeLine();
						boolean paused = false;
						if (timeLine.getStatus() == Status.RUNNING){
							timeLine.pause();
							paused = true;
						}
						DirectoryChooser dirChooser = new DirectoryChooser();
						dirChooser.setTitle("Select a folder");
						File dir = dirChooser.showDialog(stage);
						if (dir != null){
							scene.setRecordingDirectory(dir);
							scene.setRecordingInProgress(true);
							recordButton.setText("Stop");
							recordButton.setGraphic(recordStopImage);
						}
						if (paused){
							timeLine.play();
						}
					}					
				}
			}
		});
		panel.getChildren().add(recordButton);
		for (Node node : panel.getChildren()){
			VBox.setMargin(node, new Insets(graphicsItemsMargin, 0, graphicsItemsMargin, 2 * graphicsItemsMargin));
		}
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
	 * Shows a simple dialog to the user, containing a text message.
	 * User only has to click the OK button.
	 * @param type Type of the dialog, i.e. error, success etc.
	 * @param messages Each message will we shown as a single line in the dialog
	 */
	private void showDialog(DialogType type, String... messages) {
		//TODO wrap this functionality inside an extra class
		if ((type == null) || (messages == null) || (messages.length == 0)){
			return;
		}
		cropMessages(messages);
		String imageResource = null;
		switch(type){
			case ERROR:
				imageResource = "error.png";
				break;
			case INFO:
				imageResource = "info.png";
				break;
			case SUCCESS:
				imageResource = "success.png";
				break;			
		}
		ImageView image = getImageView(imageResource, dialogIconSize);
		List<Label> labels = new ArrayList<>();
		labels.add(new Label());
		for (String msg : messages){
			Label label = new Label(msg);
			labels.add(label);
		}
		labels.add(new Label());
		Button okButton = new Button("OK");
		GridPane gridPane = new GridPane();		
		GridPane.setHalignment(okButton, HPos.CENTER);
		GridPane.setValignment(image, VPos.CENTER);			
		int row = 0;
		gridPane.add(image, 0, 0, 1, labels.size());
		for (Label lab : labels){
			GridPane.setMargin(lab, new Insets(0, 20, 0, 20));
			gridPane.add(lab, 1, row);
			row += 1;
		}
		gridPane.add(okButton, 1, row);
		Scene scene = new Scene(gridPane);
		final Stage dialog = new Stage();
		okButton.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event arg0) {
				dialog.close();
			}
		});
		dialog.initStyle(StageStyle.UTILITY);
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.setScene(scene);
		dialog.sizeToScene();
		dialog.show();
	}
	
	/**
	 * Makes sure that the given Strings contain at most {@link Visualizer#dialogMessageLineLength}
	 * characters each. If any of them is longer, it is replaced with a cropped version.
	 * @param messages These Strings are checked for their length.
	 */
	private void cropMessages(String[] messages){
		if (messages != null){
			for (int i = 0; i < messages.length; i++){
				String msg = messages[i];
				if ((msg != null) && (msg.length() > dialogMessageLineLength)){
					String newMsg = msg.substring(0, dialogMessageLineLength) + "...";
					messages[i] = newMsg;
				}
			}
		}
	}
	
	/**
	 * Maximum number of characters in a single line of the messages that are shown by
	 * the dialog produced in {@link Visualizer#showDialog(DialogType, String...)}
	 */
	private final int dialogMessageLineLength = 128;
	
	/**
	 * Type of the dialogs that are produced by {@link Visualizer#showDialog(DialogType, String)} 
	 */
	private static enum DialogType {
		INFO, SUCCESS, ERROR;
	}
	
	/**
	 * Width and height of the illustrative icon shown inside the dialog that
	 * is shown with {@link Visualizer#showDialog(DialogType, String)}
	 */
	private final double dialogIconSize = 64.0;
	
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
	 */
	@Override
	public void start(Stage stage) throws IOException {	
		this.stage = stage;
		noMapNode.setPrefSize(mapWidth, mapHeight);
		controlsBar.setDisable(true);
		Label helpLabel = new Label("To import new simulation data, click File -> Import Scene");
		helpLabel.setPadding(new Insets(10, 10, 10, 10));
		noMapNode.getChildren().add(helpLabel);	
		mapPane.getChildren().add(noMapNode);
		graphicsColumn.setPrefWidth(graphicsColumnWidth);
		graphicsColumn.setMinWidth(graphicsColumnWidth);
		middleRow.getChildren().addAll(graphicsColumn, mapPane);		
		vbox.getChildren().clear();
		vbox.getChildren().addAll(menuBar, middleRow, controlsBar);
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
	 * @throws IOException Unless the application source folder contents have been changed in 
	 * any way by the user, this exception will never be thrown.
	 */
	public Visualizer() throws IOException {				
		menuBar = createMenuBar();	
		controlsBar = createControlsBar();
		graphicsColumn = createGraphicsColumn();
		importSceneGrid = createImportSceneGrid(timeLineStatus, timeLineRate);
	}			
	
}
