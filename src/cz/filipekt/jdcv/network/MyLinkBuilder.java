package cz.filipekt.jdcv.network;

import java.math.BigDecimal;
import java.util.List;

import javafx.geometry.Point2D;

/**
 * Builder for {@link MyLink}.
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
public class MyLinkBuilder {
	
	/**
	 * A unique id of the link
	 */
	private String id;
	
	/**
	 * Starting point of the link. It is part of the same network category as this link.
	 */
	private MyNode from;
	
	/**
	 * End point of the link. It is part of the same network category as this link.
	 */
	private MyNode to;
	
	/**
	 * Defines the real length of the link.  It must be (of course) at least as long 
	 * as the Euclidean distance between the from and to nodes.
	 * Contains unsigned double values, thus the {@link BigDecimal} type.
	 */
	private BigDecimal length;
	
	/**
	 * The allowed maximum speed of the link
	 */
	private double freespeed;
	
	/**
	 * The maximal capacity of this link for a given period
	 */
	private double capacity;
	
	/**
	 * The number of lanes of this link
	 */
	private double numberOfLanes;
	
	/**
	 * List of transportation modes that are allowed on this link
	 */
	private String[] allowedModes;
	
	/**
	 * Specification of the image which represents this link
	 */
	private MyLinkImg linkImage;
	
	/**
	 * Points specifying the path along which the cars/persons move through the 
	 * link visualization. Coordinates are taken from the image raster.
	 */
	private List<Point2D> pathPoints;
	
	/**
	 * Setter for {@link MyLinkBuilder#id}
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Setter for {@link MyLinkBuilder#from}
	 */
	public void setFrom(MyNode from) {
		this.from = from;
	}
	
	/**
	 * Setter for {@link MyLinkBuilder#to}
	 */
	public void setTo(MyNode to) {
		this.to = to;
	}
	
	/**
	 * Setter for {@link MyLinkBuilder#length}
	 */
	public void setLength(BigDecimal length) {
		this.length = length;
	}
	
	/**
	 * Setter for {@link MyLinkBuilder#freespeed}
	 */
	public void setFreespeed(double freespeed) {
		this.freespeed = freespeed;
	}
	
	/**
	 * Setter for {@link MyLinkBuilder#capacity}
	 */
	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}
	
	/**
	 * Setter for {@link MyLinkBuilder#numberOfLanes}
	 */
	public void setNumberOfLanes(double numberOfLanes) {
		this.numberOfLanes = numberOfLanes;
	}
	
	/**
	 * Setter for {@link MyLinkBuilder#allowedModes}
	 */
	public void setAllowedModes(String[] allowedModes) {
		this.allowedModes = allowedModes;
	}
	
	/**
	 * Setter for {@link MyLinkBuilder#linkImage}
	 */
	public void setLinkImage(MyLinkImg linkImage) {
		this.linkImage = linkImage;
	}
	
	/**
	 * Setter for {@link MyLinkBuilder#pathPoints}
	 */
	public void setPathPoints(List<Point2D> pathPoints) {
		this.pathPoints = pathPoints;
	}
	
	/**
	 * @return A new instance of {@link MyLink} using the previously collected 
	 * initialization parameters.
	 */
	public MyLink build(){
		return new MyLink(id, from, to, length, freespeed, capacity, numberOfLanes, 
				linkImage, pathPoints, allowedModes);
	}
}
