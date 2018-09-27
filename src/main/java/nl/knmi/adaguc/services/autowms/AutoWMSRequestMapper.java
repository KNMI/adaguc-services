package nl.knmi.adaguc.services.autowms;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.CrossOrigin;
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
public class AutoWMSRequestMapper {
	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
		MappingJackson2HttpMessageConverter converter = 
				new MappingJackson2HttpMessageConverter(mapper);
		return converter;
	}

	@ResponseBody
	@CrossOrigin
	@RequestMapping("autowms")
	public void autowms(HttpServletResponse response, HttpServletRequest request) throws IOException{
		JSONResponse jsonResponse = new JSONResponse(request);
		try {
			boolean enabled = AutoWMSConfigurator.getEnabled();
			if(!enabled){
				jsonResponse.setMessage(new JSONObject().put("error","ADAGUC autowms is not enabled"));
			}else{

				String reqType = null;
				try{
					reqType  = HTTPTools.getHTTPParam(request, "request");
				}catch(Exception e){					
				}

				if(reqType == null)	throw new Exception("Parameter request is missing, e.g. use ?request=getfiles&path=/");

				if(reqType.equals("getfiles")){
					String basePath =  new File(AutoWMSConfigurator.getAdagucAutoWMS()).getCanonicalPath();
					String dataSetPath = null;
					try{
						dataSetPath = AutoWMSConfigurator.getAdagucDataset();
					} catch(ElementNotFoundException e) {
					} 
					String path = null;
					try{
						path=HTTPTools.getHTTPParam(request, "path");
					}catch(Exception  e){
					}
					if(path == null || path.equals(""))path = "/";
					Debug.println("autowms getfiles for path [" + path + "]");
					String requestedFile = new File(basePath + "/" + path).getCanonicalPath();
					Debug.println(requestedFile);
					Debug.println(basePath);
					if(requestedFile.startsWith(basePath)){
						/* Add files from autowms folder */
						File directory = new File(requestedFile);
						JSONArray fileList = new JSONArray();
						if( directory.exists() ) {
							File[] files = directory.listFiles();
							for( File file : files){
								if(file.isDirectory() || 
										file.getName().endsWith(".png") || 
										file.getName().endsWith(".h5") || 
										file.getName().endsWith(".hdf5") ||
										file.getName().endsWith(".nc") || 
										file.getName().endsWith(".nc4") ||
										file.getName().endsWith(".geojson") 
										){
									String filePath = file.getAbsolutePath().substring(basePath.length()+1);
									fileList.put(
											new JSONObject().
											put("name", file.getName()).
											put("leaf", !file.isDirectory()).
											put("path", filePath).
											put("adaguc", MainServicesConfigurator.getServerExternalURL()+
													"/adagucserver?source=" + 
													URLEncoder.encode(filePath,"utf-8") + 
													"&")
											);
								}
							}
						}

						if (dataSetPath != null ) {
							/* Add adaguc dataset folder as option in browser */
							if (path.equals("/") ) {
								fileList.put(
										new JSONObject().
										put("name", "adaguc::datasets").
										put("leaf", false).
										put("path", "/adaguc::datasets")
										);										
							}
							/* Add datasets from dataset folder */
							if (path.equals("/adaguc::datasets")) {
								try{
									File datasetDir = new File(dataSetPath);
									if( datasetDir.exists() ) {
										File[] files = datasetDir.listFiles();
										files = (File[]) Arrays.stream(files).filter(
												file -> file.isFile() && file.getName().endsWith(".xml")
												).toArray(File[]::new);
										for( File file : files){
											String dataSetName = FilenameUtils.removeExtension(file.getName());
											String filePath = file.getAbsolutePath().substring(basePath.length()+1);
											fileList.put(
													new JSONObject().
													put("name", dataSetName).
													put("leaf", true).
													put("path", "/adaguc::datasets"+filePath).
													put("adaguc", MainServicesConfigurator.getServerExternalURL()+
															"/adagucserver?dataset=" + 
															URLEncoder.encode(dataSetName,"utf-8") + 
															"&")
													);
										}
									}
								}catch (Exception e){
									Debug.printStackTrace(e);
								}
							}
						}
						jsonResponse.setMessage(new JSONObject().
								put("xml2json",MainServicesConfigurator.getServerExternalURL()+"/xml2json?").
								put("adagucserver",MainServicesConfigurator.getServerExternalURL()+"/adagucserver?").
								put("result",fileList));	
					}else{
						throw new Exception("Trying to query files outside basepath");

					}

					//				}else if(reqType.equals("getlayers")){
					//					String file=HTTPTools.getHTTPParam(request, "path");
					//					if(file == null ) throw new Exception("path not set");
					//					jsonResponse.setMessage(new JSONObject().put("result",file));
					//					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					//					ADAGUCServer.runADAGUCWMS(null, null, "request=GetCapabilities&service=WMS&source="+URLEncoder.encode(file,"utf-8"), outputStream);
					//					String getCapabilities = new String(outputStream.toByteArray());
					//					outputStream.close();
					//					Debug.println(getCapabilities);
				}else{
					throw new Exception("Unknown request type");
				}
			}




		} catch (Exception e) {
			jsonResponse.setException("error: "+e.getMessage(), e);
		}
		jsonResponse.print(response);

	}
}
