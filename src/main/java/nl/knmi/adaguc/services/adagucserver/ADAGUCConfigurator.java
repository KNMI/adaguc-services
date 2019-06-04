package nl.knmi.adaguc.services.adagucserver;

import nl.knmi.adaguc.tools.ElementNotFoundException;

import java.io.IOException;

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
  	<timeout>1000</timeout>
  	<maxinstances>4</maxinstances>
  	<queuesize>4</queuesize>
    <adagucexecutable>/home/c3smagic/code/maartenplieger/adaguc-server/bin/adagucserver</adagucexecutable>
    <export>ADAGUC_PATH=/home/c3smagic/code/maartenplieger/adaguc-server/</export>
    <export>ADAGUC_CONFIG=/home/c3smagic/code/maartenplieger/adaguc-server/data/config/adaguc.autoresource.xml</export>
    <export>ADAGUC_DATARESTRICTION="FALSE"</export>
    <export>ADAGUC_FONT=/home/c3smagic/code/maartenplieger/adaguc-server/data/fonts/FreeSans.ttf</export>
  </adaguc-server>
</adaguc-services>
*/


public class ADAGUCConfigurator implements nl.knmi.adaguc.config.ConfiguratorInterface {
	private static String ADAGUCExecutable="/usr/bin/adagucserver";
	
	@Autowired
	static ConfigurationReader configurationReader;
	
	private static String[] environmentVariables = {
	};
	
	/* How long adaguc-server is allowed to run in milliseconds, -1 is unlimted */
	private static long timeOut = -1;
	/* Number of maximum simultaneaous instances before instances are queued, -1 is unlimited */
	private static int maxInstances = -1;
	/* Maximum queuesize, -1 is unlimited */
	private static int maxInstancesInQueue = -1;

	public static void doConfig(XMLElement  configReader){
		ADAGUCExecutable=configReader.getNodeValue("adaguc-services.adaguc-server.adagucexecutable");
		environmentVariables = configReader.getNodeValues("adaguc-services.adaguc-server.export");
		try { timeOut = Long.parseLong(configReader.getNodeValue("adaguc-services.adaguc-server.timeout"));} catch (Exception e) {}
		try { maxInstances = Integer.parseInt(configReader.getNodeValue("adaguc-services.adaguc-server.maxinstances"));} catch (Exception e) {}
		try { maxInstancesInQueue = Integer.parseInt(configReader.getNodeValue("adaguc-services.adaguc-server.queuesize"));} catch (Exception e) {}
	}

	public static String getADAGUCExecutable() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return ADAGUCExecutable;
	}

	public static String[] getADAGUCEnvironment() throws ElementNotFoundException, IOException {
		
		ConfigurationReader.readConfig();
		return environmentVariables;
	}

	public static long getTimeOut() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return timeOut;
	}	
	
	public static int getMaxInstances () throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return maxInstances;
	}	
	
	public static int getMaxInstancesInQueue () throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return maxInstancesInQueue;
	}	
}