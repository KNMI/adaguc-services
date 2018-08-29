package nl.knmi.adaguc.services.servicehealth;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.services.adagucserver.ADAGUCServer;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.HTTPTools;
import nl.knmi.adaguc.tools.JSONResponse;
import nl.knmi.adaguc.tools.Tools;

@RestController
public class ServiceHealthRequestMapper {
	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
		MappingJackson2HttpMessageConverter converter = 
				new MappingJackson2HttpMessageConverter(mapper);
		return converter;
	}

	@ResponseBody
	@RequestMapping("servicehealth")
	public void serviceHealth(HttpServletResponse response, HttpServletRequest request) throws IOException{
		JSONResponse jsonResponse = new JSONResponse(request);
		try {
			boolean enabled = ServiceHealthConfigurator.getEnabled();
			if(!enabled){
				jsonResponse.setMessage(new JSONObject().put("error","ADAGUC Service Health is not enabled"));
			}else{
				String basePath =  new File(ServiceHealthConfigurator.getServiceHealthDirectory()).getCanonicalPath();
				String[] fileList = Tools.ls(basePath);
				JSONObject statusList = new JSONObject();
				for (String service : fileList) {
					String status = Tools.readFile(basePath + "/" + service).trim();
					String[] contents = status.split("\\r?\\n");
					if (contents[0].startsWith("0")) {
						statusList.put(service, new JSONObject().put("statusCode", contents[0]).put("ok", true));
					} else {
						JSONObject serviceResponse = new JSONObject();
						serviceResponse.put("statusCode", contents[0])
						.put("ok", false);
						if (contents.length > 1)
							serviceResponse.put("message", contents[1]);
						statusList.put(service, serviceResponse);

					}
				}
				jsonResponse.setMessage(statusList);	
			}
		} catch (Exception e) {
			e.printStackTrace();
			jsonResponse.setException("error: "+e.getMessage(), e);
		}
		jsonResponse.print(response);

	}
}
