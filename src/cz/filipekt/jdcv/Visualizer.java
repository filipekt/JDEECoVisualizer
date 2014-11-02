package cz.filipekt.jdcv;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javafx.application.Application;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import cz.filipekt.jdcv.network.MyLink;
import cz.filipekt.jdcv.network.MyNode;

/**
 * Run this class to show the map visualizer.
 * @author Tom
 *
 */
public class Visualizer extends Application {

	/**
	 * Maps {@link MyNode#getId()} to the {@link MyNode} object. Each {@link MyNode} represents
	 * a node in the map. 
	 */
	private final Map<String,MyNode> nodes;
	
	/**
	 * Maps {@link MyLink#getId()} to the {@link MyLink} object. Each {@link MyLink} represents
	 * a link in the map.
	 */
	private final Map<String,MyLink> links;
	
	/**
	 * Maps {@link Circle} to {@link MyNode}. Each {@link Circle} represents a view of the 
	 * corresponding {@link MyNode}.
	 */
	private final Map<Node,MyNode> circles = new HashMap<>();
	
	/**
	 * Maps {@link Line} to {@link MyLink}. Each {@link Line} represents a view of the 
	 * corresponding {@link MyLink}.
	 */
	private final Map<Node,MyLink> lines = new HashMap<>();
	
	private final double minx;
	private final double miny;
	private final double maxx;
	private final double maxy;
	private final double widthFactor;
	private final double heightFactor;
	
	/**
	 * Preferred width of the map view, in pixels.
	 */
	private final double mapWidth = 800.0;
	
	/**
	 * Preferred height of the map view, in pixels.
	 */
	private final double mapHeight = 600.0;
	
	/**
	 * Margin size (in pixels) that shall be placed on the map view borders.
	 */
	private final double margin = 20.0;
	
	/**
	 * Radius (in pixels) of the {@link Circle} objects representing map nodes.
	 */
	private final double radius = 5.0;
	
	/**
	 * Zoom factor used to view the map.
	 */
	private double zoom = 1.0;
	
	/**
	 * Determines the minimal and maximal x,y coordinates across all of the map nodes.
	 * @return Array consisting of (minimal x, minimal y, maximal x, maximal y) coordinates of map nodes.
	 */
	private double[] getMapBorders(){
		double minx = Double.MAX_VALUE;
		double miny = Double.MAX_VALUE;
		double maxx = 0.0;
		double maxy = 0.0;
		for (MyNode node : nodes.values()){
			if (node.getX() < minx){
				minx = node.getX();
			}
			if (node.getX() > maxx){
				maxx = node.getX();
			}
			if (node.getY() < miny){
				miny = node.getY();
			}
			if (node.getY() > maxy){
				maxy = node.getY();
			}
		}
		return new double[]{minx, miny, maxx, maxy};
	}
	
	/**
	 * After some changes have been made to {@link Visualizer#nodes}, {@link Visualizer#zoom} or
	 * other fields that have influence on the map view, call this method to get an updated
	 * collection of the {@link Circle} objects. These represent the {@link cz.filipekt.jdcv.network.MyNode}
	 * objects for purposes of visualization.
	 * @return Updated collection of {@link Circle} objects, representing the 
	 * {@link cz.filipekt.jdcv.network.MyNode} objects for purposes of visualization. 
	 */
	private Map<Node,MyNode> generateCircles(){
		Map<Node,MyNode> res = new HashMap<>();
		for (MyNode node : nodes.values()){
			double x = node.getX();
			x -= minx;
			x *= (widthFactor * zoom);
			x += margin;
			double y = node.getY();
			y -= miny;
			y *= (heightFactor * zoom);
			y += margin;
			Circle circle = new Circle(x, y, radius, Color.RED);
			res.put(circle, node);
		}
		return res;
	}
	
