package nl.knmi.adaguc.security.token;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.security.core.AuthenticationException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.security.AuthenticationExceptionImpl;
import nl.knmi.adaguc.security.AuthenticatorInterface;
import nl.knmi.adaguc.security.user.User;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.LazyCaller;
import nl.knmi.adaguc.tools.Tools;



public class TokenManager {
	private static Map<String, Token> accesstokens = new ConcurrentHashMap<String,Token>();
	  
	public synchronized static Token getToken(String id) throws IOException, ElementNotFoundException, AuthenticationException{
		if(id == null){
			throw new AuthenticationExceptionImpl("No token information provided");
		}
		loadTokensFromStore();
		Token token = accesstokens.get(id);
		if(token == null){
			Debug.println("token not found "+id);
			throw new AuthenticationExceptionImpl("Token not found "+id);
		}
		return token;
	}

	public String getTokenFromPath(String path){
		Pattern pattern = Pattern.compile("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[4][0-9A-Fa-f]{3}-[89ABab][0-9A-Fa-f]{3}-[0-9A-Fa-f]{12}");
		Matcher matcher = pattern.matcher(path);
		if (matcher.find())	{
		    return matcher.group();
		}
		Debug.println("No access token set in PATH URL via .../accesstoken/...");
		return null;
	}
	public synchronized static Token registerToken(User user) throws IOException, ElementNotFoundException, AuthenticationException, ParseException{
		if(user == null)return null;
		String idOne = UUID.randomUUID().toString();
		Token token = new Token(idOne, user);
		accesstokens.put(idOne, token);
		Debug.println("Created token ["+idOne+"]");
		Debug.println("There are " + accesstokens.size() +" tokens.");
		saveTokensToStore();
		return token;	
	}
	
	static boolean tokenStoreIsLoaded = false;

	public static synchronized void saveTokensToStore() throws IOException, ElementNotFoundException{
		if(tokenStoreIsLoaded == false){
//			throw new IOException("Trying to save a store which was not yet loaded");
			loadTokensFromStore();
		}
		ObjectMapper om=new ObjectMapper();
		String tokenStoreStr = om.writeValueAsString(accesstokens);
		String tokenStoreDir = MainServicesConfigurator.getBaseDir()+"/tokenstore";
		Tools.mksubdirs(tokenStoreDir);
		Tools.writeFile(tokenStoreDir+"/tokenstore.json", tokenStoreStr);
	}
	
	public static synchronized void loadTokensFromStore()  throws  ElementNotFoundException{
		if(LazyCaller.getInstance().isCallable("tokenstore", 1000) == false){
			return;
		};
		tokenStoreIsLoaded = true;
		
		String tokenStoreStr = null;
		try {
			Tools.mksubdirs(MainServicesConfigurator.getBaseDir()+"/tokenstore/");
			tokenStoreStr = Tools.readFile(MainServicesConfigurator.getBaseDir()+"/tokenstore/tokenstore.json");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		ObjectMapper om=new ObjectMapper();
		TypeFactory factory = TypeFactory.defaultInstance();
		MapType type    = factory.constructMapType(ConcurrentHashMap.class, String.class, Token.class);
		try {
			accesstokens = om.readValue(tokenStoreStr, type );
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Debug.println("Loaded tokenstore: there are " + accesstokens.size() +" tokens.");		
	}
	
	public synchronized static Token getToken(AuthenticatorInterface authenticator) throws IOException, ElementNotFoundException, AuthenticationException {
		return getToken(authenticator.getClientId());
	}
}
