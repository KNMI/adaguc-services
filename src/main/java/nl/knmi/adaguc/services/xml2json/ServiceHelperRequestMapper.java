package nl.knmi.adaguc.services.xml2json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.security.PemX509Tools;
import nl.knmi.adaguc.security.SecurityConfigurator;
import nl.knmi.adaguc.services.adagucserver.ADAGUCServer;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.MyXMLParser;


@RestController
public class ServiceHelperRequestMapper {
	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
		MappingJackson2HttpMessageConverter converter = 
				new MappingJackson2HttpMessageConverter(mapper);
		return converter;
	}
	@ResponseBody
	@RequestMapping("xml2json")
	public void XML2JSON(@RequestParam(value="request")String request,@RequestParam(value="callback", required=false)String callback, HttpServletResponse response){
		/**
		 * Converts XML file pointed with request to JSON file
		 * @param requestStr
		 * @param out1
		 * @param response
		 */

		String requestStr;
		OutputStream out;
		try {
			out = response.getOutputStream();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return;
		}
		try {
			requestStr=URLDecoder.decode(request,"UTF-8");
			MyXMLParser.XMLElement rootElement = new MyXMLParser.XMLElement();
			//Remote XML2JSON request to external WMS service
			System.err.println("Converting XML to JSON for "+requestStr);

			boolean isLocal = false;
			
			if(requestStr.startsWith(MainServicesConfigurator.getServerExternalURL())){
				Debug.println("Running local adaguc for ["+requestStr+"]");
				isLocal = true;
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				String url = requestStr.substring(MainServicesConfigurator.getServerExternalURL().length());
				url = url.substring(url.indexOf("?")+1);
				Debug.println("url = ["+url+"]");
				ADAGUCServer.runADAGUCWMS(null, null, url, outputStream);
				String getCapabilities = new String(outputStream.toByteArray());
				outputStream.close();
				rootElement.parseString(getCapabilities);
			}

			if(isLocal == false){
				String ts = null;
				if(requestStr.startsWith("https://")){
					ts = SecurityConfigurator.getTrustStore();
				}
				if(ts!=null ){
					char [] tsPass = SecurityConfigurator.getTrustStorePassword().toCharArray();
					
					Debug.println("Running remote adaguc with truststore");

					CloseableHttpClient httpClient = (new PemX509Tools()).
							getHTTPClientForPEMBasedClientAuth(ts, tsPass, null);
					CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(requestStr));
					String result = EntityUtils.toString(httpResponse.getEntity());
					rootElement.parseString(result);
				}else{
					Debug.println("Running remote adaguc without truststore");

					rootElement.parse(new URL(requestStr));
				}
			}
			if (callback==null) {
				response.setContentType("application/json");
				out.write(rootElement.toJSON(null).getBytes());
			} else {
				response.setContentType("application/javascript");
				out.write(callback.getBytes());
				out.write("(".getBytes());
				out.write(rootElement.toJSON(null).getBytes());
				out.write(");".getBytes());

			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				if (callback==null) {
					response.setContentType("application/json");
					out.write("{\"error\":\"error\"}".getBytes());
				} else {
					response.setContentType("application/javascript");
					out.write(callback.getBytes());
					out.write("(".getBytes());
					out.write("{\"error\":\"error\"}".getBytes());
					out.write(");".getBytes());

				}
			}catch (Exception e1) {
				response.setStatus(500);
			}
		}
	}



}

