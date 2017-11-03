package nl.knmi.adaguc.services.pywpsserver;

import java.io.IOException;

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
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.JSONResponse;

@RestController
public class PyWPSRequestMapper {
	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
		MappingJackson2HttpMessageConverter converter = 
				new MappingJackson2HttpMessageConverter(mapper);
		return converter;
	}
	@ResponseBody
	@RequestMapping("wps")
	public void PyWPSServer(HttpServletResponse response, HttpServletRequest request) throws IOException{

		try {
			PyWPSServer.runPyWPS(request,response,null,null);
		} catch (AuthenticationException e) {
			Debug.printStackTrace(e);
				new JSONResponse(request).setErrorMessage("Authentication error", 401).print(response);
		} catch (IOException e) {
			new JSONResponse(request).setException("IOException",e).print(response);
		} catch (ElementNotFoundException e) {
			new JSONResponse(request).setException("ElementNotFoundException",e).print(response);
		} catch (InterruptedException e) {
			new JSONResponse(request).setException("InterruptedException",e).print(response);
		}

	}
}
