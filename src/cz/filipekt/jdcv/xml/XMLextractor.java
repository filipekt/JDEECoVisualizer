package cz.filipekt.jdcv.xml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Takes care of loading and parsing the input XML files. 
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
public class XMLextractor {

	/**
	 * The input XML file
	 */
	private final Path xmlFile;
	
	/**
	 * SAX handler used during the parsing
	 */
	private final ContentHandler handler;

	/**
	 * Only used by the {@link XMLextractor#run(Path, ContentHandler)} method.
	 * Just instantiates the final fields.
	 * @param xmlFile Input XML file 
	 * @param handler SAX handler used during the parsing
	 */
	private XMLextractor(Path xmlFile, ContentHandler handler) {
		this.xmlFile = xmlFile;
		this.handler = handler;
	}

	/**
	 * Runs the SAX parsing of the input file, using {@link XMLextractor#handler} as SAX handler.
	 * @throws SAXException When there is any problem when parsing the XML document.
	 * @throws If the {@link XMLextractor#xmlFile} does not exist or is inaccessible.
	 */
	private void doExtraction() throws ParserConfigurationException, SAXException, IOException{
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(false);
		SAXParser saxParser = spf.newSAXParser();
		XMLReader xmlReader = saxParser.getXMLReader();
		xmlReader.setContentHandler(handler);
		InputSource is = new InputSource(Files.newInputStream(xmlFile));
		xmlReader.parse(is);
	}
	
	/**
	 * Given an XML file and a {@link ContentHandler} it parses the input file by 
	 * SAX method, using the given handler. 
	 * @param xmlFile Input XML file
	 * @param handler SAX handler used during the parsing
	 * @throws NullPointerException Thrown when either of the parameters is null.
	 * @throws IOException If the {@link XMLextractor#xmlFile} does not exist or is inaccessible.
	 * @throws SAXException When there is any problem when parsing the XML document. 
	 * It is often used as a wrapper for other kinds of exceptions.
	 */
	public static void run(Path xmlFile, ContentHandler handler) throws ParserConfigurationException, 
			SAXException, IOException, NullPointerException {
		if ((xmlFile == null) || (handler == null)){
			throw new NullPointerException("Arguments of XMLextractor.run(..) must be non-null.");
		}
		XMLextractor instance = new XMLextractor(xmlFile, handler);
		instance.doExtraction();
	}
}
