package nl.knmi.adaguc.services.pywpsserver;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.json.JSONObject;
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
import nl.knmi.adaguc.tools.MyXMLParser;
import nl.knmi.adaguc.tools.MyXMLParser.Options;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;
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
		Tools.mksubdirs(userDataDir+"/WPS_Settings/");
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

		
		try {
			String wpsRequest=HTTPTools.getHTTPParam(request, "request");
			if (wpsRequest.equalsIgnoreCase("execute")) {
				ByteArrayOutputStream baos=new ByteArrayOutputStream(0);
				CGIRunner.runCGIProgram(commands,environmentVariablesAsArray,userHomeDir,response,baos,null);

				Writer w = new OutputStreamWriter(outputStream, "UTF-8");
				w.write(baos.toString());
				w.close();
				getUserJobInfo(queryString, userDataDir, baos.toString());

			} else {
				CGIRunner.runCGIProgram(commands,environmentVariablesAsArray,userHomeDir,response,outputStream,null);

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

	private static void getUserJobInfo(String queryString, String userDataDir, String wpsResponse) throws Exception {
		MyXMLParser.XMLElement rootElement = new MyXMLParser.XMLElement();
		rootElement.parseString(wpsResponse);
		Debug.println(rootElement.toJSON(Options.NONE));
		String statusLocation=null;
		String creationTime=null;
		JSONObject data=new JSONObject();
		
		try {
		  Debug.println(wpsResponse);
		  statusLocation = rootElement.get("wps:ExecuteResponse").getAttrValue("statusLocation");
		  Debug.println("statusLocation:"+statusLocation);
		  String status = rootElement.get("wps:ExecuteResponse").get("wps:Status").get("wps:ProcessAccepted").getValue();
		  creationTime = rootElement.get("wps:ExecuteResponse").get("wps:Status").getAttrValue("creationTime");
		  String procId = rootElement.get("wps:ExecuteResponse").get("wps:Process").get("ows:Identifier").getValue();
          Debug.println(creationTime+", "+procId);
          data.put("statuslocation", statusLocation);
          data.put("status", status);
          data.put("creationtime", creationTime);
          data.put("id",  procId);
          if (queryString!=null) {
 
            String dataInputs=HTTPTools.getKVPItem(queryString, "DataInputs");
            String responseForm=HTTPTools.getKVPItem(queryString, "ResponseForm");
            if (dataInputs!=null) {
              dataInputs=dataInputs.substring(1,dataInputs.length()-1);
            }
            if (responseForm!=null) {
              responseForm=responseForm.substring(1,responseForm.length()-1);
            }
            Debug.println("DataInputs: "+dataInputs+" , ResponseForm:"+responseForm);
            XMLElement wpsElement=new XMLElement();
            XMLElement execElement=new XMLElement("Execute");
            XMLElement dataInputsEl=new XMLElement("DataInputs");
            for (String dt: dataInputs.split(";")) {
            	String terms[]=dt.split("=");
            	Debug.println(dt+" "+terms[0]+","+terms[1]);
            	if (terms.length==2) {
            		XMLElement inputEl=new XMLElement("Input");
            		XMLElement dataEl=new XMLElement("Data");
            		XMLElement literalDataEl=new XMLElement("LiteralData");
            		dataEl.add(literalDataEl);
            		literalDataEl.setValue(terms[1]);
            		inputEl.add(dataEl);
            		XMLElement identifierEl=new XMLElement("Identifier");
            		identifierEl.setValue(terms[0]);

            		inputEl.add(identifierEl);
            		dataInputsEl.add(inputEl);
            	}
            }
            execElement.add(dataInputsEl);
            wpsElement.add(execElement);
            
            data.put("wpspostdata", wpsElement.toJSON(Options.NONE));           
          }
          String uniqueID=statusLocation.substring(statusLocation.lastIndexOf("/")+1);
          data.put("uniqueid", uniqueID);
          
//  		  Tools.mksubdirs(userDataDir+"/WPS_Settings/");
  		  String baseName = statusLocation.substring(statusLocation.lastIndexOf("/")).replace(".xml", ".wpssettings");
  	      String wpsSettingsFile = userDataDir+"/WPS_Settings/";
  	      Tools.mksubdirs(wpsSettingsFile);
  	      wpsSettingsFile+=baseName;
  	      Tools.writeFile(wpsSettingsFile, data.toString());
  		  
		} catch(Exception e) {
		  Debug.println("Synchronous execution");
		}
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
