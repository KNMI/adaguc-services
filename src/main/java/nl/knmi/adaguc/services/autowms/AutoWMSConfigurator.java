package nl.knmi.adaguc.services.autowms;

import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.config.ConfigurationReader;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;

/**
 * 
 * @author maartenplieger
 * 

*/


public class AutoWMSConfigurator implements nl.knmi.adaguc.config.ConfiguratorInterface {
	private static boolean enabled=false;
	private static String adagucAutoWMS = null;
	private static String adagucDataset = null;
	static ConfigurationReader configurationReader = new ConfigurationReader ();
	public void doConfig(XMLElement  configReader){
		if(configReader.getNodeValue("adaguc-services.autowms") == null){
			return;
		}
		String enabledStr=configReader.getNodeValue("adaguc-services.autowms.enabled");
		if(enabledStr != null && enabledStr.equals("true")){
			enabled = true;
		}
		
		if(enabled){
			adagucAutoWMS=configReader.getNodeValue("adaguc-services.autowms.autowmspath");
			adagucDataset=configReader.getNodeValue("adaguc-services.autowms.datasetpath");

		}
	}


	public static String getAdagucDataset() throws ElementNotFoundException {
		configurationReader.readConfig();
		return adagucDataset;
	}
	public static String getAdagucAutoWMS() throws ElementNotFoundException {
		configurationReader.readConfig();
		return adagucAutoWMS;
	}

	public static boolean getEnabled() throws ElementNotFoundException {
		configurationReader.readConfig();
		return enabled;
	}
}