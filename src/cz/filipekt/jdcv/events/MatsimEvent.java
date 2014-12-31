package cz.filipekt.jdcv.events;


/**
 * Represents an event element from the MATSIM output.
 */
public interface MatsimEvent extends Event{
	
	/**
	 * @return ID of the person involved in the event
	 */
	String getPerson();
}
