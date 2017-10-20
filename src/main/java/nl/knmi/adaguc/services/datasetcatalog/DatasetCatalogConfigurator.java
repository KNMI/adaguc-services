package nl.knmi.adaguc.services.datasetcatalog;

import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.config.ConfigurationReader;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;

public class DatasetCatalogConfigurator implements nl.knmi.adaguc.config.ConfiguratorInterface{
	private static boolean enabled=false;
	private static String catalogPath=null;
	static ConfigurationReader configurationReader = new ConfigurationReader ();
	@Override
	public void doConfig(XMLElement configReader) throws ElementNotFoundException {
		if(configReader.getNodeValue("adaguc-services.basket") == null){
			return;
		}
		String enabledStr=configReader.getNodeValue("adaguc-services.datasetcatalog.enabled");
		if(enabledStr != null && enabledStr.equals("true")){
			enabled = true;
		}
		
		if(enabled){
			catalogPath=configReader.getNodeValue("adaguc-services.datasetcatalog.catalogpath");
		}
	}
	
	public static boolean getEnabled() throws ElementNotFoundException {
		configurationReader.readConfig();
		return enabled;
	}
	
	public static String getCatalogPath() throws ElementNotFoundException {
		configurationReader.readConfig();
		return catalogPath;
	}
}
