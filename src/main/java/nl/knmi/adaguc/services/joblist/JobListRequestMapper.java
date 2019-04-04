package nl.knmi.adaguc.services.joblist;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;

import nl.knmi.adaguc.security.AuthenticatorFactory;
import nl.knmi.adaguc.security.AuthenticatorInterface;
import nl.knmi.adaguc.security.user.UserManager;
import nl.knmi.adaguc.services.pywpsserver.PyWPSServer;
import nl.knmi.adaguc.services.xml2json.ServiceHelperRequestMapper;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.HTTPTools;
import nl.knmi.adaguc.tools.HTTPTools.InvalidHTTPKeyValueTokensException;
import nl.knmi.adaguc.tools.InvalidTokenException;
import nl.knmi.adaguc.tools.JSONResponse;
import nl.knmi.adaguc.tools.Tools;

@SuppressWarnings("deprecation")
@RestController
@RequestMapping("joblist")
@CrossOrigin
public class JobListRequestMapper {

	private static JSONObject NewStatusLocation(String queryString, String statusLocation)
			throws InvalidTokenException, ElementNotFoundException, InvalidHTTPKeyValueTokensException, IOException {
		Debug.println("Checking [" + statusLocation + "]");
		if (statusLocation != null) {
			JSONObject statusJson = null;
			try {
				String data = HTTPTools.makeHTTPGetRequest(statusLocation);
				statusJson = PyWPSServer.statusLocationDataAsXMLToWPSStatusObject(queryString, data);
			} catch (Exception e) {
				Debug.errprintln("PyWPSServer.statusLocationDataAsXMLToWPSStatusObjec FAILED " + e.getMessage());
			}
			return statusJson;
		}
		Debug.println("No Output found");
		return null;

	}

