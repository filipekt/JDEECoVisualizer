package cz.filipekt.jdcv.xml;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import cz.filipekt.jdcv.exceptions.InvalidAttributeValueException;
import cz.filipekt.jdcv.exceptions.LinkNotFoundException;
import cz.filipekt.jdcv.network.MyAct;
import cz.filipekt.jdcv.network.MyLeg;
import cz.filipekt.jdcv.network.MyLink;
import cz.filipekt.jdcv.network.MyPerson;
import cz.filipekt.jdcv.network.MyPlan;

/**
 * SAX handler used to parse the XML file containing the plans(population) description.
 * Collects the "person" elements. 
 * Can only be used after the "link" and "facility" elements have been collected.
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
public class PopulationHandler extends DefaultHandler {
	
	/**
	 * Local name of the act element
	 */
	private final String actName = "act";
	
	/**
	 * Name of the type attribute of the act element
	 */
	private final String actTypeName = "type";
	
	/**
	 * Name of the x attribute of the act element
	 */
	private final String actXName = "x";
	
	/**
	 * Name of the y attribute of the act element
	 */
	private final String actYName = "y";
	
	/**
	 * Name of the link attribute of the act element
	 */
	private final String actLinkName = "link";
	
	/**
	 * Name of the facility attribute of the act element
	 */
	private final String actFacilityName = "facility";
	
	/**
	 * Name of the startTime attribute of the act element
	 */
	private final String actStartTimeName = "start_time";
	
	/**
	 * Name of the endTime attribute of the act element
	 */
	private final String actEndTimeName = "end_time";
	
	/**
	 * Name of the maxDur attribute of the act element
	 */
	private final String actMaxDurName = "max_dur";
	
	/**
	 * Local name of the route element
	 */
	private final String routeName = "route";
	
	/**
	 * Name of the type attribute of the route element
	 */
	private final String routeTypeName = "type";
	
	/**
	 * The value of the type attribute of the route element marking that the CDATA section 
	 * of the route element contains a whitespace separated list of link IDs.
	 */
	private final String routeTypeLinks = "links";
	
	/**
	 * Local name of the leg element
	 */
	private final String legName = "leg";
	
	/**
	 * Name of the mode attribute of the leg element
	 */
	private final String legModeName = "mode";
	
	/**
	 * The value of the mode attribute of the last encountered leg element
	 */
	private String legModeVal;
	
	/**
	 * Name of the depTime attribute of the leg element
	 */
	private final String legDepTimeName = "dep_time";
	
	/**
	 * The value of the depTime attribute of the last encountered leg element
	 */
	private Date legDepTimeVal;
	
	/**
	 * Name of the travTime attribute of the leg element
	 */
	private final String legTravTimeName = "trav_time";
	
	/**
	 * The value of the travTime attribute of the last encountered leg element
	 */
	private Date legTravTimeVal;
	
	/**
	 * Name of the arrTime attribute of the leg element
	 */
	private final String legArrTimeName = "arr_time";
	
	/**
	 * The value of the arrTime attribute of the last encountered leg element
	 */
	private Date legArrTimeVal;
	
	/**
	 * Local name of the plan element 
	 */
	private final String planName = "plan";
	
	/**
	 * Name of the "selected" attribute of the plan element 
	 */
	private final String planSelectedName = "selected";
	
	/**
	 * The value of the "selected" attribute of the plan element that means "yes"
	 */
	private final String planSelectedTrue = "yes";
	
	/**
	 * The value of the "selected" attribute of the plan element that means "no"
	 */
	private final String planSelectedFalse = "no";
	
	/**
	 * The value of the "selected" attribute of the last encountered plan element
	 */
	private boolean planSelectedVal;
	
	/**
	 * Local name of the person element
	 */
	private final String personName = "person";
	
	/**
	 * Name of the ID attribute of the the person element
	 */
	private final String personIdName = "id";
	
	/**
	 * The value of the ID attribute of the last encountered person element
	 */
	private String personIdVal;
	
	/**
	 * Name of the sex attribute of the person element
	 */
	private final String personSexName = "sex";
	
	/**
	 * The value of the sex attribute of the person element that means "male"
	 */
	private final String personSexMale = "m";
	
	/**
	 * The value of the sex attribute of the person element that means "female"
	 */
	private final String personSexFemale = "f";
	
	/**
	 * The value of the sex attribute of the last encountered person element.
	 * Null value means the attribute was not present.
	 */
	private Boolean personSexVal;
	
	/**
	 * Name of the age attribute of the person element
	 */
	private final String personAgeName = "age";
	
	/**
	 * The value of the age attribute of the last encountered person element.
	 * Null value means the attribute was not present.
	 */
	private Integer personAgeVal;
	
	/**
	 * Name of the license attribute of the person element
	 */
	private final String personLicenseName = "license";
	
	/**
	 * The value of the license attribute of the person element that means "yes"
	 */
	private final String personLicenseYes = "yes";
	
	/**
	 * The value of the license attribute of the person element that means "no"
	 */
	private final String personLicenseNo = "no";
	
	/**
	 * The value of the license attribute of the last encountered person element.
	 * Null value means the attribute was not present.
	 */
	private Boolean personLicenseVal;
	
	/**
	 * Name of the carAvail attribute of the person element
	 */
	private final String personCarAvailName = "car_avail";
	
	/**
	 * The value of the carAvail attribute of the person element that means "always"
	 */
	private final String personCarAvailAlways = "always";
	
	/**
	 * The value of the carAvail attribute of the person element that means "never"
	 */
	private final String personCarAvailNever = "never";
	
	/**
	 * The value of the carAvail attribute of the person element that means "sometimes"
	 */
	private final String personCarAvailSometimes = "sometimes";
	
	/**
	 * The value of the carAvail attribute of the last encountered person element.
	 * Null value means the attribute was not present.
	 */
	private String personCarAvailVal;
	
	/**
	 * Name of the employed attribute of the person element
	 */
	private final String personEmployedName = "employed";
	
	/**
	 * The value of the employed attribute of the person element that means "yes"
	 */
	private final String personEmployedYes = "yes";
	
	/**
	 * The value of the employed attribute of the person element that means "no"
	 */
	private final String personEmployedNo = "no";
	
	/**
	 * The value of the employed attribute of the last encountered person element
	 * Null value means the attribute was not present.
	 */
	private Boolean personEmployedVal;
	
	/**
	 * {@link MyAct} representations of the act elements inside the current plan element.
	 * Continuously updated as the parser moves through the plan element.
	 * When a new plan element is entered, a new list is assigned.
	 */
	private List<MyAct> currentActivities = new ArrayList<>();
	
	/**
	 * {@link MyLeg} representations of the leg elements inside the current plan element.
	 * Continuously updated as the parser moves through the plan element.
	 * When a new plan element is entered, a new list is assigned.
	 */
	private List<MyLeg> currentLegs = new ArrayList<>();
	
	/**
	 * {@link MyPlan} representations of the plan elements inside the current person element.
	 * Continuously updated as the parser moves through the person element.
	 * When a new person element is entered, a new list is assigned.
	 */
	private List<MyPlan> currentPlans = new ArrayList<>();
	
	/**
	 * {@link MyPerson} representations of the collected person elements.
	 * Continuously updated as the parser moves through the input file.
	 */
	private final Map<String,MyPerson> persons = new HashMap<>();
	
	/**
	 * @return {@link MyPerson} representations of the collected person elements. 
	 * Keys are person IDs, values the {@link MyPerson} representation of the person elements.
	 * @see {@link PopulationHandler#persons}
	 */
	public Map<String, MyPerson> getPersons() {
		return persons;
	}

	/**
	 * The links definitions, previously parsed from a separate XML file.
	 */
	private final Map<String,MyLink> links;
	
	/**
	 * @param links The links definitions, previously parsed from a separate XML file.
	 */
	public PopulationHandler(Map<String, MyLink> links) {
		this.links = links;
	}

	/**
	 * A buffer used to read the CDATA of route elements.
	 */
	private final StringBuilder routeChars = new StringBuilder();
	
	/**
	 * If set, the SAX parser is currently inside a route element.
	 * This is used in the {@link PopulationHandler#characters(char[], int, int)} method
	 * to determine whether to collect CDATA.
	 */
	private boolean insideRoute = false;
	
	/**
	 * The route element contains some link IDs in its CDATA section. After a route
	 * element is processed, these link IDs are placed here to be collected later.
	 */
	private String[] routeLinks;
	
	/**
	 * Format of the time and duration element values
	 * in the source XML file. It is set to "hh:mm:ss".
	 * @see http://www.matsim.org/files/dtd/population_v5.dtd
	 */
	private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	
	/**
	 * Marks that the currently parsed plan element won't be saved, 
	 * as it contains a route in an unrecognized format
	 */
	private boolean discardThisPlan;

	/**
	 * Called by the SAX engine when an element is being entered.
	 * This method distributes the handling actions to the respective methods
	 * that contain the proper actions.
	 */
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		switch (qName){
			case actName:
				processActivity(attributes);
				break;
			case routeName:
				processRoute(attributes);
				break;
			case legName:
				processLeg(attributes);
				break;
			case planName:
				processPlan(attributes);
				break;
			case personName:
				processPerson(attributes);
				break;
		}
	}

	/**
	 * Called by the SAX engine when an element is being left.
	 * This method distributes the handling actions to the respective methods
	 * that contain the proper actions.
	 */
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		switch(qName){
			case routeName:
				processRouteEnd();
				break;
			case legName:
				processLegEnd();
				break;
			case planName:
				processPlanEnd();
				break;
			case personName:
				processPersonEnd();
				break;
		}
	}
	
	/**
	 * Called by the SAX engine whenever some character data is encountered inside
	 * an element. If the character data is inside the route element, it is
	 * stored in {@link PopulationHandler#routeChars}.
	 */
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (insideRoute){
			routeChars.append(ch, start, length);
		}
	}
	
	/**
	 * Defines actions to be taken when leaving a route element.
	 * Makes sure that the {@link PopulationHandler#characters(char[], int, int)} method
	 * does not continue to collect characters, as the route definition is over.
	 */
	private void processRouteEnd(){
		insideRoute = false;
		routeLinks = routeChars.toString().split("\\s+");
	}
	
	/**
	 * Defines actions to be taken when leaving a person element.
	 * Builds a {@link MyPerson} representation of the person element.
	 */
	private void processPersonEnd(){
		MyPerson person = new MyPerson(personIdVal, personSexVal, personAgeVal, personLicenseVal, personCarAvailVal, personEmployedVal, currentPlans);
		persons.put(person.getId(), person);
		currentPlans = new ArrayList<>();
	}
	
	/**
	 * Defines actions to be taken when leaving a plan element.
	 * Builds a {@link MyPlan} representation of the plan element.
	 */
	private void processPlanEnd(){
		MyPlan plan = new MyPlan(planSelectedVal, currentActivities, currentLegs);
		if (!discardThisPlan){
			currentPlans.add(plan);
		}
		currentActivities = new ArrayList<>();
		currentLegs = new ArrayList<>();
	}
	
	/**
	 * Defines actions to be taken when leaving a leg element.
	 * It goes through all the routes that were defined inside the plan and
	 * finds the {@link MyLink} representations of the links that the route consists of.
	 * Builds a {@link MyLeg} representation of the leg element.
	 * @throws SAXException When a route defined inside this plan refers to a non existent link.
	 */
	private void processLegEnd() throws SAXException{
		if (discardThisPlan) return;
		List<MyLink> parsedLinks = new ArrayList<>();
		for (int i = 0; i < routeLinks.length; i++){
			MyLink newLink = links.get(routeLinks[i]);
			if (newLink == null){
				throw new SAXException(new LinkNotFoundException());
			} else {
				parsedLinks.add(newLink);
			}
		}
		MyLeg leg = new MyLeg(legModeVal, legDepTimeVal, legTravTimeVal, legArrTimeVal, parsedLinks);
		currentLegs.add(leg);
	}
	
	/**
	 * Defines actions to be taken when entering an act element.
	 * Collects the values of the mandatory and optional attributes and saves a newly created
	 * {@link MyAct} representation of the act element into {@link PopulationHandler#currentActivities}.
	 * @param attributes Attributes of the act element. Usually provided by the SAX engine.
	 * @throws SAXException When a mandatory attribute of the plan element is missing or empty, 
	 * or an optional attribute has an invalid value.
	 */
	private void processActivity(Attributes attributes) throws SAXException {
		String typeVal = attributes.getValue(actTypeName);
		Utils.ensureNonNullAndNonEmpty(typeVal);
		String xVal = attributes.getValue(actXName);
		double x = -1;
		String yVal = attributes.getValue(actYName);
		double y = -1;
		String linkVal = attributes.getValue(actLinkName);
		MyLink link = null;
		if (Utils.checkNonNullAndNonEmpty(xVal) && Utils.checkNonNullAndNonEmpty(yVal)){
			try {
				x = Double.parseDouble(xVal);
				y = Double.parseDouble(yVal);
			} catch (NumberFormatException ex){
				throw new SAXException(new InvalidAttributeValueException());
			}
		} else {
			Utils.ensureNonNullAndNonEmpty(linkVal);
			link = links.get(linkVal);
			if (link == null){
				throw new SAXException(new LinkNotFoundException());
			}
		}
		String facilityVal = attributes.getValue(actFacilityName);
		String startTimeVal = attributes.getValue(actStartTimeName);
		Date startTime = null;
		if (Utils.checkNonNullAndNonEmpty(startTimeVal)){
			try {
				startTime = dateFormat.parse(startTimeVal);
			} catch (ParseException e) {
				throw new SAXException(new InvalidAttributeValueException());
			}
		}
		String endTimeVal = attributes.getValue(actEndTimeName);
		Date endTime = null;
		if (Utils.checkNonNullAndNonEmpty(endTimeVal)){
			try {
				endTime = dateFormat.parse(endTimeVal);
			} catch (ParseException e) {
				throw new SAXException(new InvalidAttributeValueException());
			}
		}
		String maxDurVal = attributes.getValue(actMaxDurName);
		Date maxDur = null;
		if (Utils.checkNonNullAndNonEmpty(maxDurVal)){
			try {
				maxDur = dateFormat.parse(maxDurVal);
			} catch (ParseException e) {
				throw new SAXException(new InvalidAttributeValueException());
			}
		}
		MyAct act = new MyAct(typeVal, x, y, link, facilityVal, startTime, endTime, maxDur);
		currentActivities.add(act);
	}
	
	/**
	 * Defines actions to be taken when entering a leg element.
	 * Collects and saves the values of the mandatory and optional attributes.
	 * @param attributes Attributes of the leg element. Usually provided by the SAX engine.
	 * @throws SAXException When a mandatory attribute of the plan element is missing or empty, 
	 * or an optional attribute has an invalid value.
	 */
	private void processLeg(Attributes attributes) throws SAXException {
		legModeVal = attributes.getValue(legModeName);
		Utils.ensureNonNullAndNonEmpty(legModeVal);
		String depTimeVal = attributes.getValue(legDepTimeName);
		legDepTimeVal = null;
		if (Utils.checkNonNullAndNonEmpty(depTimeVal)){
			try {
				legDepTimeVal = dateFormat.parse(depTimeVal);
			} catch (ParseException e) {
				throw new SAXException(new InvalidAttributeValueException());
			}
		}
		String travTimeVal = attributes.getValue(legTravTimeName);
		legTravTimeVal = null;
		if (Utils.checkNonNullAndNonEmpty(travTimeVal)){
			try {
				legTravTimeVal = dateFormat.parse(travTimeVal);
			} catch (ParseException e) {
				throw new SAXException(new InvalidAttributeValueException());
			}
		}
		String arrTimeVal = attributes.getValue(legArrTimeName);
		legArrTimeVal = null;
		if (Utils.checkNonNullAndNonEmpty(arrTimeVal)){
			try {
				legArrTimeVal = dateFormat.parse(arrTimeVal);
			} catch (ParseException e) {
				throw new SAXException(new InvalidAttributeValueException());
			}
		}
	}
	
	/**
	 * Defines actions to be taken when entering a plan element.
	 * Collects and saves the value of the mandatory attribute "selected" into the 
	 * variable {@link PopulationHandler#planSelectedVal}.
	 * @param attributes Attributes of the plan element. Usually provided by the SAX engine.
	 * @throws SAXException When a mandatory attribute of the plan element is missing or empty.
	 */
	private void processPlan(Attributes attributes) throws SAXException {
		discardThisPlan = false;
		String selectedVal = attributes.getValue(planSelectedName);
		Utils.ensureNonNullAndNonEmpty(selectedVal);
		if (selectedVal.equals(planSelectedTrue)){
			planSelectedVal = true;
		} else if (selectedVal.equals(planSelectedFalse)){
			planSelectedVal = false;
		} else {
			throw new SAXException(new InvalidAttributeValueException());
		}
	}
	
	/**
	 * Defines action to be taken when entering a route element.
	 * Checks for the route format and prepares to parse the CDATA section of the element.
	 * @param attributes Attributes of the route element. Usually provided by the SAX engine.
	 */
	private void processRoute(Attributes attributes) {
		String typeVal = attributes.getValue(routeTypeName);
		routeChars.setLength(0);
		if (Utils.checkNonNullAndNonEmpty(typeVal) && typeVal.equals(routeTypeLinks)){
			insideRoute = true;
		} else {
			discardThisPlan = true;
		}
	}
	
	/**
	 * Defines action to be taken when entering a person element.
	 * Collects and saves the values of the mandatory and optional attributes.
	 * @param attributes Attributes of the person element. Usually provided by the SAX engine.
	 * @throws SAXException When a mandatory attribute of the plan element is missing or empty, 
	 * or an optional attribute has an invalid value.
	 */
	private void processPerson(Attributes attributes) throws SAXException {
		personIdVal = attributes.getValue(personIdName);
		Utils.ensureNonNullAndNonEmpty(personIdVal);
		String sex = attributes.getValue(personSexName);
		personSexVal = null;
		if (Utils.checkNonNullAndNonEmpty(sex)){
			if (sex.equals(personSexMale)){
				personSexVal = Boolean.TRUE;
			} else if (sex.equals(personSexFemale)){
				personSexVal = Boolean.FALSE;
			} else {
				throw new SAXException(new InvalidAttributeValueException());
			}
		}
		String age = attributes.getValue(personAgeName);
		personAgeVal = null;
		if (Utils.checkNonNullAndNonEmpty(age)){
			try {
				personAgeVal = Integer.valueOf(age);
			} catch (NumberFormatException ex){
				throw new SAXException(new InvalidAttributeValueException());
			}
		}
		String license = attributes.getValue(personLicenseName);
		personLicenseVal = null;
		if (Utils.checkNonNullAndNonEmpty(license)){
			if (license.equals(personLicenseYes)){
				personLicenseVal = Boolean.TRUE;
			} else if (license.equals(personLicenseNo)){
				personLicenseVal = Boolean.FALSE;
			} else {
				throw new SAXException(new InvalidAttributeValueException());
			}
		}
		String carAvail = attributes.getValue(personCarAvailName);
		personCarAvailVal = null;
		if (Utils.checkNonNullAndNonEmpty(carAvail)){
			if (carAvail.equals(personCarAvailAlways) ||
					carAvail.equals(personCarAvailNever) || carAvail.equals(personCarAvailSometimes)){
				personCarAvailVal = carAvail;
			} else {
				throw new SAXException(new InvalidAttributeValueException());
			}
		}
		String employed = attributes.getValue(personEmployedName);
		personEmployedVal = null;
		if (Utils.checkNonNullAndNonEmpty(employed)){
			if (employed.equals(personEmployedYes)){
				personEmployedVal = Boolean.TRUE;
			} else if (employed.equals(personEmployedNo)){
				personEmployedVal = Boolean.FALSE;
			} else {
				throw new SAXException(new InvalidAttributeValueException());
			}
		}
	}
}
