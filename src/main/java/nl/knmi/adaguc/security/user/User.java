package nl.knmi.adaguc.security.user;

import java.io.IOException;

import lombok.Getter;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;

public class User {
	@Getter
	String homeDir = null;
	
	@Getter
	String userId = null;
	
	@Getter
	String dataDir = null;
	  
	  
	public static String makePosixUserId(String userId){
	    if (userId == null)
	      return null;

	    userId = userId.replace("http://", "");
	    userId = userId.replace("https://", "");
	    userId = userId.replaceAll("/", ".");
	    return userId;
	  }


	public User(String _id) throws IOException, ElementNotFoundException {
		Debug.println("New user ID is made :["+_id+"]");
		String userWorkspace = MainServicesConfigurator.getUserWorkspace();
		userId = makePosixUserId(_id);
		homeDir=userWorkspace+"/"+userId;
		dataDir = homeDir+"/data";
		Tools.mksubdirs(homeDir);
		Tools.mksubdirs(dataDir);
		Debug.println("User Home Dir: "+homeDir);
	}


	
}
