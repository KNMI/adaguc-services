package nl.knmi.adaguc.services.esgfsearch;

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

	static ConfigurationReader configurationReader = new ConfigurationReader ();

	@Synchronized
	@Override
	public void doConfig(XMLElement  configReader){
		if(configReader.getNodeValue ("adaguc-services.esgfsearch")==null){
			return;
		}
		esgfSearchURL=configReader.getNodeValue("adaguc-services.esgfsearch.searchurl");
		enabled="true".equals(configReader.getNodeValue("adaguc-services.esgfsearch.enabled"));
		cacheLocation=configReader.getNodeValue("adaguc-services.esgfsearch.cachelocation");
	}
	public static String getEsgfSearchURL() throws ElementNotFoundException {
		configurationReader.readConfig();
		return esgfSearchURL;
	}
	public static boolean getEnabled() throws ElementNotFoundException {
		configurationReader.readConfig();
		return enabled;
	}
	public static String getCacheLocation() throws ElementNotFoundException {
		configurationReader.readConfig();
		return cacheLocation;
	}

}


