package cz.filipekt.jdcv.plugins;

import java.io.InputStream;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import cz.filipekt.jdcv.util.Resources;

/**
 * The side panel allowing the user to filter out some of the visualization 
 * elements. Only simple filters are supported - for more elaborate ones, 
 * use the scripting console. 
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
public class FilterPanel extends PanelWithPreferences {
	
	/**
	 * Short name of this plugin
	 */
	private final String pluginName = "Filter";

	/**
	 * @return Short name of this plugin
	 */
	@Override
	public String getName() {
		return pluginName;
	}

	/**
	 * @return Input stream opened on a file containing a small image representing this plugin
	 */
	@Override
	public InputStream getThumbnail() {
		return Resources.getResourceInputStream("filter.png");
	}

	/**
	 * @return The main panel of the plugin
	 * @see {@link FilterPanel#panel}
	 */
	@Override
	public Node getPanel() {
		return panel;
	}
	
	/**
	 * The main panel of the plugin
	 */
	private final Node panel;

	/**
	 * This is a singleton class, so uncontrolled instantiation is forbidden.
	 * Initializes the main panel.
	 */
	private FilterPanel() {
		panel = createPanel();
	}
	
	/**
	 * The singleton instance of this class
	 */
	private static final FilterPanel INSTANCE = new FilterPanel();
	
	/**
	 * @return The singleton instance of this class
	 * @see {@link FilterPanel#INSTANCE}
	 */
	public static FilterPanel getInstance(){
		return INSTANCE;
	}
	
	/**
	 * Constructs the main panel of the plugin, later held in {@link FilterPanel#panel}
	 * @return The main panel of the plugin
	 */
	private Node createPanel(){
		VBox vGrid = new VBox();
		Button addButton = new Button("Add Filter");
		Button removeButton = new Button("Remove Selected");
		HBox buttons = new HBox(addButton, removeButton);
		ListView<Filter> filters = new ListView<Filter>();
		VBox.setVgrow(buttons, Priority.NEVER);
		VBox.setVgrow(filters, Priority.ALWAYS);
		filters.getItems().add(new Filter() {
			
			@Override
			public void unapply() {
			}
			
			@Override
			public void apply() {
			}
			
			@Override
			public String toString(){
				return "Not Implemented Yet";
			}
		});
		filters.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		vGrid.getChildren().addAll(buttons, filters);
		vGrid.setDisable(true);
		return vGrid;
	}

}
