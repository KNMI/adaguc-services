package nl.knmi.adaguc.config;

import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;

/**
<?xml version="1.0" encoding="UTF-8"?>
<adaguc-services>
  <external-home-url>http://172.17.0.2:8080</external-home-url>
</adaguc-services>
*/

public class MainServicesConfigurator implements ConfiguratorInterface{
	private static String serverExternalURL="";
	private static String userWorkspace="/tmp";
	private static String serverPort="443";
	private static String baseDir="/tmp/";
	
	static ConfigurationReader configurationReader = new ConfigurationReader ();
	
	public static void doConfig(XMLElement configReader) throws ElementNotFoundException {
		serverExternalURL = configReader.getNodeValueMustNotBeUndefined("adaguc-services.external-home-url");
		userWorkspace = configReader.getNodeValueMustNotBeUndefined("adaguc-services.userworkspace");
		serverPort = configReader.getNodeValue("adaguc-services.server.port");
		baseDir = configReader.getNodeValueMustNotBeUndefined("adaguc-services.basedir");
	}

	public static String getServerExternalURL() throws ElementNotFoundException {
		configurationReader.readConfig();
		return serverExternalURL;
	}
	
	public static String getUserWorkspace() throws ElementNotFoundException{
		configurationReader.readConfig();
		return userWorkspace;
	}

	public static String getServerPort() throws ElementNotFoundException {
		configurationReader.readConfig();
		return serverPort;
		
	}

	public static String getBaseDir() throws ElementNotFoundException {
		configurationReader.readConfig();
		return baseDir;
	}
}