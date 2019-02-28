package nl.knmi.adaguc.services.adagucserver;

import nl.knmi.adaguc.tools.ElementNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;

import nl.knmi.adaguc.config.ConfigurationReader;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;

/**
 * 
 * @author maartenplieger
 * 
<?xml version="1.0" encoding="UTF-8"?>
<adaguc-services>
  <adaguc-server>
    <adagucexecutable>/home/c3smagic/code/maartenplieger/adaguc-server/bin/adagucserver</adagucexecutable>
    <export>ADAGUC_PATH=/home/c3smagic/code/maartenplieger/adaguc-server/</export>
    <export>ADAGUC_TMP=/tmp</export>
    <export>ADAGUC_CONFIG=/home/c3smagic/code/maartenplieger/adaguc-server/data/config/adaguc.autoresource.xml</export>
    <export>ADAGUC_DATARESTRICTION="FALSE"</export>
    <export>ADAGUC_LOGFILE=/tmp/adaguc.autoresource.log</export>
    <export>ADAGUC_ERRORFILE=/tmp/adaguc.autoresource.errlog</export>
    <export>ADAGUC_FONT=/home/c3smagic/code/maartenplieger/adaguc-server/data/fonts/FreeSans.ttf</export>
    <export>ADAGUC_ONLINERESOURCE=http://adaguc-services/adagucserver?</export>
  </adaguc-server>
</adaguc-services>
*/


public class ADAGUCConfigurator implements nl.knmi.adaguc.config.ConfiguratorInterface {
	private static String ADAGUCExecutable="/usr/bin/adagucserver";
	
	@Autowired
	static ConfigurationReader configurationReader;
	
	private static String[] environmentVariables = {
	};

	public static void doConfig(XMLElement  configReader){
		ADAGUCExecutable=configReader.getNodeValue("adaguc-services.adaguc-server.adagucexecutable");
		environmentVariables = configReader.getNodeValues("adaguc-services.adaguc-server.export");
	}

	public static String getADAGUCExecutable() throws ElementNotFoundException {
		ConfigurationReader.readConfig();
		return ADAGUCExecutable;
	}

	public static String[] getADAGUCEnvironment() throws ElementNotFoundException {
		
		ConfigurationReader.readConfig();
		return environmentVariables;
	}	
}