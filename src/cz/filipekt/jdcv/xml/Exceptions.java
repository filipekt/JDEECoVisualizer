package cz.filipekt.jdcv.xml;


/**
 * Thrown when a mandatory attribute is not present in a processed element. 
 */
@SuppressWarnings("serial")
class MandatoryAttributeNotFoundException extends Exception {}


/**
 * Thrown when the value of a mandatory attribute does not contain the required 
 * data type (according to the DTD definition).
 */
@SuppressWarnings("serial")
class InvalidAttributeValueException extends Exception {}


/**
 * Thrown when a link element refers to a node element that has not been encountered in the XML file.
 */
@SuppressWarnings("serial")
class NodeNotFoundException extends Exception {}


/**
 * Thrown when an XML element refers to a "link" element that is not present in the network XML definition file
 */
@SuppressWarnings("serial")
class LinkNotFoundException extends Exception {}