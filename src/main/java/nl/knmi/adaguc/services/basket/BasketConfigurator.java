package nl.knmi.adaguc.services.basket;

import nl.knmi.adaguc.tools.ElementNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;

import nl.knmi.adaguc.config.ConfigurationReader;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;

public class BasketConfigurator implements nl.knmi.adaguc.config.ConfiguratorInterface{
	private static boolean enabled=false;
	@Autowired
	static ConfigurationReader configurationReader;
	public static void doConfig(XMLElement configReader) throws ElementNotFoundException {
		if(configReader.getNodeValue("adaguc-services.basket") == null){
			return;
		}
		String enabledStr=configReader.getNodeValue("adaguc-services.basket.enabled");
		if(enabledStr != null && enabledStr.equals("true")){
			enabled = true;
		}
		
		if(enabled){
		}
	}
	
	public static boolean getEnabled() throws ElementNotFoundException {
		ConfigurationReader.readConfig();
		return enabled;
	}
	
}
