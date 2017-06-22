package nl.knmi.adaguc.services.datasetcatalog;

import nl.knmi.adaguc.config.ConfigurationItemNotFoundException;
import nl.knmi.adaguc.config.ConfigurationReader;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;

public class DatasetCatalogConfigurator implements nl.knmi.adaguc.config.ConfiguratorInterface{
	private static boolean enabled=false;
	private static String catalogPath=null;
	@Override
	public void doConfig(XMLElement configReader) throws ConfigurationItemNotFoundException {
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
	
	public static boolean getEnabled() throws ConfigurationItemNotFoundException {
		ConfigurationReader.readConfig();
		return enabled;
	}
	
	public static String getCatalogPath() throws ConfigurationItemNotFoundException {
		ConfigurationReader.readConfig();
		return catalogPath;
	}
}
