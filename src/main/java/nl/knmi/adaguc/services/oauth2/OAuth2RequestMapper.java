package nl.knmi.adaguc.services.oauth2;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.operator.OperatorCreationException;
import org.ietf.jgss.GSSException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.security.CertificateVerificationException;
import nl.knmi.adaguc.security.SecurityConfigurator;
import nl.knmi.adaguc.security.SecurityConfigurator.ComputeNode;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.HTTPTools;
import nl.knmi.adaguc.tools.JSONResponse;


@RestController
public class OAuth2RequestMapper {
	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
		MappingJackson2HttpMessageConverter converter = 
				new MappingJackson2HttpMessageConverter(mapper);
		return converter;
	}
	
	@CrossOrigin
	@ResponseBody
	@RequestMapping(
			path="oauth",
			method = RequestMethod.GET 
		)
	public void doOauth(HttpServletResponse response, HttpServletRequest request) throws JSONException, IOException, ElementNotFoundException{
//		boolean useDev = false; // TODO
//		if (useDev) {
//			request.getSession().setAttribute("user_identifier","maarten");
//			Vector<ComputeNode> computeNodes = SecurityConfigurator.getComputeNodes();
//		    request.getSession().setAttribute("domain",computeNodes.get(0).url.replace("https://", ""));
//		    try {
//				OAuth2Handler.makeUserCertificate("maarten");// TODO
//			} catch (InvalidKeyException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			} catch (KeyManagementException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			} catch (UnrecoverableKeyException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			} catch (CertificateException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			} catch (NoSuchAlgorithmException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			} catch (OperatorCreationException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			} catch (KeyStoreException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			} catch (NoSuchProviderException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			} catch (SignatureException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			} catch (GSSException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			} catch (CertificateVerificationException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//;			try {
//				response.sendRedirect(HTTPTools.getHTTPParam(request, "returnurl"));
//			} catch (Exception e) {
//				e.printStackTrace();
//				response.sendRedirect(MainServicesConfigurator.getServerExternalURL());
//			}
//			return;
//		} 
//	
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
