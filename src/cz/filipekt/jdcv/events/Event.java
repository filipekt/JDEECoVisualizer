package cz.filipekt.jdcv.events;


/**
 * Represents an event element from the simulation output.
 */
public interface Event {
	
	/**
	 * @return The type of the event.
	 * @see {@link EventType}
	 */
	EventType getType();
	
	/**
	 * @return The time at which the event occurred.
	 */
	double getTime();
	
	/**
	 * @return ID of the person involved in the event
	 */
	String getPerson();
}
