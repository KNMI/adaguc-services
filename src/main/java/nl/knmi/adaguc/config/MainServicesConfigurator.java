package nl.knmi.adaguc.config;

import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;

/**
<?xml version="1.0" encoding="UTF-8"?>
<adaguc-services>
  <external-home-url>http://172.17.0.2:8080</external-home-url>
</adaguc-services>
*/

public class MainServicesConfigurator implements ConfiguratorInterface {
	private static String serverExternalURL="";
	private static String userWorkspace="/tmp";
	private static String serverPort="443";

	public void doConfig(XMLElement configReader) {
		serverExternalURL = configReader.getNodeValue("adaguc-services.external-home-url");
		userWorkspace = configReader.getNodeValue("adaguc-services.userworkspace");
		serverPort = configReader.getNodeValue("adaguc-services.server.port");
	}

	public static String getServerExternalURL() throws ConfigurationItemNotFoundException {
		ConfigurationReader.readConfig();
		return serverExternalURL;
	}
	
	public static String getUserWorkspace() throws ConfigurationItemNotFoundException{
		ConfigurationReader.readConfig();
		return userWorkspace;
	}

	public static String getServerPort() throws ConfigurationItemNotFoundException {
		ConfigurationReader.readConfig();
		return serverPort;
		
	}
}