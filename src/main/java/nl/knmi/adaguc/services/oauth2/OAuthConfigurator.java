package nl.knmi.adaguc.services.oauth2;

import java.io.IOException;
import java.util.Vector;

import org.springframework.beans.factory.annotation.Autowired;

import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.config.ConfigurationReader;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;



public class OAuthConfigurator implements nl.knmi.adaguc.config.ConfiguratorInterface {
	@Autowired
	static ConfigurationReader configurationReader;
	public static class Oauth2Settings{
		//  For building the web page:
		public String description = null;
		public String logo = null;
		public String registerlink= null;

		//For server requests:
		public String OAuthAuthLoc = null;
		public String OAuthTokenLoc = null;
		public String OAuthClientId = null;
		public String OAuthClientScope = null;

		//Secret thing
		public String OAuthClientSecret = null;



		public String id = null;
		public String oauthCallbackURL = null;
		public String getConfig() {
			String config = "OAuthAuthLoc: "+OAuthAuthLoc+"\n";
			config += "OAuthTokenLoc: "+OAuthTokenLoc+"\n";
			config += "OAuthClientId: "+OAuthClientId+"\n";
			config += "OAuthClientScope: "+OAuthClientScope+"\n";
			return config;
		}
	}

	static Vector<Oauth2Settings> oauth2Providers = new Vector<Oauth2Settings>();

	private static Oauth2Settings _getOauthSetting(String id){
		Debug.println("oauth2Providers.size = " + oauth2Providers.size());
		for(int j=0;j<oauth2Providers.size();j++){
			Debug.println("Iterating "+oauth2Providers.get(j).id);
			if(oauth2Providers.get(j).id.equals(id))return oauth2Providers.get(j);
		}
		return null;
	}

	private static Vector<String> _getProviders(){
		Vector<String> providers = new Vector<String>();
		for(int j=0;j<oauth2Providers.size();j++){
			providers.add(oauth2Providers.get(j).id);
		}
		return providers;
	}


	public static void doConfig(XMLElement  configReader){
		synchronized(oauth2Providers){
			oauth2Providers.clear();
			try {
				configReader.get("adaguc-services").get("oauth2") ;
			} catch (Exception e2) {
				Debug.println("adaguc-services.oauth2 not configured");
				return;
			}


			Vector<XMLElement> providers = null;
			try {
				providers = configReader.get("adaguc-services").get("oauth2").getList("provider");
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			if(providers == null){
				Debug.errprintln("No Oauth2 providers configured");
				return;
			}
			for(int j=0;j<providers.size();j++){
				XMLElement provider = providers.get(j);

				try {
					Oauth2Settings oauthSetting = new Oauth2Settings();
					oauthSetting.id = provider.getAttrValue("name");
					oauthSetting.OAuthAuthLoc = provider.get("authloc").getValue();
					oauthSetting.OAuthTokenLoc = provider.get("tokenloc").getValue();
					oauthSetting.OAuthClientId = provider.get("clientid").getValue();
					oauthSetting.OAuthClientSecret = provider.get("clientsecret").getValue();
					oauthSetting.OAuthClientScope = provider.get("scope").getValue();

					try{oauthSetting.description = provider.get("description").getValue();}catch(Exception e){}
					try{oauthSetting.logo = provider.get("logo").getValue();}catch(Exception e){}
					try{oauthSetting.registerlink = provider.get("registerlink").getValue();}catch(Exception e){}
					try{oauthSetting.oauthCallbackURL = provider.get("oauthcallbackurl").getValue();}catch(Exception e){}

					oauth2Providers.add(oauthSetting);
					//Debug.println(j+") Found Oauth2 provider "+oauthSetting.id);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static Oauth2Settings getOAuthSettings(String id) throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();
		return _getOauthSetting(id);
	}

	public static Vector<String> getProviders() throws ElementNotFoundException, IOException {
		ConfigurationReader.readConfig();

		return _getProviders();
	}
}