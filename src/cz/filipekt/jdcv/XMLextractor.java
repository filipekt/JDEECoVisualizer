package cz.filipekt.jdcv;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import cz.filipekt.jdcv.network.MyLink;
import cz.filipekt.jdcv.network.MyNode;

/**
 * Takes care of loading and parsing the input XML files.
 * @author Tom
 *
 */
public class XMLextractor {
	
	/**
	 * Path to the XML file containing the map of the simulated place.
	 */
	private final Path sourceFile;
	
	public XMLextractor(Path sourceFile) {
		this.sourceFile = sourceFile;
	}
	
	/**
	 * Extracts the requested data from the input XML files.
	 * @return An instance of {@link XMLresult} containing the parsed data.
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public XMLresult doExtraction() throws ParserConfigurationException, SAXException, IOException{
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(false);
		SAXParser saxParser = spf.newSAXParser();
		XMLReader xmlReader = saxParser.getXMLReader();
		MyHandler mh = new MyHandler();
		xmlReader.setContentHandler(mh);
		InputSource is = new InputSource(Files.newInputStream(sourceFile));
		xmlReader.parse(is);
		return new XMLresult(mh.getNodes(), mh.getLinks());
	}
	
}

/**
 * Contains the results of parsing an XML file.
 * @author Tom
 *
 */
class XMLresult {
	private final Map<String,MyNode> nodes;
	private final Map<String,MyLink> links;
	public XMLresult(Map<String, MyNode> nodes, Map<String, MyLink> links) {
		super();
		this.nodes = nodes;
		this.links = links;
	}
	public Map<String, MyNode> getNodes() {
		return nodes;
	}
	public Map<String, MyLink> getLinks() {
		return links;
	}
		
}

/**
 * SAX handler used to parse the XML file containing the map description.
 * For now, it is supposed that all "node" elements precede all of the "link" elements in the
 * input XML map description (will be fixed shortly).
 * @author Tom
 *
 */
class MyHandler extends DefaultHandler {
	
	/**
	 * Local name of the node element.
	 */
	private final String nodeName = "node";
	
	/**
	 * Local name of the link element.
	 */
	private final String linkName = "link";
	
	/**
	 * Contains the {@link MyNode} representations of the node elements.
	 */
	private final Map<String,MyNode> nodes = new HashMap<>();
	
	/**
	 * Contains the {@link MyLink} representations of the link elements.
	 */
	private final Map<String,MyLink> links = new HashMap<>();
	
	private final String idName = "id";
	private final String xName = "x";
	private final String yName = "y";
	private final String fromName = "from";
	private final String toName = "to";
	private final String lengthName = "length";
	private final String freespeedName = "freespeed";
	private final String capacityName = "capacity";
	private final String permlanesName = "permlanes";
	private final String modesName = "modes";

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {	
		
		if (qName.equals(nodeName)){
			try {
				processNode(attributes);
			} catch (MandatoryAttributeNotFoundException | InvalidAttributeValueException ex){
				// do nothing, continue with next node
			}
		}
		if (qName.equals(linkName)){
			try {
				processLink(attributes);
			} catch (InvalidAttributeValueException | MandatoryAttributeNotFoundException ex){
				// do nothing, continue with next node
			}
		}
	}
	
	

	public Map<String, MyNode> getNodes() {
		return nodes;
	}

	public Map<String, MyLink> getLinks() {
		return links;
	}

	/**
	 * Thrown when a mandatory attribute is not present in a processed element. 
	 * @author Tom
	 *
	 */
	private static class MandatoryAttributeNotFoundException extends Exception {

		private static final long serialVersionUID = -3992499046298588793L;
	}
	
	/**
	 * Thrown when the value of a mandatory attribute does not contain the required 
	 * data type (according to the DTD definition).
	 * @author Tom
	 *
	 */
	private static class InvalidAttributeValueException extends Exception {

		private static final long serialVersionUID = 750577050657060443L;
	}
	
	/**
	 * Thrown when a link element refers to a node element that has not been encountered in the XML file.
	 * @author Tom
	 *
	 */
	private static class NodeNotFoundException extends Exception {

		private static final long serialVersionUID = -19864586710695852L;
	}
	
	/**
	 * When a "node" elements is encountered, this method creates a {@link MyNode} representation 
	 * of this element and places it in the {@link MyHandler#nodes} map.
	 * @param attributes
	 * @throws MandatoryAttributeNotFoundException
	 * @throws InvalidAttributeValueException
	 */
	private void processNode(Attributes attributes) throws MandatoryAttributeNotFoundException, InvalidAttributeValueException{
		String id = attributes.getValue(idName);
		String x = attributes.getValue(xName);
		String y = attributes.getValue(yName);
		if ((id!=null) && (x!=null) && (y!=null)){
			try {
				double xd = Double.parseDouble(x);
				double yd = Double.parseDouble(y);
				MyNode node = new MyNode(id, xd, yd);
				nodes.put(node.getId(), node);
			} catch (NumberFormatException ex){
				throw new InvalidAttributeValueException();
			}
		} else {
			throw new MandatoryAttributeNotFoundException();
		}
		
	}
	
	/**
	 * When a "link" elements is encountered, this method creates a {@link MyLink} representation 
	 * of this element and places it in the {@link MyHandler#links} map.
	 * @param attributes
	 * @throws InvalidAttributeValueException
	 * @throws MandatoryAttributeNotFoundException
	 */
	private void processLink(Attributes attributes) throws InvalidAttributeValueException, MandatoryAttributeNotFoundException{
		String id = attributes.getValue(idName);
		String from = attributes.getValue(fromName);
		String to = attributes.getValue(toName);
		String length = attributes.getValue(lengthName);
		String freespeed = attributes.getValue(freespeedName);
		String capacity = attributes.getValue(capacityName);
		String permlanes = attributes.getValue(permlanesName);
		String modes = attributes.getValue(modesName);
		if ((id != null) && (from != null) && (to != null) && (length != null) &&
				(freespeed != null) && (capacity != null) && (permlanes != null) && (modes != null)){
			try {
				MyNode fromNode = nodes.get(from);
				MyNode toNode = nodes.get(to);
				if ((fromNode==null) || (toNode==null)){
					throw new NodeNotFoundException();
				}
				BigDecimal lengthDecimal = new BigDecimal(length);
				double freespeedDouble = Double.parseDouble(freespeed);
				double capacityDouble = Double.parseDouble(capacity);
				double permlanesDouble = Double.parseDouble(permlanes);
				String[] modesArray = modes.split(",");
				MyLink link = new MyLink(id, fromNode, toNode, lengthDecimal, freespeedDouble, capacityDouble, permlanesDouble, modesArray);
				links.put(id, link);
			} catch (NodeNotFoundException | NumberFormatException ex){
				throw new InvalidAttributeValueException();
			}
		} else {
			throw new MandatoryAttributeNotFoundException();
		}
	}
	
}
