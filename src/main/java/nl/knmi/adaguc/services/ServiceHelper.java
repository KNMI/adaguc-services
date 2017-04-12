package nl.knmi.adaguc.services;

import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import nl.knmi.adaguc.services.adagucserver.AdagucServer;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.HTTPTools;
import nl.knmi.adaguc.tools.JSONResponse;
import nl.knmi.adaguc.tools.MyXMLParser;


@RestController
public class ServiceHelper {
	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
		MappingJackson2HttpMessageConverter converter = 
				new MappingJackson2HttpMessageConverter(mapper);
		return converter;
	}
	@ResponseBody
	@RequestMapping("XML2JSON")
	public void XML2JSON(@RequestParam(value="request", required=false)String request,@RequestParam(value="callback", required=false)String callback, HttpServletResponse response, HttpServletRequest req){
		/**
		 * Converts XML file pointed with request to JSON file
		 * @param requestStr
		 * @param out1
		 * @param response
		 */
		JSONResponse jsonResponse = new JSONResponse(req);

		if(request == null){
			jsonResponse.setErrorMessage("Parameter request is missing", 400);
			try{
				jsonResponse.setMessage("bla");
			} catch(Exception e){
				jsonResponse.setException("nice error message",e);
			}

			try {
				jsonResponse.print(response);
			} catch (Exception e1) {

			}
		}

		Debug.println("XML2JSON "+request);


		try {
			String requestStr=URLDecoder.decode(HTTPTools.getHTTPParam(req, "request"), "UTF-8");
			MyXMLParser.XMLElement rootElement = new MyXMLParser.XMLElement();
			//Remote XML2JSON request to external WMS service
			Debug.println("Converting XML to JSON for "+requestStr);
			rootElement.parse(new URL(requestStr));

			jsonResponse.setMessage(new String(rootElement.toJSON(null).getBytes()));
		} catch (Exception e) {

			jsonResponse.setException("XML2JSON request failed",e);
		}
		try {
			jsonResponse.print(response);
		} catch (Exception e1) {

		}
	}
	
	@ResponseBody
	@RequestMapping("adagucserver")
	public void ADAGUCSERVER(HttpServletResponse response, HttpServletRequest request){
		
		try {
			AdagucServer.runADAGUCWMS(request,response,null,null);
		} catch (Exception e) {
			JSONResponse jsonResponse = new JSONResponse(request);
			jsonResponse.setException("ADAGUCServer request failed",e);
			try {
				jsonResponse.print(response);
			} catch (Exception e1) {

			}
		}
		 
	 }
		 
}

