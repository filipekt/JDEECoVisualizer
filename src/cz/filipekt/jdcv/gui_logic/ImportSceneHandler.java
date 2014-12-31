package cz.filipekt.jdcv.gui_logic;

import cz.filipekt.jdcv.Visualizer;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

/**
 * Listener for the {@link Event} that user clicks the "import new scene" option in the menu.
 * Makes sure that the menu specifying importing options is shown.
 */
public class ImportSceneHandler implements EventHandler<ActionEvent> {
	
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
	public ImportSceneHandler(MenuItem importSceneItem, MenuItem closeThisSceneItem, Visualizer visualizer) {
		this.importSceneItem = importSceneItem;
		this.closeThisSceneItem = closeThisSceneItem;
		this.visualizer = visualizer;
	}

	/**
	 * Called whenever the user clicks the "import new scene" option in the menu.
	 * Makes sure that the menu specifying importing options is shown.
	 */
	@Override
	public void handle(ActionEvent arg0) {
		Pane mapPane = visualizer.getMapPane();
		GridPane importSceneGrid = visualizer.getImportSceneGrid();
		mapPane.getChildren().clear();
		mapPane.getChildren().add(importSceneGrid);				
		visualizer.setScene(null);
        closeThisSceneItem.setDisable(false);
        importSceneItem.setDisable(true);
	}
	
}