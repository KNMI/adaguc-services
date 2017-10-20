package nl.knmi.adaguc.services.joblist;

import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.config.ConfigurationReader;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;

public class JobListConfigurator implements nl.knmi.adaguc.config.ConfiguratorInterface{
	private static boolean enabled=false;
	static ConfigurationReader configurationReader = new ConfigurationReader ();
	@Override
	public void doConfig(XMLElement configReader) throws ElementNotFoundException {
		if(configReader.getNodeValue("adaguc-services.joblist") == null){
			return;
		}
		String enabledStr=configReader.getNodeValue("adaguc-services.joblist.enabled");
		if(enabledStr != null && enabledStr.equals("true")){
			enabled = true;
		}

		if(enabled){
		}
	}

	public static boolean getEnabled() throws ElementNotFoundException {
		configurationReader.readConfig();
		return enabled;
	}

}

