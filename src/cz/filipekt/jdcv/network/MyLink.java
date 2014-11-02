package cz.filipekt.jdcv.network;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a link in the map.
 * @author Tom
 *
 */
public class MyLink {
	
	private final String id;
	private final MyNode from;
	private final MyNode to;
	private final BigDecimal length;	// represents unsigned double values	
	private final double freespeed;
	private final double capacity;	
	private final double numberOfLanes;
	private final Set<String> allowedModes = new HashSet<>();
	
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
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MyLink){
			MyLink that = (MyLink)obj;
			return this.id.equals(that.id);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	public String getId() {
		return id;
	}

	public MyNode getFrom() {
		return from;
	}

	public MyNode getTo() {
		return to;
	}

	public BigDecimal getLength() {
		return length;
	}

	public double getFreespeed() {
		return freespeed;
	}

	public double getCapacity() {
		return capacity;
	}

	public double getNumberOfLanes() {
		return numberOfLanes;
	}

	public Set<String> getAllowedModes() {
		return allowedModes;
	}
	
	
	
	
}
