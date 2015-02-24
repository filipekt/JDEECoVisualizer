package cz.filipekt.jdcv.network;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a "link" XML element in the network source file
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
public class MyLink {
	
	/**
	 * A unique id of the link
	 */
	private final String id;
	
	/**
	 * Starting point of the link. It is part of the same network category as this link.
	 */
	private final MyNode from;
	
	/**
	 * End point of the link. It is part of the same network category as this link.
	 */
	private final MyNode to;
	
	/**
	 * Defines the real length of the link.  It must be (of course) at least as long 
	 * as the Euclidean distance between the from and to nodes.
	 * Contains unsigned double values, thus the {@link BigDecimal} type.
	 */
	private final BigDecimal length;
	
	/**
	 * The allowed maximum speed of the link
	 */
	private final double freespeed;
	
	/**
	 * The maximal capacity of this link for a given period 
	 */
	private final double capacity;	
	
	/**
	 * The number of lanes of this link
	 */
	private final double numberOfLanes;
	
	/**
	 * List of transportation modes that are allowed on this link
	 */
	private final Set<String> allowedModes = new HashSet<>();
	
	/**
	 * @param id A unique id of the link
	 * @param from Starting point of the link. It is part of the same network category as this link.
	 * @param to End point of the link. It is part of the same network category as this link.
	 * @param length Defines the real length of the link.  It must be (of course) at least as long 
	 * as the Euclidean distance between the from and to nodes.
	 * @param freespeed The allowed maximum speed of the link
	 * @param capacity The maximal capacity of this link for a given period 
	 * @param numberOfLanes The number of lanes of this link
	 * @param modes List of transportation modes that are allowed on this link
	 */
	public MyLink(String id, MyNode from, MyNode to, BigDecimal length,
			double freespeed, double capacity, double numberOfLanes,
			String... modes) {
		super();
		this.id = id;
		this.from = from;
		this.to = to;
		this.length = length;
		this.freespeed = freespeed;
		this.capacity = capacity;
		this.numberOfLanes = numberOfLanes;
		Collections.addAll(allowedModes, modes);
	}
	
	/**
	 * Implements the unique identification of the {@link MyLink} object 
	 * by {@link MyLink#id}.
	 * @see {@link MyLink#hashCode()}
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MyLink){
			MyLink that = (MyLink)obj;
			return this.id.equals(that.id);
		} else {
			return false;
		}
	}
	
	/**
	 * Implements the unique identification of the {@link MyLink} object 
	 * by {@link MyLink#id}.
	 * @see {@link MyLink#equals(Object)}
	 */
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	/**
	 * @return A unique id of the link
	 * @see {@link MyLink#id}
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return Starting point of the link. It is part of the same network category as this link.
	 * @see {@link MyLink#from}
	 */
	public MyNode getFrom() {
		return from;
	}

	/**
	 * @return End point of the link. It is part of the same network category as this link.
	 * @see {@link MyLink#to}
	 */
	public MyNode getTo() {
		return to;
	}

	/**
	 * @return The real length of the link.
	 * @see {@link MyLink#length}
	 */
	public BigDecimal getLength() {
		return length;
	}

	/**
	 * @return The allowed maximum speed of the link
	 * @see {@link MyLink#freespeed}
	 */
	public double getFreespeed() {
		return freespeed;
	}

	/**
	 * @return The maximal capacity of this link for a given period 
	 * @see {@link MyLink#capacity}
	 */
	public double getCapacity() {
		return capacity;
	}

	/**
	 * @return The number of lanes of this link
	 * @see {@link MyLink#numberOfLanes}
	 */
	public double getNumberOfLanes() {
		return numberOfLanes;
	}

	/**
	 * @return List of transportation modes that are allowed on this link
	 * @see {@link MyLink#allowedModes}
	 */
	public Set<String> getAllowedModes() {
		return allowedModes;
	}
}