	@ResponseBody
	@RequestMapping("/remove")
	public void removeFromJobList(HttpServletResponse response, HttpServletRequest request,
			@RequestParam("job") String job) throws IOException {
		JSONResponse jsonResponse = new JSONResponse(request);
		try {
			boolean enabled = JobListConfigurator.getEnabled();
			if (!enabled) {
				jsonResponse.setMessage(new JSONObject().put("error", "ADAGUC joblist is not enabled"));
			} else {
				Debug.println("removeFromJobList()");
				if (job != null) {
					AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);
					String userDataDir = UserManager.getUser(authenticator).getDataDir();
					String cleanPath = Tools.makeCleanPath(job);
					String wpsSettingsName = cleanPath.replace(".xml", ".ready.json");
					File f = new File(userDataDir + "/WPS_Settings/" + wpsSettingsName);
					f.delete();
					wpsSettingsName = cleanPath.replace(".xml", ".execute.json");
					f = new File(userDataDir + "/WPS_Settings/" + wpsSettingsName);
					Debug.println("removing:" + f.getPath());
					if (f.delete()) {
						jsonResponse.setMessage(new JSONObject().put("message", "jobfile deleted"));
					} else {
						jsonResponse.setErrorMessage("delete file failed for " + f.getName(), 200);
					}
				} else {
					jsonResponse.setErrorMessage("job parameter missing", 200);
				}
			}
		} catch (Exception e) {
			jsonResponse.setException("error: " + e.getMessage(), e);
		}
		jsonResponse.print(response);
	}

	public static void saveExecuteResponseToJob(String queryString, String executeResponse, HttpServletRequest request) throws Exception {
		boolean enabled = JobListConfigurator.getEnabled();
		if (!enabled) {
			throw new Exception("ADAGUC joblist is not enabled");
		}
		
		JSONObject data=PyWPSServer.statusLocationDataAsXMLToWPSStatusObject(queryString, executeResponse);
		if (data!=null) {

			//  		  Tools.mksubdirs(userDataDir+"/WPS_Settings/");
			String statusLocation=data.getString("statuslocation");
			String baseName = statusLocation.substring(statusLocation.lastIndexOf("/")).replace(".xml", ".execute.json");
			AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);
			String userDataDir = UserManager.getUser(authenticator).getDataDir();
			String wpsSettingsFile = userDataDir+"/WPS_Settings/";
			Tools.mksubdirs(wpsSettingsFile);
			wpsSettingsFile+=baseName;
			Tools.writeFile(wpsSettingsFile, data.toString());
		} else {
			Debug.println("Synchronous execution");
		}

	}

	@ResponseBody
	@RequestMapping("/list")
	public void listJobs(HttpServletResponse response, HttpServletRequest request) throws IOException {
		JSONResponse jsonResponse = new JSONResponse(request);
		ObjectMapper om = new ObjectMapper();
		om.registerModule(new JSR310Module());
		om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		// List all jobfiles in WPS_Settings
		Debug.println("/joblist/list");
		JSONObject jobs = new JSONObject();
		JSONArray jobArray = new JSONArray();
		try {
			jobs.put("jobs", jobArray);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);

			String userDataDir = UserManager.getUser(authenticator).getDataDir();
			String dir = userDataDir + "/WPS_Settings";
			File d = new File(dir);
			// Debug.println(dir+" "+d.isDirectory());
			if (d.isDirectory()) {
				String[] filesIndir = d.list();
				for (String fn : filesIndir) {
					File f = new File(dir + "/" + fn);
					// Debug.println("fn:"+fn+" "+f.isFile()+" ;
					String readyFile = fn.replace("execute.json", "ready.json");
						
					// "+fn.endsWith("settings"));
					if (f.isFile() && fn.endsWith("execute.json")) {
						
						// if status==Accepted
						if (Arrays.asList(filesIndir).contains(readyFile)) {
							String fileString = new String(Files.readAllBytes(Paths.get(new File(dir + "/" + readyFile).getAbsolutePath())));
							// Debug.println("found: "+fn+" "+fileString.length());
							JSONObject job = new JSONObject(fileString);
							jobArray.put(job);
							continue;
						}
						String fileString = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())));
						// Debug.println("found: "+fn+" "+fileString.length());
						JSONObject job = new JSONObject(fileString);
						String status = null;
						JSONObject wpsPostData = null;
						try {
							/* Extract the status */
							status = job.getString("wpsstatus");
							/* Extract the orginal settings with which the process was started */
							wpsPostData = job.getJSONObject("wpspostdata");
						} catch (JSONException e) {
						}
						if (status != null) {
							if (status.equalsIgnoreCase("PROCESSACCEPTED") || status.equalsIgnoreCase("PROCESSSTARTED")
									|| status.equalsIgnoreCase("PROCESSPAUSED")) {
//								Debug.println(fn + ": with status " + status + " let's look again");
								String statusLocation = job.getString("statuslocation");
								String queryString = null;
								try {
									URLDecoder.decode(job.getString("querystring"), "utf-8");
								} catch (Exception e) {
								}
								Debug.println("Querying " + statusLocation);
								JSONObject newJobStatus = NewStatusLocation(queryString, statusLocation);
								if(newJobStatus!=null && newJobStatus.length() !=0) {
									/* Put the wps post data (the input settings with which the wps was run) back into the new object */
									if (wpsPostData!=null) {
										newJobStatus.put("wpspostdata", wpsPostData);
									}
									try {
										Debug.println("Joblist - started copyStatusLocationElements");
										JSONObject output = ServiceHelperRequestMapper.copyStatusLocationElements(request, HTTPTools.makeHTTPGetRequest(statusLocation)).toJSONObject(null);
										if (output!=null) {
											newJobStatus.put("output", output);
											Debug.println("Joblist - finished copyStatusLocationElements");
											Debug.println(output.toString());
										} else {
											Debug.errprintln("Unable to copyStatusLocationElements");
										}
									}catch(Exception e) {
										Debug.printStackTrace(e);
									}
									String newWPSStatus = newJobStatus.getString("wpsstatus");
									/* Save the new ready.json file */
									if (newWPSStatus!=null && status!=null && !newWPSStatus.equals(status) && newWPSStatus.equals("PROCESSSUCCEEDED")) {
										String baseName = statusLocation.substring(statusLocation.lastIndexOf("/"))
												.replace(".xml", ".ready.json");
										String wpsSettingsFile = userDataDir + "/WPS_Settings/";
										Tools.mksubdirs(wpsSettingsFile);
										wpsSettingsFile += baseName;
										Tools.writeFile(wpsSettingsFile, newJobStatus.toString());
										Debug.println("Written " + wpsSettingsFile);
									}
									/* Update the job information with info retrieved from the statusLocation */
									if (status.equalsIgnoreCase("PROCESSSTARTED") || ((newJobStatus != null)
											&& !status.equals(newJobStatus.getString("wpsstatus")))) {
										job = newJobStatus;
									}
								}
							} else {
								// Debug.println(fn+": finished");
							}

						}
						// Debug.println(job.toString());
						jobArray.put(job);
					}
				}
			}
			jsonResponse.setMessage(jobs);
		} catch (Exception e) {
			Debug.printStackTrace(e);
			jsonResponse.setException("error: " + e.getMessage(), e);
		}
		
		jsonResponse.print(response);
	}
}