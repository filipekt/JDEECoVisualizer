package cz.filipekt.jdcv.gui_logic;

import cz.filipekt.jdcv.MapScene;
import cz.filipekt.jdcv.Visualizer;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;

/**
 * Listener for the {@link Event} that user clicks the "close this scene" option in the menu.
 * Makes sure that the current visualization is stopped and the map scene is closed.
 */
public class CloseSceneHandler implements EventHandler<ActionEvent>{
	
	/**
	 * The item in the "file" menu allowing for importing a new scene.
	 */
	private final MenuItem importSceneItem;
	
	/**
	 * The item in the "file" menu allowing for closing the current scene.
	 * This is the item to which this handler is assigned.
	 */
	private final MenuItem closeThisSceneItem;
	
	/**
	 * Context in which this handler is called
	 */
	private final Visualizer visualizer;

	/**
	 * @param importSceneItem The item in the "file" menu allowing for importing a new scene.
	 * @param closeThisSceneItem The item in the "file" menu allowing for closing the current scene.
	 * @param visualizer Context in which this handler is called
	 */
	public CloseSceneHandler(MenuItem importSceneItem, MenuItem closeThisSceneItem, Visualizer visualizer) {
		this.importSceneItem = importSceneItem;
		this.closeThisSceneItem = closeThisSceneItem;
		this.visualizer = visualizer;
	}

	/**
	 * Called whenever the user clicks the "close this scene" option in the menu.
	 * Makes sure that the current visualization is stopped and the map scene is closed.
	 */
	@Override
	public void handle(ActionEvent arg0) {
		Pane mapPane = visualizer.getMapPane();
		MapScene scene = visualizer.getScene();
		mapPane.getChildren().clear();
		mapPane.getChildren().add(visualizer.getNoMapNode());
		if (scene != null){
			scene.getTimeLine().stop();
			visualizer.setScene(null);
		}
		closeThisSceneItem.setDisable(true);
		importSceneItem.setDisable(false);	
		visualizer.getControlsBar().setDisable(true);
		visualizer.getGraphicsColumn().setDisable(true);
		visualizer.setGraphicsColumnDefaults();
	}
	
}