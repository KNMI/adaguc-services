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

import org.springframework.security.core.AuthenticationException;

import nl.knmi.adaguc.config.ConfigurationItemNotFoundException;
import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.security.AuthenticatorFactory;
import nl.knmi.adaguc.security.AuthenticatorInterface;
import nl.knmi.adaguc.security.user.UserManager;
import nl.knmi.adaguc.tools.CGIRunner;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.HTTPTools;
import nl.knmi.adaguc.tools.HTTPTools.InvalidHTTPKeyValueTokensException;
import nl.knmi.adaguc.tools.InvalidTokenException;
import nl.knmi.adaguc.tools.Tools;

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
	 * @throws ConfigurationItemNotFoundException 
	 * @throws IOException 
	 * @throws AuthenticationException 
	 * @throws InterruptedException 
	 * @throws Exception
	 */
	public static void runPyWPS(HttpServletRequest request,HttpServletResponse response,String queryString,OutputStream outputStream) throws AuthenticationException, IOException, ConfigurationItemNotFoundException, InterruptedException {
		Debug.println("RunPyWPS");
		List<String> environmentVariables = new ArrayList<String>();
		String userHomeDir="/tmp/";

		String homeURL=MainServicesConfigurator.getServerExternalURL();




		String pywpsExecLoc = PyWPSConfigurator.getPyWPSExecutable();
		Debug.println("PyWPSExecutableLocation: "+pywpsExecLoc);
		String pyWPSConfig = PyWPSConfigurator.getPyWPSConfig();
		String pyWPSProcessesDir = PyWPSConfigurator.getPyWPSProcessesDir();

		File f=new File(pywpsExecLoc);
		if(f.exists() == false || f.isFile() == false){
			Debug.errprintln("PyWPSServer executable not found");
			throw new ConfigurationItemNotFoundException("PyWPSServer executable not found");
		}

		if(response == null && outputStream == null){
			throw new IOException("Either response or outputstream needs to be set");
		}

		if(request == null && queryString == null){
			throw new IOException("Either request or queryString needs to be set");
		}

		if(outputStream == null){
			outputStream = response.getOutputStream();
		}

		if(queryString == null){
			queryString = request.getQueryString();
		}


		Debug.println("Using query string "+queryString);

		try {
			if(checkStatusLocation(queryString,response ) == true)return;
		} catch (InvalidTokenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidHTTPKeyValueTokensException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);
		if(authenticator != null){
			userHomeDir = UserManager.getUser(authenticator).getHomeDir();
		}


		String userDataDir = UserManager.getUser(authenticator).getDataDir();
		Tools.mksubdirs(userDataDir+"/WPS_Scratch/");
		environmentVariables.add( "POF_OUTPUT_PATH="+userDataDir+"/WPS_Scratch/");

		String pofOutputURL = homeURL+"/opendap/"+UserManager.getUser(authenticator).getUserId()+"/WPS_Scratch/";
		pofOutputURL = HTTPTools.makeCleanURL(pofOutputURL);
		pofOutputURL = pofOutputURL.replace("?", "");
		environmentVariables.add( "POF_OUTPUT_URL="+pofOutputURL);



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

	private static boolean checkStatusLocation(String queryString, HttpServletResponse response) throws InvalidTokenException, ConfigurationItemNotFoundException, InvalidHTTPKeyValueTokensException, IOException  {
		//  Check for status location first.
		Debug.println("Checking ["+queryString+"]");
		if(queryString!=null){ 
			String output = HTTPTools.getKVPItem(queryString, "OUTPUT");
			if(output!=null){
				String portalOutputPath = PyWPSConfigurator.getPyWPSOutputDir();

				//Remove first "/" token;
				output = output.substring(1);
				portalOutputPath = Tools.makeCleanPath(portalOutputPath);
				String fileName = portalOutputPath+"/"+output;
				Debug.println("WPS GET status request: "+fileName);

				Tools.checkValidCharsForFile(output);
				String data = Tools.readFile(fileName);
				if(response!=null){
					response.setContentType("text/xml");
				}
				response.getOutputStream().write(data.getBytes());          
				return true;
			}
		}
		Debug.println("No Output found");
		return false;

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
