package nl.knmi.adaguc.services.tokenapi;

import java.io.IOException;
import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.security.AuthenticatorFactory;
import nl.knmi.adaguc.security.AuthenticatorInterface;
import nl.knmi.adaguc.security.token.Token;
import nl.knmi.adaguc.security.token.TokenManager;
import nl.knmi.adaguc.security.user.User;
import nl.knmi.adaguc.security.user.UserManager;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.JSONResponse;


@RestController
public class TokenApiRequestMapper {
	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
		MappingJackson2HttpMessageConverter converter = 
				new MappingJackson2HttpMessageConverter(mapper);
		return converter;
	}
	@ResponseBody
	@RequestMapping("registertoken")
	public void registerToken(HttpServletRequest request, HttpServletResponse response) throws IOException{
		Debug.println("registerToken");
		JSONResponse jsonResponse = new JSONResponse(request);

		AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);
		User user = null;
		try {
			user = UserManager.getUser(authenticator);
		} catch (AuthenticationException e1) {
			Debug.printStackTrace(e1);
			jsonResponse.setErrorMessage("Authentication error", 401);
		} catch (ElementNotFoundException e1) {
			Debug.printStackTrace(e1);
			jsonResponse.setErrorMessage("Authentication error", 401);
		}
		if (user == null) {
			Debug.println("No user found.");
		}
		if (user != null) {
			try {
				Token token = TokenManager.registerToken(user);
				ObjectMapper om=new ObjectMapper();
				jsonResponse.setMessage(om.writeValueAsString(token));
			} catch (AuthenticationException e) {
				Debug.printStackTrace(e);
				jsonResponse.setErrorMessage("Authentication error", 401);
			} catch (ElementNotFoundException e) {
				jsonResponse.setException("ElementNotFoundException",e);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		try {
			jsonResponse.print(response);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}



}

