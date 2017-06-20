package nl.knmi.adaguc.services.joblist;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;

import nl.knmi.adaguc.config.ConfigurationItemNotFoundException;
import nl.knmi.adaguc.security.AuthenticatorFactory;
import nl.knmi.adaguc.security.AuthenticatorInterface;
import nl.knmi.adaguc.security.user.UserManager;
import nl.knmi.adaguc.services.basket.BasketConfigurator;
import nl.knmi.adaguc.services.pywpsserver.PyWPSConfigurator;
import nl.knmi.adaguc.services.pywpsserver.PyWPSServer;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.HTTPTools;
import nl.knmi.adaguc.tools.InvalidTokenException;
import nl.knmi.adaguc.tools.JSONResponse;
import nl.knmi.adaguc.tools.MyXMLParser;
import nl.knmi.adaguc.tools.MyXMLParser.Options;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.adaguc.tools.HTTPTools.InvalidHTTPKeyValueTokensException;

@RestController
@RequestMapping("joblist")
@CrossOrigin
public class JobListRequestMapper {
	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
		MappingJackson2HttpMessageConverter converter = 
				new MappingJackson2HttpMessageConverter(mapper);
		return converter;
	}

	private static JSONObject NewStatusLocation(String queryString, String statusLocation) throws InvalidTokenException, ConfigurationItemNotFoundException, InvalidHTTPKeyValueTokensException, IOException  {
		//  Check for status location first.
		Debug.println("Checking ["+statusLocation+"]");
		if (statusLocation!=null){ 
			String portalOutputPath = PyWPSConfigurator.getPyWPSOutputDir();
			String output=statusLocation.substring(statusLocation.lastIndexOf("/"));
			//Remove first "/" token;
			output = output.substring(1);
			portalOutputPath = Tools.makeCleanPath(portalOutputPath);
			String fileName = portalOutputPath+"/"+output;
			Debug.println("NewStatusLocation request: "+fileName);

			Tools.checkValidCharsForFile(output);
			String data = Tools.readFile(fileName);
			JSONObject statusJson=null;
			try {
				statusJson=PyWPSServer.xmlStatusToJSONStatus(queryString, data);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return statusJson;

		}
		Debug.println("No Output found");
		return null;

	}

	@ResponseBody
	@RequestMapping("/remove")
	public void removeFromJobList(HttpServletResponse response, HttpServletRequest request, @RequestParam("job") String job) throws IOException{
		JSONResponse jsonResponse = new JSONResponse(request);
		try {
			boolean enabled = JobListConfigurator.getEnabled();
			if(!enabled){
				jsonResponse.setMessage(new JSONObject().put("error","ADAGUC joblist is not enabled"));
			}else{
				Debug.println("removeFromJobList()");
				if (job!=null) {
					AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);
					String userDataDir = UserManager.getUser(authenticator).getDataDir();
					String cleanPath=Tools.makeCleanPath(job);
					String wpsSettingsName=cleanPath.replace(".xml", ".wpssettings");
					File f=new File(userDataDir+"/WPSSettings/"+wpsSettingsName);
					Debug.println("removing:"+ f.getPath());
					f.delete();
					jsonResponse.setMessage(new JSONObject().put("message", "jobfile deleted"));
				} else {
					jsonResponse.setErrorMessage("job parameter missing", 200);
				}
			}
		} catch (Exception e) {
			jsonResponse.setException("error: "+e.getMessage(), e);
		}
		jsonResponse.print(response);
	}

	@ResponseBody
	@RequestMapping("/list")
	public void listJobs(HttpServletResponse response, HttpServletRequest request) throws IOException{
		JSONResponse jsonResponse = new JSONResponse(request);
		ObjectMapper om=new ObjectMapper();
		om.registerModule(new JSR310Module());
		om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		//List all jobfiles in WPS_Settings
		Debug.println("/joblist/list");
		JSONObject jobs=new JSONObject();
		JSONArray jobArray=new JSONArray();
		try {
			jobs.put("jobs", jobArray);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);

			String userDataDir = UserManager.getUser(authenticator).getDataDir();
			String dir=userDataDir+"/WPS_Settings";
			File d=new File(dir);
			//			Debug.println(dir+" "+d.isDirectory());
			if (d.isDirectory()) {
				String[] filesIndir=d.list();
				for (String fn: filesIndir){
					File f=new File(dir+"/"+fn);
					//					Debug.println("fn:"+fn+" "+f.isFile()+" ; "+fn.endsWith("settings"));
					if (f.isFile()&&fn.endsWith("settings")){
						String fileString=new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())));
						Debug.println("found: "+fn+" "+fileString.length());
						JSONObject job=new JSONObject(fileString);
						//if status==Accepted

						String status=null;
						try {
							status=job.getString("wpsstatus");
						}catch (JSONException e){}
						if (status!=null) {
							if (status.equalsIgnoreCase("PROCESSACCEPTED")||
									status.equalsIgnoreCase("PROCESSSTARTED")||	
									status.equalsIgnoreCase("PROCESSPAUSED")){
								Debug.println(fn+": with status "+status+" let's look again");
								String statusLocation=job.getString("statuslocation");
								JSONObject newJobStatus=NewStatusLocation(URLDecoder.decode(job.getString("querystring"), "utf-8"), statusLocation);
								Debug.println("newJobStatus: "+newJobStatus.toString());
								Debug.println("st:"+newJobStatus.getString("wpsstatus")+ "<==="+status);
								if ((newJobStatus!=null)&&!status.equals(newJobStatus.getString("wpsstatus"))) {

									//  		  Tools.mksubdirs(userDataDir+"/WPS_Settings/");
									String baseName = statusLocation.substring(statusLocation.lastIndexOf("/")).replace(".xml", ".wpssettings");
									String wpsSettingsFile = userDataDir+"/WPS_Settings/";
									Tools.mksubdirs(wpsSettingsFile);
									wpsSettingsFile+=baseName;
									Tools.writeFile(wpsSettingsFile, newJobStatus.toString());
									Debug.println("re-written "+wpsSettingsFile);
									job=newJobStatus;
								}								

							} else {
								Debug.println(fn+": finished");
							}

						}
						Debug.println(job.toString());
						jobArray.put(job);
					}
				}
			}

		} catch (Exception e) {
			jsonResponse.setException("error: "+e.getMessage(), e);
		}
		jsonResponse.setMessage(jobs);
		jsonResponse.print(response);
	}	
}
