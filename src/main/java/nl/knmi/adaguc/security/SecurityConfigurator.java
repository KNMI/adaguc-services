package nl.knmi.adaguc.security;

import nl.knmi.adaguc.config.ConfigurationReader;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;

/**
 * 
 * @author maartenplieger
 * 
<?xml version="1.0" encoding="UTF-8"?>
<adaguc-services>
  <security>
    <truststorepassword>changeit</truststorepassword>
    <truststore>/home/c3smagic/config/esg-truststore.ts</truststore>
    <trustrootscadirectory>/home/c3smagic/.globus/certificates/</trustrootscadirectory>
  </security>
</security-services>
*/


public class SecurityConfigurator implements nl.knmi.adaguc.config.ConfiguratorInterface {
	
//	private static boolean configDone = false;
//	
//	@Override 
//	public void setConfigDone() {
//		configDone =true;
//	}
	
	private static String trustStorePassword=null;
	private static String trustStore=null;
	private static String trustRootsCADirectory=null;
	private static String keyStore=null;
	private static String keyStorePassword=null;
	private static String keyStoreType="JKS";
	private static String keyAlias="tomcat";
	private static String caCertificate = null;
	private static String caPrivateKey = null;		
	
	static ConfigurationReader configurationReader = new ConfigurationReader ();
	
	@Override
	public void doConfig(XMLElement  configReader){
		
		if(configReader.getNodeValue ("adaguc-services.security")==null){
			Debug.println("adaguc-services.security is not configured");
			return;
		}
		trustStorePassword=configReader.getNodeValue("adaguc-services.security.truststorepassword");
		trustStore=configReader.getNodeValue("adaguc-services.security.truststore");
		trustRootsCADirectory=configReader.getNodeValue("adaguc-services.security.trustrootscadirectory");
		keyStore=configReader.getNodeValue("adaguc-services.security.keystore");
		keyStorePassword=configReader.getNodeValue("adaguc-services.security.keystorepassword");
		keyStoreType=configReader.getNodeValue("adaguc-services.security.keystoretype");
		keyAlias=configReader.getNodeValue("adaguc-services.security.keyalias");
		if (configReader.getNodeValue("adaguc-services.security.tokenapi")!=null){
			caCertificate=configReader.getNodeValue("adaguc-services.security.tokenapi.cacertificate");
			caPrivateKey=configReader.getNodeValue("adaguc-services.security.tokenapi.caprivatekey");
		} else {
			Debug.println("tokenapi is not enabled");
		}
		
	
	}
	
	public static String getCACertificate() throws ElementNotFoundException {
		configurationReader.readConfig();
		return caCertificate;
	}
	public static String getCAPrivateKey() throws ElementNotFoundException {
		configurationReader.readConfig();
		return caPrivateKey;
	}
	public static String getTrustStorePassword() throws ElementNotFoundException {
		configurationReader.readConfig();
		return trustStorePassword;
	}
	public static String getTrustStore() throws ElementNotFoundException {
		configurationReader.readConfig();
		return trustStore;
	}
	public static String getTrustRootsCADirectory() throws ElementNotFoundException {
		configurationReader.readConfig();
		return trustRootsCADirectory;
	}

	public static Object getKeyStore() throws ElementNotFoundException {
		configurationReader.readConfig();
		return keyStore;
	}

	public static Object getKeyStorePassword() throws ElementNotFoundException {
		configurationReader.readConfig();
		return keyStorePassword;
	}

	public static Object getKeyStoreType() throws ElementNotFoundException {
		configurationReader.readConfig();
		return keyStoreType;
	}

	public static Object getKeyAlias() throws ElementNotFoundException {
		configurationReader.readConfig();
		return keyAlias;
	}
}


