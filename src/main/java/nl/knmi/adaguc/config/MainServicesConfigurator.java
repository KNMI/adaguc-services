package nl.knmi.adaguc.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

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
	private static String serverPort="8080";
	private static String serverPortHTTPS="443";
	private static String baseDir="/tmp/";
	private static String contextPath="/";
	
	@Autowired
	static ConfigurationReader configurationReader;
	
	public static void doConfig(XMLElement configReader) throws ElementNotFoundException {
		serverExternalURL = configReader.getNodeValueMustNotBeUndefined("adaguc-services.external-home-url");
		userWorkspace = configReader.getNodeValueMustNotBeUndefined("adaguc-services.userworkspace");
		serverPort = configReader.getNodeValue("adaguc-services.server.port");
		serverPortHTTPS = configReader.getNodeValue("adaguc-services.server.porthttps");		
		baseDir = configReader.getNodeValueMustNotBeUndefined("adaguc-services.basedir");
		contextPath = configReader.getNodeValue("adaguc-services.server.contextpath");
	}

	public static String getServerExternalURL() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return serverExternalURL;
	}
	
	public static String getUserWorkspace() throws ElementNotFoundException, IOException{
		ConfigurationReader.readConfig();
		return userWorkspace;
	}

	public static String getServerPort() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return serverPort;
		
	}

	public static String getBaseDir() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return baseDir;
	}
	
	public static String getContextPath() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return contextPath;
	}

	public static Object getServerPortHTTPS() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return serverPortHTTPS;
	}
}