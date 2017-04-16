package nl.knmi.adaguc.services.serverconfig;


import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;



public class ConfigurationReader {
	/*
	 * 
<xml>
  <external-home-url>http://172.17.0.2:8080</<external-home-url>
  <adaguc-server>
    <adagucexecutable>/usr/bin/adagucserver</adagucexecutable>
    <export>ADAGUC_PATH=/src/adaguc-server-master</export>
    <export>ADAGUC_PATH=/src/adaguc-server-master</export>
    <export>ADAGUC_TMP=/tmp</export>
    <export>ADAGUC_CONFIG="/data/config/adaguc.autoresource.xml"</export>
    <export>ADAGUC_DATARESTRICTION="FALSE"</export>
    <export>ADAGUC_LOGFILE="/adaguc.autoresource.log"</export>
    <export>ADAGUC_ERRORFILE="/adaguc.autoresource.errlog"</export>
    <export>ADAGUC_FONT="/data/fonts/FreeSans.ttf"</export>
    <export>ADAGUC_ONLINERESOURCE="http:///adaguc-services/adagucserver?"</export>
  </adaguc-server>
</xml>

	 */


	static long readConfigPolInterval = 0;;

	public static String getHomePath(){
		return System.getProperty("user.home")+"/";
	}

	private static String getConfigFile(){
		try{
			String configLocation = System.getenv("ADAGUC-SERVICES_CONFIG");
			if(configLocation!=null){
				if(configLocation.length()>0){
					return configLocation;
				}
			}
		}catch(Exception e){
		}

		return getHomePath()+"adaguc-services-config.xml";
	}




	public static synchronized void readConfig(){
		//Re-read the config file every 10 seconds.
		if(readConfigPolInterval != 0){
			if(System.currentTimeMillis()<readConfigPolInterval+10000)return;
		}
		readConfigPolInterval=System.currentTimeMillis(); 
		Debug.println("Reading configfile "+getConfigFile());
		XMLElement configReader = new XMLElement();
		try {
			configReader.parseFile(getConfigFile());
		} catch (Exception e) {
			Debug.println("Unable to read "+getConfigFile());
			configReader = null;
			return;
		}

		Main.doConfig(configReader);
		ADAGUCServerConfig.doConfig(configReader);

		configReader = null; 
	}

	public static class Main{
		public static void doConfig(XMLElement configReader) {
			serverExternalURL = configReader.getNodeValue("adaguc-services.external-home-url");

		}
		private static String serverExternalURL="";
		public static String getServerExternalURL(){
			readConfig();
			return serverExternalURL;
		}
	}

	public static class ADAGUCServerConfig{

		private static String ADAGUCExecutable="/usr/bin/adagucserver";

		private static String[] environmentVariables = {
		};

		public static void doConfig(XMLElement  configReader){
			ADAGUCExecutable=configReader.getNodeValue("adaguc-services.adaguc-server.adagucexecutable");
			Debug.println("ADAGUCExecutable"+ ADAGUCExecutable);
			environmentVariables = configReader.getNodeValues("adaguc-services.adaguc-server.export");
		}

		public static String getADAGUCExecutable() {
			readConfig();
			return ADAGUCExecutable;
		}

		public static String[] getADAGUCEnvironment() {
			readConfig();
			return environmentVariables;
		}
	}

}

