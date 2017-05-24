package nl.knmi.adaguc.usermanagement;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nl.knmi.adaguc.config.ConfigurationItemNotFoundException;
import nl.knmi.adaguc.security.AuthenticatorInterface;



public class UserManager {
	private static Map<String, User> users = new ConcurrentHashMap<String,User>();
	  
	public synchronized static User getUser(String id) throws IOException, ConfigurationItemNotFoundException{
		id = harmonizeUserId(id);
		User user = users.get(id);
		if(user == null){
			users.put(id, new User(id));
			return getUser(id);
		}
		return user;
	}

	private static String harmonizeUserId(String id) {
		return id;
	}

	public synchronized static User getUser(AuthenticatorInterface authenticator) throws IOException, ConfigurationItemNotFoundException {
		return getUser(authenticator.getClientId());
	}
}
