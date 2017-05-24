package nl.knmi.adaguc.config;

import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;

/**
 * @author maartenplieger
 * New configurators must implement this class. New configurators are automatically found by using Reflection on classes which implement this interface.
 *
 */
public interface ConfiguratorInterface {
	/**
	 * This method is called when the configuration is read.
	 * @param configReader The configuration file object
	 * @throws Exception 
	 */
	public void doConfig(XMLElement configReader) throws ConfigurationItemNotFoundException;
}
