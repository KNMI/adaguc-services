package nl.knmi.adaguc.security.user;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.AuthenticationException;

import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.security.AuthenticationExceptionImpl;
import nl.knmi.adaguc.security.AuthenticatorInterface;



public class UserManager {
	private static Map<String, User> users = new ConcurrentHashMap<String,User>();
	  
	public synchronized static User getUser(String id) throws IOException, ElementNotFoundException, AuthenticationException{
		if(id == null){
			throw new AuthenticationExceptionImpl("No user information provided");
		}
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

	public synchronized static User getUser(AuthenticatorInterface authenticator) throws IOException, ElementNotFoundException, AuthenticationException {
		return getUser(authenticator.getClientId());
	}
}
