package nl.knmi.adaguc.services.pywpsserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
	@RequestMapping("pywpsserver")
	public void PyWPSServer(HttpServletResponse response, HttpServletRequest request){

		try {
			PyWPSServer.runPyWPS(request,response,null,null);
		} catch (Exception e) {
			JSONResponse jsonResponse = new JSONResponse(request);
			jsonResponse.setException("PyWPS request failed",e);
			try {
				jsonResponse.print(response);
			} catch (Exception e1) {

			}
		}

	}
}
