package cz.filipekt.jdcv.xml;

import org.xml.sax.SAXException;

import cz.filipekt.jdcv.exceptions.MandatoryAttributeNotFoundException;

/**
 * Contains static utility methods for use inside the package.
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
public class Utils {
	
	/**
	 * Ensures that the given String is not null and not empty.
	 * @param value The String to be examined
	 * @throws SAXException When the given String is either null or empty.
	 */
	static void ensureNonNullAndNonEmpty(String value) throws SAXException{
		if ((value == null) || value.isEmpty()){
			throw new SAXException(new MandatoryAttributeNotFoundException());
		}
	}
	
	/**
	 * Checks whether the given String is not null and not empty.
	 * @param value The String to be examined
	 */
	static boolean checkNonNullAndNonEmpty(String value) {
		if ((value == null) || value.isEmpty()){
			return false;
		} else {
			return true;
		}
	}
	
}