	/**
	 * After some changes have been made to {@link Visualizer#links}, {@link Visualizer#zoom} or
	 * other fields that have influence on the map view, call this method to get an updated
	 * collection of the {@link Line} objects. These represent the {@link cz.filipekt.jdcv.network.MyLink}
	 * objects for purposes of visualization.
	 * @return Updated collection of {@link Line} objects, representing the 
	 * {@link cz.filipekt.jdcv.network.MyLink} objects for purposes of visualization. 
	 */
	private Map<Node,MyLink> generateLines(){
		Map<Node,MyLink> res = new HashMap<>();
		for (MyLink link : links.values()){
			double fromx = link.getFrom().getX();
			fromx -= minx;
			fromx *= widthFactor * zoom;
			fromx += margin;
			double fromy = link.getFrom().getY();
			fromy -= miny;
			fromy *= heightFactor * zoom;
			fromy += margin;
			double tox = link.getTo().getX();
			tox -= minx;
			tox *= widthFactor * zoom;
			tox += margin;
			double toy = link.getTo().getY();
			toy -= miny;
			toy *= heightFactor * zoom;
			toy += margin;
			Line line = new Line(fromx, fromy, tox, toy);
			line.setStroke(Color.BLACK);
			line.setStrokeWidth(3);		
			res.put(line, link);
		}
		return res;
	}
	
	public static void main(String[] args){
		launch(args);
	}
	
	/**
	 * Contains the GUI representation of the map. Contains various {@link Circle} and {@link Line} instances, 
	 * which correspond to the map elements. 
	 */
	private final Group mapGroup = new Group();

	
	/**
	 * Builds the GUI, should only be called by the JavaFX runtime.
	 */
	@Override 
	public void start(Stage stage) {
		reloadFxNodes();
		ScrollPane scrollPane = new ScrollPane();
		scrollPane.setContent(mapGroup);
		scrollPane.setPrefSize(mapWidth + (2*margin), mapHeight + (2*margin));
		Button zoomInButton = new Button("Zoom IN");
		zoomInButton.setOnMouseClicked(new  EventHandler<Event>() {

			@Override
			public void handle(Event arg0) {
				changeZoom(0.2);
			}
		});
		Button zoomOutButton = new Button("Zoom OUT");
		zoomOutButton.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event arg0) {
				changeZoom(-0.2);
			}
		});
		HBox hbox = new HBox(zoomInButton, zoomOutButton); 	
		hbox.setAlignment(Pos.TOP_RIGHT);
		VBox vbox = new VBox(scrollPane, hbox);
		Scene scene = new Scene(vbox, Color.WHITE);
	    stage.setScene(scene);
	    stage.setResizable(false);
	    stage.setTitle("Map Visualizer");
	    stage.show();
	}
	
	/**
	 * Zooms in or zooms out the map view. Ensures the map view reloaded with the new zoom value.
	 * @param factor
	 */
	private void changeZoom(double factor){
		zoom += factor;
		updateCirclesAndLines();
		reloadFxNodes();
	}
	
	/**
	 * Should be called after changing the contents of {@link Visualizer#lines} and 
	 * {@link Visualizer#circles}. It makes sure the map view is updated and reloaded accordingly. 
	 */
	private void reloadFxNodes(){
		mapGroup.getChildren().clear();
		Collection<Node> fxNodesToShow = new HashSet<>();
		fxNodesToShow.addAll(lines.keySet());
		fxNodesToShow.addAll(circles.keySet());
		mapGroup.getChildren().addAll(fxNodesToShow);
	    moveCirclesToFront();	    
	}
	
	/**
	 * Updates the collections of {@link Circle} and {@link Line} objects to
	 * represent the current state of the map view.
	 */
	private void updateCirclesAndLines(){
		Map<Node,MyNode> newCircles = generateCircles();
		Map<Node,MyLink> newLines = generateLines();
		circles.clear();
		circles.putAll(newCircles);
		lines.clear();
		lines.putAll(newLines);
	}
	
	/**
	 * Ensures that the circles representing "node" elements are drawn in the 
	 * foreground with respect to the lines representing "link" elements.
	 */
	private void moveCirclesToFront(){
		for (Node node : circles.keySet()){
			((Circle)node).toFront();
		}
	}

	public Visualizer() throws ParserConfigurationException, SAXException, IOException {
		super();
		Path sourceFile = Paths.get("output_network.xml");
		XMLextractor extractor = new XMLextractor(sourceFile);
		XMLresult result = extractor.doExtraction();
		nodes = result.getNodes();
		links = result.getLinks();
		double[] borders = getMapBorders();
		this.minx = borders[0];
		this.miny = borders[1];
		this.maxx = borders[2];
		this.maxy = borders[3];
		widthFactor = mapWidth / (maxx - minx);
		heightFactor = mapHeight / (maxy - miny);
		updateCirclesAndLines();
	}			
	
}
