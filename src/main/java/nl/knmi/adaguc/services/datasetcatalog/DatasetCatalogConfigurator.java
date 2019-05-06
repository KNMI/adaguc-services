package nl.knmi.adaguc.services.datasetcatalog;

import nl.knmi.adaguc.tools.ElementNotFoundException;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import nl.knmi.adaguc.config.ConfigurationReader;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;

public class DatasetCatalogConfigurator implements nl.knmi.adaguc.config.ConfiguratorInterface{
	private static boolean enabled=false;
	private static String catalogPath=null;
	@Autowired
	static ConfigurationReader configurationReader;
	public static void doConfig(XMLElement configReader) throws ElementNotFoundException {
		if(configReader.getNodeValue("adaguc-services.basket") == null){
			return;
		}
		if(configReader.getNodeValue("adaguc-services.datasetcatalog") == null){
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
	
	public static boolean getEnabled() throws ElementNotFoundException , IOException{
		ConfigurationReader.readConfig();
		return enabled;
	}
	
	public static String getCatalogPath() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return catalogPath;
	}
}
