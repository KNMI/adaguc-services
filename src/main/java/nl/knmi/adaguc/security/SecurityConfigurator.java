package nl.knmi.adaguc.security;

import java.io.IOException;
import java.util.Vector;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.Synchronized;
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
	@Autowired
	static ConfigurationReader configurationReader;
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
	private static String userHeader=null;	
	private static String caCertificate = null;
	private static String caPrivateKey = null;		
	private static String enableSSL = null;
	private static String user = null;
	
	public static class ComputeNode {
		public String url = null;
		public String name = null;
	};

	static Vector<ComputeNode> computeNodes = new Vector<ComputeNode>();
	


	@Synchronized
	public static void doConfig(XMLElement  configReader){
		
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
		computeNodes.clear();
		keyAlias=configReader.getNodeValue("adaguc-services.security.keyalias");
		userHeader=configReader.getNodeValue("adaguc-services.security.userheader");
		enableSSL=configReader.getNodeValue("adaguc-services.security.enablessl");
		user=configReader.getNodeValue("adaguc-services.security.user");
		
		if (configReader.getNodeValue("adaguc-services.security.tokenapi")!=null){
			caCertificate=configReader.getNodeValue("adaguc-services.security.tokenapi.cacertificate");
			caPrivateKey=configReader.getNodeValue("adaguc-services.security.tokenapi.caprivatekey");
			if (configReader.getNodeValue("adaguc-services.security.tokenapi.remote-instances")!=null){
				try {
					Vector<XMLElement> computeNodeElements = configReader.get("adaguc-services").get("security").get("tokenapi").get("remote-instances").getList("adaguc-service");
					for(int j=0;j<computeNodeElements.size();j++){
						XMLElement computeNodeElement = computeNodeElements.get(j);

						try {
							ComputeNode computeNode = new ComputeNode();
							computeNode.url = computeNodeElement.getValue();
							computeNode.name = computeNodeElement.getAttrValue("name");
//							Debug.println("Added remote instance " + computeNode.url + " with name " + computeNode.name);
							computeNodes.add(computeNode);
						} catch (Exception e) {
							Debug.printStackTrace(e);
						}
					}
				} catch (Exception e) {
					
					e.printStackTrace();
				}
			} else {
				Debug.println("No remote instances configured");
			}

		} 

	}

	public static Vector<ComputeNode> getComputeNodes() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return computeNodes;
	}
	public static String getCACertificate() throws ElementNotFoundException, IOException {
		Debug.println("getCACertificate");
		ConfigurationReader.readConfig();
		Debug.println("getCACertificate="+caCertificate);
		return caCertificate;
	}
	public static String getCAPrivateKey() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return caPrivateKey;
	}
	public static String getTrustStorePassword() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return trustStorePassword;
	}
	public static String getTrustStore() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return trustStore;
	}
	public static String getTrustRootsCADirectory() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return trustRootsCADirectory;
	}

	public static Object getKeyStore() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return keyStore;
	}

	public static Object getKeyStorePassword() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return keyStorePassword;
	}

	public static Object getKeyStoreType() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return keyStoreType;
	}

	public static Object getKeyAlias() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return keyAlias;
	}

	public static String getUserHeader() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return userHeader;
	}
	public static String getEnableSSL() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return enableSSL;
	}
	public static String getUser() throws ElementNotFoundException, IOException {
		return user;
	}
}


