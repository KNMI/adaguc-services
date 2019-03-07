package nl.knmi.adaguc.services.esgfsearch;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.Synchronized;
import nl.knmi.adaguc.config.ConfigurationReader;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;

/**
 * 
 * @author maartenplieger
 * 
<?xml version="1.0" encoding="UTF-8"?>
<adaguc-services>
  <esgfsearch>
    <searchurl>changeit</searchurl>
  </esgfsearch>
</security-services>
 */


public class ESGFSearchConfigurator implements nl.knmi.adaguc.config.ConfiguratorInterface {

	private static String esgfSearchURL=null;
	private static String cacheLocation="/tmp/";
	private static boolean enabled = false;

	@Autowired
	static ConfigurationReader configurationReader;

	@Synchronized
	public static void doConfig(XMLElement  configReader){
		if(configReader.getNodeValue ("adaguc-services.esgfsearch")==null){
			return;
		}
		esgfSearchURL=configReader.getNodeValue("adaguc-services.esgfsearch.searchurl");
		enabled="true".equals(configReader.getNodeValue("adaguc-services.esgfsearch.enabled"));
		cacheLocation=configReader.getNodeValue("adaguc-services.esgfsearch.cachelocation");
	}
	public static String getEsgfSearchURL() throws ElementNotFoundException {
		ConfigurationReader.readConfig();
		return esgfSearchURL;
	}
	public static boolean getEnabled() throws ElementNotFoundException {
		ConfigurationReader.readConfig();
		return enabled;
	}
	public static String getCacheLocation() throws ElementNotFoundException {
		ConfigurationReader.readConfig();
		return cacheLocation;
	}

}


