package cz.filipekt.jdcv.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import cz.filipekt.jdcv.events.ActStartOrEnd;
import cz.filipekt.jdcv.events.ArrivalOrDeparture;
import cz.filipekt.jdcv.events.EnteredOrLeftLink;
import cz.filipekt.jdcv.events.EntersOrLeavesVehicle;
import cz.filipekt.jdcv.events.MatsimEvent;
import cz.filipekt.jdcv.events.EventType;
import cz.filipekt.jdcv.exceptions.InvalidAttributeValueException;
import cz.filipekt.jdcv.exceptions.LinkNotFoundException;
import cz.filipekt.jdcv.network.MyLink;

/**
 * SAX handler used to parse the XML file containing the events.
 * Collects the "event" elements. 
 */
public class EventWithPersonHandler extends DefaultHandler {
	
	/**
	 * Local name of the event element
	 */
	private final String eventName = "event";
	
	/**
	 * Name of the type attribute of the event element
	 */
	private final String typeName = "type";
	
	/**
	 * The parsed event elements from the source file
	 */
	private final List<MatsimEvent> events = new ArrayList<>();
	
	/**
	 * @return The parsed event elements from the source file
	 * @see {@link EventWithPersonHandler#events}
	 */
	public List<MatsimEvent> getEvents() {
		return events;
	}

	/**
	 * Name of the time attribute of the event element
	 */
	private final String timeName = "time";
	
	/**
	 * Name of the person attribute of the event element
	 */
	private final String personName = "person";
	
	/**
	 * Name of the vehicle attribute of the event element
	 */
	private final String vehicleName = "vehicle";
	
	/**
	 * Name of the link attribute of the event element
	 */
	private final String linkName = "link";
	
	/**
	 * Name of the legMode attribute of the event element
	 */
	private final String legModeName = "legMode";
	
	/**
	 * Name of the facility attribute of the event element
	 */
	private final String facilityName = "facility";
	
	/**
	 * Name of the actType attribute of the event element
	 */
	private final String actTypeName = "actType";
	
	/**
	 * Collection of parsed link elements, as extracted from a network source file.
	 */
	private final Map<String,MyLink> links;
	
	/**
	 * If true, only the events of the people corresponding to the injected JDEECo components will be
	 * taken into account. If false, all of the events will be parsed.
	 */
	private final boolean onlyComponents;

	/**
	 * @param links Collection of parsed link elements, as extracted from a network source file.
	 * @param onlyComponents If true, only the events of the people corresponding to the injected JDEECo components will be
	 * taken into account. If false, all of the events will be parsed.
	 */
	public EventWithPersonHandler(Map<String, MyLink> links, boolean onlyComponents) {		
		this.links = links;
		this.onlyComponents = onlyComponents;
	}
	
	/**
	 * @param personID An ID of a person that appears in the MATSIM event log
	 * @return True if the person is an injected JDEECo component, false if it is
	 * a regular MATSIM person.
	 */
	private boolean isInjectedComponent(String personID){
		return personID.startsWith("V");
	}

