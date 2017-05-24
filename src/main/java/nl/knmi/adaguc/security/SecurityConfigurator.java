package nl.knmi.adaguc.security;

import nl.knmi.adaguc.config.ConfigurationItemNotFoundException;
import nl.knmi.adaguc.config.ConfigurationReader;
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
	private static String trustStorePassword=null;
	private static String trustStore=null;
	private static String trustRootsCADirectory=null;
	private static String keyStore=null;
	private static String keyStorePassword=null;
	private static String keyStoreType="JKS";
	private static String keyAlias="tomcat";
	public void doConfig(XMLElement  configReader){
		trustStorePassword=configReader.getNodeValue("adaguc-services.security.truststorepassword");
		trustStore=configReader.getNodeValue("adaguc-services.security.truststore");
		trustRootsCADirectory=configReader.getNodeValue("adaguc-services.security.trustrootscadirectory");
		keyStore=configReader.getNodeValue("adaguc-services.security.keystore");
		keyStorePassword=configReader.getNodeValue("adaguc-services.security.keystorepassword");
		keyStoreType=configReader.getNodeValue("adaguc-services.security.keystoretype");
		keyAlias=configReader.getNodeValue("adaguc-services.security.keyalias");
	}

	public static String getTrustStorePassword() throws ConfigurationItemNotFoundException {
		ConfigurationReader.readConfig();
		return trustStorePassword;
	}
	public static String getTrustStore() throws ConfigurationItemNotFoundException {
		ConfigurationReader.readConfig();
		return trustStore;
	}
	public static String getTrustRootsCADirectory() throws ConfigurationItemNotFoundException {
		ConfigurationReader.readConfig();
		return trustRootsCADirectory;
	}

	public static Object getKeyStore() throws ConfigurationItemNotFoundException {
		ConfigurationReader.readConfig();
		return keyStore;
	}

	public static Object getKeyStorePassword() throws ConfigurationItemNotFoundException {
		ConfigurationReader.readConfig();
		return keyStorePassword;
	}

	public static Object getKeyStoreType() throws ConfigurationItemNotFoundException {
		ConfigurationReader.readConfig();
		return keyStoreType;
	}

	public static Object getKeyAlias() throws ConfigurationItemNotFoundException {
		ConfigurationReader.readConfig();
		return keyAlias;
	}
}