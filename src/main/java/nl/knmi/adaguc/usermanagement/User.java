package nl.knmi.adaguc.usermanagement;

import java.io.IOException;

import lombok.Getter;
import nl.knmi.adaguc.config.ConfigurationItemNotFoundException;
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


	public User(String id) throws IOException, ConfigurationItemNotFoundException {
		Debug.println("New user ID is made :["+id+"]");
		String userWorkspace = MainServicesConfigurator.getUserWorkspace();
		userId = id;
		homeDir=userWorkspace+"/"+makePosixUserId(id);
		dataDir = homeDir+"/data";
		Tools.mksubdirs(homeDir);
		Tools.mksubdirs(dataDir);
		Debug.println("User Home Dir: "+homeDir);
	}


	
}
