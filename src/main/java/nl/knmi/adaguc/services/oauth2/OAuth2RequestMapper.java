package nl.knmi.adaguc.services.oauth2;

import java.io.IOException;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.security.SecurityConfigurator;
import nl.knmi.adaguc.security.SecurityConfigurator.ComputeNode;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.JSONResponse;


@RestController
public class OAuth2RequestMapper {

	@CrossOrigin
	@ResponseBody
	@RequestMapping(
			path="oauth",
			method = RequestMethod.GET 
		)
	public void doOauth(HttpServletResponse response, HttpServletRequest request) throws JSONException, IOException, ElementNotFoundException{
		OAuth2Handler.doGet(request, response);
	}
	/**
	 * Small function to check if the Id is unknown.
	 * @param id
	 * @return
	 */
	private boolean isIdUnknown(String id) {
		return id == null || id.length() == 0;
	}
	
	@CrossOrigin
	@RequestMapping(
			path="getid",
			method = RequestMethod.GET 
		)
	public void getId(HttpServletResponse response, HttpServletRequest request) throws JSONException, IOException, ElementNotFoundException{
		JSONResponse jsonResponse = new JSONResponse(request);
		Debug.println("getid");
		try{
			String userName = SecurityConfigurator.getUser();
			if (userName != null) {
				Debug.println("Setting user to " + userName);
				request.getSession().setAttribute("user_identifier",userName);
				request.getSession().setAttribute("services_access_token",userName);
				request.getSession().setAttribute("emailaddress",userName);
			}
		}catch(Exception e){
			Debug.println("No user name");
		}


		String id = (String) request.getSession().getAttribute("user_identifier");
		String servicesAccessToken = (String) request.getSession().getAttribute("services_access_token");
		String emailAddress = (String) request.getSession().getAttribute("emailaddress");

		JSONObject jsonObj = new JSONObject();
		
		if(isIdUnknown(id)){
			jsonObj.put("error","Not signed in (77)");
		}else{
			jsonObj.put("id",id);
		}
		
		if(servicesAccessToken == null || servicesAccessToken.length() == 0
				|| isIdUnknown(id)){
			jsonObj.put("services_access_token","undefined");
		}else{
			jsonObj.put("services_access_token",servicesAccessToken);
		}
		
		if(emailAddress == null || emailAddress.length() == 0
				|| isIdUnknown(id)){
			jsonObj.put("email_address","undefined");
		}else{
			jsonObj.put("email_address",emailAddress);
		}
		
		jsonObj.put("backend",MainServicesConfigurator.getServerExternalURL());
		Vector<ComputeNode> computeNodes = SecurityConfigurator.getComputeNodes();
		JSONArray jsonArray = new JSONArray();
		for (final ComputeNode computeNode : computeNodes) {
			jsonArray.put(new JSONObject().put("url", computeNode.url).put("name", computeNode.name));
		}
		jsonObj.put("compute", jsonArray);
		
		jsonResponse.setMessage(jsonObj);
		jsonResponse.print(response);

	}
	
	@CrossOrigin
	@RequestMapping(
			path="logout",
			method = RequestMethod.GET 
		)
	public void doLogout(HttpServletResponse response, HttpServletRequest request) throws JSONException, IOException, ElementNotFoundException{
		JSONResponse jsonResponse = new JSONResponse(request);
		
		String id = (String) request.getSession().getAttribute("user_identifier");
		
		if(id !=null){
			request.getSession().setAttribute("user_identifier",null);
			jsonResponse.setMessage(new JSONObject().put("message","ok logged out"));
		}else{
			jsonResponse.setMessage(new JSONObject().put("message","already logged out"));
		}
		
		jsonResponse.print(response);

	}

}
