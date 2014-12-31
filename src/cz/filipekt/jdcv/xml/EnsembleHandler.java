package cz.filipekt.jdcv.xml;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import cz.filipekt.jdcv.events.EnsembleEvent;
import cz.filipekt.jdcv.exceptions.InvalidAttributeValueException;

/**
 * SAX handler used to parse the XML file containing the ensemble events.
 * Collects the "event" elements. 
 */
public class EnsembleHandler extends DefaultHandler {

	/**
	 * Local name of the event element
	 */
	private final String eventName = "event";
	
	/**
	 * Name of the coordinator attribute of the event element
	 */
	private final String coordinatorName = "coordinator";
	
	/**
	 * Name of the member attribute of the event element
	 */
	private final String memberName = "member";
	
	/**
	 * Name of the membership attribute of the event element
	 */
	private final String membershipName = "membership";
	
	/**
	 * Name of the ensemble attribute of the event element
	 */
	private final String ensembleName = "ensemble";
	
	/**
	 * Name of the time attribute of the event element
	 */
	private final String timeName = "time";
	
	/**
	 * Storage for the parsed event elements 
	 */
	private final List<EnsembleEvent> events = new ArrayList<>();
	
	/**
	 * @return The parsed event elements
	 * @see {@link EnsembleHandler#events} 
	 */
	public List<EnsembleEvent> getEvents() {
		return events;
	}

	/**
	 * Makes sure that when an ensemble event element is encountered, it contains all the 
	 * required attributes with values in correct format. Afterwards, event element is stored
	 * in the parsed form in the {@link EnsembleHandler#events} storage.
	 */
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (qName.equals(eventName)){
			String coordinatorVal = attributes.getValue(coordinatorName);
			Utils.ensureNonNullAndNonEmpty(coordinatorVal);
			String memberVal = attributes.getValue(memberName);
			Utils.ensureNonNullAndNonEmpty(memberVal);
			String membershipVal = attributes.getValue(membershipName);
			Utils.ensureNonNullAndNonEmpty(membershipVal);
			String ensembleVal = attributes.getValue(ensembleName);
			Utils.ensureNonNullAndNonEmpty(ensembleVal);
			String timeVal = attributes.getValue(timeName);
			Utils.ensureNonNullAndNonEmpty(timeVal);
			boolean membership;
			switch(membershipVal){
				case "true":
					membership = true;
					break;
				case "false":
					membership = false;
					break;
				default:
					throw new SAXException(new InvalidAttributeValueException());
			}
			double time;
			try {
				time = Double.parseDouble(timeVal);
			} catch (NumberFormatException ex){
				throw new SAXException(new InvalidAttributeValueException());
			}
			EnsembleEvent eev = new EnsembleEvent(coordinatorVal, memberVal, membership, ensembleVal, time);
			events.add(eev);
		}
	}
	
}
