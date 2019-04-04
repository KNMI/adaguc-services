package nl.knmi.adaguc.services.servicehealth;

import nl.knmi.adaguc.tools.ElementNotFoundException;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import nl.knmi.adaguc.config.ConfigurationReader;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;

/**
 * 
 * @author maartenplieger
 * 

*/


public class ServiceHealthConfigurator implements nl.knmi.adaguc.config.ConfiguratorInterface {
	private static boolean enabled=false;
	// The directory that contains a file per service with the return code of updatedb
	private static String serviceHealthDirectory = null;
	@Autowired
	static ConfigurationReader configurationReader;
	public static void doConfig(XMLElement  configReader){
		if(configReader.getNodeValue("adaguc-services.servicehealth") == null){
			return;
		}
		String enabledStr=configReader.getNodeValue("adaguc-services.servicehealth.enabled");
		if(enabledStr != null && enabledStr.equals("true")){
			enabled = true;
		}
		
		if(enabled){
			serviceHealthDirectory=configReader.getNodeValue("adaguc-services.servicehealth.servicehealthpath");

		}
	}


	public static String getServiceHealthDirectory() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return serviceHealthDirectory;
	}

	public static boolean getEnabled() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return enabled;
	}
}