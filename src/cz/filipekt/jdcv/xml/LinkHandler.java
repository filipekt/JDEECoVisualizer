package cz.filipekt.jdcv.xml;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Point2D;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import cz.filipekt.jdcv.exceptions.InvalidAttributeValueException;
import cz.filipekt.jdcv.exceptions.NodeNotFoundException;
import cz.filipekt.jdcv.network.MyLink;
import cz.filipekt.jdcv.network.MyLinkBuilder;
import cz.filipekt.jdcv.network.MyLinkImg;
import cz.filipekt.jdcv.network.MyNode;

/**
 * SAX handler used to parse the XML file containing the network(map) description.
 * Collects the "link" elements. 
 * Can only be used after the "node" elements have been collected.
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
public class LinkHandler extends DefaultHandler {
	
	/**
	 * Local name of the link XML element.
	 */
	private final String linkName = "link";
	
	/**
	 * Name of the link attribute containing the id of the link
	 */
	private final String idName = "id";
	
	/**
	 * Name of the link attribute containing the id of the node where this link starts.
	 */
	private final String fromName = "from";
	
	/**
	 * Name of the link attribute containing the id of the node where this link ends.
	 */
	private final String toName = "to";
	
	/**
	 * Name of the link attribute containing the length of this link.
	 */
	private final String lengthName = "length";
	
	/**
	 * Name of the link attribute containing the allowed maximum speed of the link 
	 * (for street link), the 'fixed' speed of the link (for public transport links), 
	 * the 'typical' speed of the link (for pedestrian links).
	 */
	private final String freespeedName = "freespeed";
	
	/**
	 * Name of the link attribute containing the maximal capacity of this link 
	 * for a given period (specified by "links" element attribute "capperiod").
	 */
	private final String capacityName = "capacity";
	
	/**
	 * Name of the link attribute containing the number of lanes of this link.
	 */
	private final String permlanesName = "permlanes";
	
	/**
	 * Name of the link attribute containing the comma-separated list of 
	 * transportation modes that are allowed on this link.
	 */
	private final String modesName = "modes";
	
	/**
	 * Contains the {@link MyLink} representations of the encountered link elements.
	 */
	private final Map<String,MyLink> links = new HashMap<>();
	
	/**
	 * @return The {@link MyLink} representations of the encountered link elements.
	 * @see {@link LinkHandler#links}
	 */
	public Map<String, MyLink> getLinks() {
		return links;
	}

	/**
	 * Contains the {@link MyNode} representations of all the "node" elements.
	 */
	private final Map<String,MyNode> nodes;

	/**
	 * The link elements of the network XML source contain mandatory attributes "from" and "to",
	 * which refer to ids of some network nodes. When parsing the link elements, these node ids
	 * are immediately dereferenced into {@link MyNode} representations of nodes. Thus the node
	 * collection is required.
	 * @param nodes Presumably the result of parsing all of the "node" elements in the network XML file. 
	 * Maps the node id to the {@link MyNode} representation of the node.
	 */
	public LinkHandler(Map<String, MyNode> nodes) {
		this.nodes = nodes;
	}

	/**
	 * Called by the {@link XMLReader} during the SAX parsing, when an element is entered.
	 */
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (qName.equals(linkName)){
			processLink(attributes);
		}
		if (qName.equals(linkImgName)){
			processLinkImg(attributes);
		}
		if (qName.equals(linkPathName)){
			points = new ArrayList<>();
		}
		if (qName.equals(pointName)){
			processPoint(attributes);
		}
	}
	
	/**
	 * Called by the {@link XMLReader} during the SAX parsing, when an element is being left.
	 * Makes sure that when the name of the element is "link", proper actions are taken.
	 * @see {@link LinkHandler#processLink(Attributes)}
	 */
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName.equals(linkName)){
			if (linkBuilder != null){
				linkBuilder.setLinkImage(linkImage);
				linkImage = null;
				linkBuilder.setPathPoints(points);
				points = null;
				MyLink link = linkBuilder.build();
				linkBuilder = null;
				links.put(link.getId(), link);
			}
		}		
	}
	
	/**
	 * Points describing the path along which cars/people move. 
	 * Taken from the last encountered link_path element 
	 */
	private List<Point2D> points;
	
	/**
	 * Local name of the link_path element specifying the path through which
	 * cars/persons go when traveling through the link
	 */
	private final String linkPathName = "link_path";
	
	/**
	 * Local name of the point element specifying a point in the map,
	 * for example inside a link_path element
	 */
	private final String pointName = "point";
	
	/**
	 * Name of the attribute specifying the x-coordinate of a point
	 */
	private final String pointXName = "x";
	
	/**
	 * Name of the attribute specifying the y-coordinate of a point
	 */
	private final String pointYName = "y";

	/**
	 * Local name of the link_img element
	 */
	private final String linkImgName = "link_img";
	
	/**
	 * Name of the attribute specifying the path to the image that reperesents a link
	 */
	private final String linkImgSourceName = "source";
	
	/**
	 * Name of the attribute specifying the x-coordinate of the point in the
	 * link visualization where the link starts.
	 */
	private final String linkImgFromXName = "fromx";
	
	/**
	 * Name of the attribute specifying the y-coordinate of the point in the
	 * link visualization where the link starts.
	 */
	private final String linkImgFromYName = "fromy";
	
	/**
	 * Name of the attribute specifying the x-coordinate of the point in the
	 * link visualization where the link ends.
	 */
	private final String linkImgToXName = "tox";
	
	/**
	 * Name of the attribute specifying the y-coordinate of the point in the
	 * link visualization where the link ends.
	 */
	private final String linkImgToYName = "toy";
	
	/**
	 * Called when a point element is encountered. It parses the element and
	 * adds the parsed form to {@link LinkHandler#points} 
	 * @param attributes Attributes of the point element.
	 * @throws SAXException When a mandatory attribute is missing, empty or has an invalid value.
	 */
	private void processPoint(Attributes attributes) throws SAXException{
		String xStr = attributes.getValue(pointXName);
		Utils.ensureNonNullAndNonEmpty(xStr);
		String yStr = attributes.getValue(pointYName);
		Utils.ensureNonNullAndNonEmpty(yStr);
		try {
			int x = Integer.parseInt(xStr);
			int y = Integer.parseInt(yStr);
			Point2D point = new Point2D(x, y);
			points.add(point);
		} catch (NumberFormatException ex){
			throw new SAXException(new InvalidAttributeValueException());
		}
	}
	
	/**
	 * Called when a link_img element is encountered. It parses the element and
	 * puts the parsed form into {@link LinkHandler#linkImage} 
	 * @param attributes The attributes of the link_img element
	 * @throws SAXException When a mandatory attribute is missing, empty or has an invalid value.
	 */
	private void processLinkImg(Attributes attributes) throws SAXException{
		String source = attributes.getValue(linkImgSourceName);
		Utils.ensureNonNullAndNonEmpty(source);
		String fromx = attributes.getValue(linkImgFromXName);
		Utils.ensureNonNullAndNonEmpty(fromx);
		String fromy = attributes.getValue(linkImgFromYName);
		Utils.ensureNonNullAndNonEmpty(fromy);
		String tox = attributes.getValue(linkImgToXName);
		Utils.ensureNonNullAndNonEmpty(tox);
		String toy = attributes.getValue(linkImgToYName);
		Utils.ensureNonNullAndNonEmpty(toy);
		try {
			int fromXNum = Integer.parseInt(fromx);
			int fromYNum = Integer.parseInt(fromy);
			int toXNum = Integer.parseInt(tox);
			int toYNum = Integer.parseInt(toy);
			linkImage = new MyLinkImg(source, fromXNum, fromYNum, toXNum, toYNum);
		} catch (NumberFormatException ex){
			throw new SAXException(new InvalidAttributeValueException());
		}
	}
	
	/**
	 * Builder for {@link MyLink}
	 */
	private MyLinkBuilder linkBuilder;
	
	/**
	 * Specification of the image which represents the last encountered link
	 */
	private MyLinkImg linkImage;
	
	/**
	 * When a "link" element is encountered, this method creates a {@link MyLink} representation 
	 * of this element and places it in the {@link LinkHandler#links} map.
	 * @param attributes The attributes of the currently encountered link element.
	 * @throws SAXException When a mandatory attribute is missing, empty or has an invalid value.
	 */
	private void processLink(Attributes attributes) throws SAXException {
		String id = attributes.getValue(idName);
		Utils.ensureNonNullAndNonEmpty(id);
		String from = attributes.getValue(fromName);
		Utils.ensureNonNullAndNonEmpty(from);
		String to = attributes.getValue(toName);
		Utils.ensureNonNullAndNonEmpty(to);		
		String length = attributes.getValue(lengthName);
		if (!Utils.checkNonNullAndNonEmpty(length)){
			length = null;
		}
		String freespeed = attributes.getValue(freespeedName);
		if (!Utils.checkNonNullAndNonEmpty(freespeed)){
			freespeed = null;
		}
		String capacity = attributes.getValue(capacityName);
		if (!Utils.checkNonNullAndNonEmpty(capacity)){
			capacity = null;
		}
		String permlanes = attributes.getValue(permlanesName);
		if (!Utils.checkNonNullAndNonEmpty(permlanes)){
			permlanes = null;
		}
		String modes = attributes.getValue(modesName);
		if (!Utils.checkNonNullAndNonEmpty(modes)){
			modes = null;
		}
		MyNode fromNode = nodes.get(from);
		MyNode toNode = nodes.get(to);
		if ((fromNode==null) || (toNode==null)){
			throw new SAXException(new NodeNotFoundException());
		}
		try {
			linkBuilder = new MyLinkBuilder();
			linkBuilder.setId(id);
			linkBuilder.setFrom(fromNode);
			linkBuilder.setTo(toNode);
			if (length != null){
				BigDecimal lengthDecimal = new BigDecimal(length);
				linkBuilder.setLength(lengthDecimal);
			}
			if (freespeed != null){
				double freespeedDouble = Double.parseDouble(freespeed);
				linkBuilder.setFreespeed(freespeedDouble);
			}
			if (capacity != null){
				double capacityDouble = Double.parseDouble(capacity);
				linkBuilder.setCapacity(capacityDouble);
			}
			if (permlanes != null){
				double permlanesDouble = Double.parseDouble(permlanes);
				linkBuilder.setNumberOfLanes(permlanesDouble);
			}			
			if (modes != null){
				String[] modesArray = modes.split(",");
				linkBuilder.setAllowedModes(modesArray);
			}																							
		} catch (NumberFormatException ex){
			throw new SAXException(new InvalidAttributeValueException());
		}
	}
	
}