	/**
	 * Makes sure that when an event element is encountered, correct parsing is carried out.
	 * For each event type, the parsing work is done by specialized methods.
	 */
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (qName.equals(eventName)){
			String typeVal = attributes.getValue(typeName);
			EventType type = EventType.from(typeVal);
			if (type != null){
				String timeVal = attributes.getValue(timeName);
				Utils.ensureNonNullAndNonEmpty(timeVal);
				String personVal = attributes.getValue(personName);
				Utils.ensureNonNullAndNonEmpty(personVal);
				if (!onlyComponents || isInjectedComponent(personVal)){
					switch(type){
						case PERSON_ENTERS_VEHICLE:
							processEnteredOrLVehicle(attributes, timeVal, personVal, true);						
							break;
						case PERSON_LEAVES_VEHICLE:
							processEnteredOrLVehicle(attributes, timeVal, personVal, false);
							break;
						case ENTERED_LINK:
							processEorLLink(attributes, timeVal, personVal, true);
							break;
						case LEFT_LINK:
							processEorLLink(attributes, timeVal, personVal, false);
							break;
						case ARRIVAL:
							processArrivalDeparture(attributes, timeVal, personVal, false);
							break;
						case DEPARTURE:
							processArrivalDeparture(attributes, timeVal, personVal, true);
							break;
						case ACT_START:
							processActStartEnd(attributes, timeVal, personVal, true);
							break;
						case ACT_END:
							processActStartEnd(attributes, timeVal, personVal, false);
							break;
						default:
							return;
					}
				}
			}
		}
	}
	
	/**
	 * Parses the PersonEntersVehicle and PersonLeavesVehicle events.
	 * @param attributes Attributes of the event element. Usually provided by the SAX engine.
	 * @param entered When true, the event type is PersonEntersVehicle. 
	 * When false, the event type is PersonLeavesVehicle.
	 * @throws SAXException When a mandatory attribute is missing or has an invalid value.
	 */
	private void processEnteredOrLVehicle(Attributes attributes, String timeVal, String personVal, boolean entered) throws SAXException{
		String vehicleVal = attributes.getValue(vehicleName);
		Utils.ensureNonNullAndNonEmpty(vehicleVal);
		try {
			double time = Double.parseDouble(timeVal);
			EntersOrLeavesVehicle elv = new EntersOrLeavesVehicle(entered, time, personVal, vehicleVal);
			events.add(elv);
		} catch (NumberFormatException | NullPointerException ex){
			throw new SAXException(new InvalidAttributeValueException());
		}
	}
	
	/**
	 * Parses the "entered link" and "left link" events.
	 * @param attributes Attributes of the event element. Usually provided by the SAX engine.
	 * @param entered When true, the event type is "entered link".
	 * When false, the event type is "left link".
	 * @throws SAXException When a mandatory attribute is missing or has an invalid value.
	 */
	private void processEorLLink(Attributes attributes, String timeVal, String personVal, boolean entered) throws SAXException{
		String linkVal = attributes.getValue(linkName);
		Utils.ensureNonNullAndNonEmpty(linkVal);
		String vehicleVal = attributes.getValue(vehicleName);
		try {
			double time = Double.parseDouble(timeVal);
			MyLink link = links.get(linkVal);
			if (link == null){
				throw new SAXException(new LinkNotFoundException());
			}			
			EnteredOrLeftLink ell = new EnteredOrLeftLink(entered, time, personVal, link, vehicleVal);
			events.add(ell);
		} catch (NumberFormatException ex){
			throw new SAXException(new InvalidAttributeValueException());
		}
	}
	
	/**
	 * Parses the arrival and departure events.
	 * @param attributes Attributes of the event element. Usually provided by the SAX engine.
	 * @param departure When true, the event type is departure. When false, the event type is arrival.
	 * @throws SAXException When a mandatory attribute is missing or has an invalid value.
	 */
	private void processArrivalDeparture(Attributes attributes, String timeVal, String personVal, boolean departure) throws SAXException{
		String linkVal = attributes.getValue(linkName);
		Utils.ensureNonNullAndNonEmpty(linkVal);
		String legModeVal = attributes.getValue(legModeName);
		Utils.ensureNonNullAndNonEmpty(legModeVal);
		try {
			double time = Double.parseDouble(timeVal);
			MyLink link = links.get(linkVal);
			if (link == null){
				throw new SAXException(new LinkNotFoundException());
			}
			ArrivalOrDeparture aod = new ArrivalOrDeparture(departure, time, personVal, link, legModeVal);
			events.add(aod);
		} catch (NumberFormatException ex){
			throw new SAXException(new InvalidAttributeValueException());
		}
	}
	
	/**
	 * Parses the actstart and actend events.
	 * @param attributes Attributes of the event element. Usually provided by the SAX engine.
	 * @param start When true, the event type is actstart. When false, the event type is actend.
	 * @throws SAXException When a mandatory attribute is missing or has an invalid value.
	 */
	private void processActStartEnd(Attributes attributes, String timeVal, String personVal, boolean start) throws SAXException{
		String linkVal = attributes.getValue(linkName);
		Utils.ensureNonNullAndNonEmpty(linkVal);
		String facilityVal = attributes.getValue(facilityName);
		String actTypeVal = attributes.getValue(actTypeName);
		try {
			double time = Double.parseDouble(timeVal);
			MyLink link = links.get(linkVal);
			if (link == null){
				throw new SAXException(new LinkNotFoundException());
			}
			ActStartOrEnd ase = new ActStartOrEnd(start, time, personVal, link, facilityVal, actTypeVal);
			events.add(ase);
		} catch (NumberFormatException ex){
			throw new SAXException(new InvalidAttributeValueException());
		}
	}
	
}
