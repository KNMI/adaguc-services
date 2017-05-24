package nl.knmi.adaguc.services.pywpsserver;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.tools.Tool;

import org.apache.commons.lang3.StringUtils;

import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.security.AuthenticatorFactory;
import nl.knmi.adaguc.security.AuthenticatorInterface;
import nl.knmi.adaguc.tools.CGIRunner;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.adaguc.usermanagement.UserManager;

/**
 * 
 * @author maartenplieger
 *
 */
public class PyWPSServer extends HttpServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Runs the PyWPS WPS server as executable on the system. 
	 * Emulates the behavior of scripts in a traditional cgi-bin directory of apache http server.
	 * @param request, the HttpServletRequest to obtain querystring parameters from.
	 * @param response Can be null, when given the content-type for the response will be set. 
	 * Results are not sent to this stream, this is done by outputStream parameter
	 * @param queryString The querystring for the CGI script
	 * @param outputStream A standard byte output stream in which the data of stdout is captured. 
	 * When null, it will be set to response.getOutputStream().
	 * @throws Exception
	 */
	public static void runPyWPS(HttpServletRequest request,HttpServletResponse response,String queryString,OutputStream outputStream) throws Exception{
		Debug.println("RunPyWPS");
		List<String> environmentVariables = new ArrayList<String>();
		String userHomeDir="/tmp/";

		String homeURL=MainServicesConfigurator.getServerExternalURL();
		if(homeURL == null){
			throw new Exception("401");
		}
		String pyWPSServerURL = homeURL+"/pywpsserver";


		AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);
		if(authenticator != null){
			userHomeDir = UserManager.getUser(authenticator).getHomeDir();    			    
		}

		String pywpsExecLoc = PyWPSConfigurator.getPyWPSExecutable();
		Debug.println("PyWPSExecutableLocation: "+pywpsExecLoc);

		if(pywpsExecLoc == null){
			throw new Exception("PyWPSServer executable not configured");
		}
		String pyWPSConfigTemplate = PyWPSConfigurator.getPyWPSConfigTemplate();
		if(pyWPSConfigTemplate == null){
			throw new Exception("adaguc-services.pywps-server.pywpsconfigtemplate not set");
		}
		String tempDir = PyWPSConfigurator.getTempDir();
		if(tempDir == null){
			throw new Exception("adaguc-services.pywps-server.tmp not set");
		}
		String pyWPSOutputDir = PyWPSConfigurator.getPyWPSOutputDir();
		if(pyWPSOutputDir == null){
			throw new Exception("adaguc-services.pywps-server.pywpsoutputdir not set");
		} 
		String pyWPSProcessesDir = PyWPSConfigurator.getPyWPSProcessesDir();
		if(pyWPSProcessesDir == null){
			throw new Exception("adaguc-services.pywps-server.pywpsprocesses not set");
		} 
		File f=new File(pywpsExecLoc);
		if(f.exists() == false || f.isFile() == false){
			Debug.errprintln("PyWPSServer executable not found");
			throw new Exception("PyWPSServer executable not found");
		}

		if(response == null && outputStream == null){
			throw new Exception("Either response or outputstream needs to be set");
		}

		if(request == null && queryString == null){
			throw new Exception("Either request or queryString needs to be set");
		}

		if(outputStream == null){
			outputStream = response.getOutputStream();
		}

		if(queryString == null){
			queryString = request.getQueryString();
		}

		String pyWPSConfig = tempDir + "/pywps-config.cfg";

		String configTemplate = Tools.readFile(pyWPSConfigTemplate);
		if(configTemplate == null){
			throw new Exception("adaguc-services.pywps-server.pywpsconfigtemplate is invalid ["+pyWPSConfigTemplate+"]");
		}
		String[] configLines = configTemplate.split("\n");

		for( int j=0;j< configLines.length;j++){
			String line = configLines[j];
			if(line.startsWith("serveraddress")){
				configLines[j]="serveraddress=" + pyWPSServerURL;
			}
			if(line.startsWith("tempPath")){
				configLines[j]="tempPath=" + tempDir;
			}
			if(line.startsWith("outputUrl")){
				configLines[j]="outputUrl=" + pyWPSServerURL+"?OUTPUT=";
			}
			if(line.startsWith("outputPath")){
				configLines[j]="outputPath=" + pyWPSOutputDir;
			}
			if(line.startsWith("outputPath")){
				configLines[j]="outputPath=" + pyWPSOutputDir;
			}
			if(line.startsWith("processesPath")){
				configLines[j]="processesPath=" + pyWPSProcessesDir;
			}
			if(line.startsWith("maxinputparamlength")){
				configLines[j]="maxinputparamlength=32768";
			}
			if(line.startsWith("maxfilesize")){
				configLines[j]="maxfilesize=500mb";
			}
		}


		String newConfig = StringUtils.join(configLines, "\n");
		Tools.writeFile(pyWPSConfig, newConfig);

		environmentVariables.add("HOME="+userHomeDir);
		environmentVariables.add("QUERY_STRING="+queryString);
		environmentVariables.add("ADAGUC_ONLINERESOURCE="+homeURL+"/adagucserver?");
		environmentVariables.add("ADAGUC_TMP="+userHomeDir+"/tmp/");
		environmentVariables.add("PYWPS_CFG="+pyWPSConfig);
		environmentVariables.add("PYWPS_PROCESSES="+pyWPSProcessesDir);

		String[] configEnv = PyWPSConfigurator.getPyWPSEnvironment();
		if(configEnv == null){
			Debug.println("Warning PyWPS environment is not configured");
		}else{
			for(int j=0;j<configEnv.length;j++)environmentVariables.add(configEnv[j]);
		}
		String commands[] = {pywpsExecLoc};

		String[] environmentVariablesAsArray = new String[ environmentVariables.size() ];
		environmentVariables.toArray( environmentVariablesAsArray );

		CGIRunner.runCGIProgram(commands,environmentVariablesAsArray,userHomeDir,response,outputStream,null);

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request,response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		Debug.println("Handle PyWPS WPS requests");
		OutputStream out1 = null;
		//response.setContentType("application/json");
		try {
			out1 = response.getOutputStream();
		} catch (IOException e) {
			Debug.errprint(e.getMessage());
			return;
		}

		try {
			PyWPSServer.runPyWPS(request,response,request.getQueryString(),out1);

		} catch (Exception e) {
			response.setStatus(401);
			try {
				out1.write(e.getMessage().getBytes());
			} catch (Exception e1) {
				Debug.errprintln("Unable to write to stream");
				Debug.printStackTrace(e);
			}
		}    
	}


}
