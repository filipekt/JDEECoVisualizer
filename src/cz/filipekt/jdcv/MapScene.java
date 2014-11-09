package cz.filipekt.jdcv;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import cz.filipekt.jdcv.network.MyLink;
import cz.filipekt.jdcv.network.MyNode;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

/**
 * The scene that the {@link Visualizer} will visualize. It contains the map, view parameters, event log etc.
 * @author Tomas Filipek
 *
 */
class MapScene {
	
	public MapScene(XMLresult parsedXML, double mapWidth, double mapHeight) {
		this.nodes = parsedXML.getNodes();
		this.links = parsedXML.getLinks();
		this.mapWidth = mapWidth;
		this.mapHeight = mapHeight;
		double[] borders = getMapBorders();
		this.minx = borders[0];
		this.miny = borders[1];
		this.maxx = borders[2];
		this.maxy = borders[3];
		widthFactor = mapWidth / (maxx - minx);
		heightFactor = mapHeight / (maxy - miny);
		updateCirclesAndLines();
		reloadFxNodes();
	}

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
	private final double mapWidth;
	
	/**
	 * Preferred height of the map view, in pixels.
	 */
	private final double mapHeight;
	
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
	 * Contains the GUI representation of the map. Contains various {@link Circle} and {@link Line} instances, 
	 * which correspond to the map elements. 
	 */
	private final Group mapGroup = new Group();
	
	private final ScrollPane mapPane = new ScrollPane(mapGroup);

	public ScrollPane getMapPane() {
		return mapPane;
	}

	/**
	 * Computes the total preferred width of the map, including margins.
	 * @return
	 */
	double getTotalWidth(){
		return mapWidth + (2 * margin);
	}
	
	/**
	 * Computes the total preferred heght of the map, including margins.
	 * @return
	 */
	double getTotalHeight(){
		return mapHeight + (2 * margin);
	}
	
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
	 * Should be called after changing the contents of {@link Visualizer#lines} and 
	 * {@link Visualizer#circles}. It makes sure the map view is updated and reloaded accordingly. 
	 * 
	 * TODO: is always called in pair after {@link Visualizer#updateCirclesAndLines()}, 
	 * so these should be joined into a single method.
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
	 * Ensures that the circles representing "node" elements are drawn in the 
	 * foreground with respect to the lines representing "link" elements.
	 */
	private void moveCirclesToFront(){
		for (Node node : circles.keySet()){
			((Circle)node).toFront();
		}
	}
	
	/**
	 * Zooms in or zooms out the map view. Ensures the map view reloaded with the new zoom value.
	 * @param factor
	 */
	void changeZoom(double factor){
		zoom += factor;
		updateCirclesAndLines();
		reloadFxNodes();
	}

}
