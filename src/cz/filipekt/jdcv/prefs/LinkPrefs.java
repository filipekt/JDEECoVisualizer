package cz.filipekt.jdcv.prefs;

import java.io.IOException;
import java.io.Writer;

import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;

public class LinkPrefs {
	
	/**
	 * Identification of the link
	 */
	private final String id;
	
	/**
	 * @return Identification of the link
	 * @see {@link LinkPrefs#id}
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * ID of the node where this link starts
	 */
	private final String fromNode;
	
	/**
	 * @return ID of the node where this link starts
	 * @see {@link LinkPrefs#fromNode}
	 */
	public String getFromNode() {
		return fromNode;
	}
	
	/**
	 * ID of the node where this link ends
	 */
	private final String toNode;
	
	/**
	 * @return ID of the node where this link ends
	 * @see {@link LinkPrefs#toNode}
	 */
	public String getToNode() {
		return toNode;
	}
	
	/**
	 * The geometric shape that represents the link in the visualization
	 */
	private final Line line;
	
	/**
	 * Used for logging of the carried out operations
	 */
	private final Writer writer;
	
	/**
	 * @param id Identification of the link
	 * @param fromNode ID of the node where this link starts
	 * @param toNode ID of the node where this link ends
	 * @param line The geometric shape that represents the link in the visualization
	 * @param writer Used for logging of the carried out operations 
	 */
	public LinkPrefs(String id, String fromNode, String toNode, Line line, Writer writer) {
		this.id = id;
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.line = line;
		this.writer = writer;
	}
	
	/**
	 * @return The current color of the link visualization
	 */
	public Paint getColor(){
		return line.getStroke();
	}
	
	/**
	 * Sets the color of the link visualization
	 * @param paint The new color
	 */
	public void setColor(Paint paint){
		line.setStroke(paint);
		try {
			writer.append("Color of the link " + id + " set to " + paint);
		} catch (IOException e) {}
	}
	
	/**
	 * @return The current width of the link visualization
	 */
	public double getWidth(){
		return line.getStrokeWidth();
	}
	
	/**
	 * Sets the width of the link visualization
	 * @param val The new width
	 */
	public void setWidth(double val){
		line.setStrokeWidth(val);
		try {
			writer.append("Width of the link " + id + " set to " + val);
		} catch (IOException e) {}
	}
}