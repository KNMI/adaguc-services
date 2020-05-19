package nl.knmi.adaguc.services.adagucserver;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.security.AuthenticatorFactory;
import nl.knmi.adaguc.security.AuthenticatorInterface;
import nl.knmi.adaguc.security.user.UserManager;
import nl.knmi.adaguc.tools.CGIRunner;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.ProcessRunner;
import nl.knmi.adaguc.tools.Tools;

/**
 * 
 * @author maartenplieger
 *
 */
public class ADAGUCServer extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Runs the ADAGUC WMS server as executable on the system. Emulates the behavior
	 * of scripts in a traditional cgi-bin directory of apache http server.
	 * 
	 * @param request,     the HttpServletRequest to obtain querystring parameters
	 *                     from.
	 * @param response     Can be null, when given the content-type for the response
	 *                     will be set. Results are not sent to this stream, this is
	 *                     done by outputStream parameter
	 * @param queryString  The querystring for the CGI script
	 * @param outputStream A standard byte output stream in which the data of stdout
	 *                     is captured. When null, it will be set to
	 *                     response.getOutputStream().
	 * @throws Exception
	 */
	public static void runADAGUCWMS(HttpServletRequest request, HttpServletResponse response, String queryString,
			OutputStream outputStream) throws Exception {
		runADAGUC(request, response, queryString, outputStream, ADAGUCServiceType.WMS);
	}

	/**
	 * Runs the ADAGUC WCS server as executable on the system. Emulates the behavior
	 * of scripts in a traditional cgi-bin directory of apache http server.
	 * 
	 * @param request,     the HttpServletRequest to obtain querystring parameters
	 *                     from.
	 * @param response     Can be null, when given the content-type for the response
	 *                     will be set. Results are not sent to this stream, this is
	 *                     done by outputStream parameter
	 * @param queryString  The querystring for the CGI script
	 * @param outputStream A standard byte output stream in which the data of stdout
	 *                     is captured. When null, it will be set to
	 *                     response.getOutputStream().
	 * @throws Exception
	 */
	public static void runADAGUCWCS(HttpServletRequest request, HttpServletResponse response, String queryString,
			OutputStream outputStream) throws Exception {
		runADAGUC(request, response, queryString, outputStream, ADAGUCServiceType.WCS);
	}

	enum ADAGUCServiceType {
		WMS, WCS, OPENDAP
	}

	private static AtomicInteger numInstancesRunning = new AtomicInteger(0);
	private static AtomicInteger numInstancesInQue = new AtomicInteger(0);

	public static void runADAGUC(HttpServletRequest request, HttpServletResponse response, String queryString,
			OutputStream outputStream, ADAGUCServiceType serviceType) throws Exception {
		Debug.println("runADAGUC");
		int maxInstances = ADAGUCConfigurator.getMaxInstances();
		int maxInstancesInQueue = ADAGUCConfigurator.getMaxInstancesInQueue();
		Exception exception = null;
		String instanceId = UUID.randomUUID().toString();

		if (maxInstancesInQueue > 0 && numInstancesInQue.get() > maxInstancesInQueue) {

			String msg = "[ADAGUC-Server] Queue limit [" + maxInstancesInQueue + "]  exceeded";
			Debug.errprintln(msg);
			response.setStatus(500);
			OutputStream o = response.getOutputStream();
			o.write(msg.getBytes());
			return;
		}

		try {
			if (maxInstances > 0 && maxInstancesInQueue > 0) {
				if (numInstancesRunning.get() >= maxInstances) {
					Debug.println("[ADAGUC-Server] Too many instances running, Queued: [" + numInstancesInQue.get()
							+ "], Running: [" + numInstancesRunning + "]");
				}
				if (maxInstancesInQueue > 0)
					numInstancesInQue.getAndIncrement();
				try {
					while (numInstancesRunning.get() >= maxInstances) {
						Thread.sleep(100);
					}
				} catch (Exception e) {
					exception = e;
				}
				if (maxInstancesInQueue > 0)
					numInstancesInQue.getAndDecrement();
			}
		} catch (Exception e) {
			exception = e;
		}
		if (exception == null) {
			numInstancesRunning.getAndIncrement();
			try {
				_runADAGUC(request, response, queryString, outputStream, serviceType, instanceId);
			} catch (Exception e) {
				exception = e;
			}
			numInstancesRunning.getAndDecrement();
		}

		if (exception != null)
			throw exception;
	}

	private static void _runADAGUC(HttpServletRequest request, HttpServletResponse response, String queryString,
			OutputStream outputStream, ADAGUCServiceType serviceType, String instanceId) throws Exception {

		// Debug.println("Headers:");
		// Enumeration<String> headerNames = request.getHeaderNames();
		// while (headerNames.hasMoreElements()) {
		// String headerName = headerNames.nextElement();
		// String headerValue = request.getHeader(headerName);
		// Debug.println(headerName + ":" + headerValue);
		// }
		//

		String userHomeDir = "/tmp/";

		AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);
		if (authenticator != null) {
			try {
				userHomeDir = UserManager.getUser(authenticator).getHomeDir();
			} catch (Exception e) {
				Debug.println("No user information provided: " + e.getMessage());
			}

		}
		// Debug.println("Using home " + userHomeDir);
		String homeURL = MainServicesConfigurator.getServerExternalURL();
		String adagucExecutableLocation = ADAGUCConfigurator.getADAGUCExecutable();
		// Debug.println("adagucExecutableLocation: "+adagucExecutableLocation);

		if (adagucExecutableLocation == null) {
			Debug.errprintln("Adagucserver executable not configured");
			throw new Exception("Adagucserver executable not configured");
		}

		File f = new File(adagucExecutableLocation);
		if (f.exists() == false || f.isFile() == false) {
			String message = "Adagucserver executable not found at [" + adagucExecutableLocation + "]";
			Debug.errprintln(message);
			throw new Exception(message);
		}

		if (response == null && outputStream == null) {
			throw new Exception("Either response or outputstream needs to be set");
		}

		if (request == null && queryString == null) {
			throw new Exception("Either request or queryString needs to be set");
		}

		if (outputStream == null) {
			outputStream = response.getOutputStream();
		}

		if (queryString == null) {
			queryString = request.getQueryString();
		}
		Debug.println("[ADAGUC-Server] queryString [" + queryString + "]");

		List<String> environmentVariables = new ArrayList<String>();
		String tmpDir = userHomeDir + "/adaguctmp/";
		Tools.mksubdirs(tmpDir);
		environmentVariables.add("ADAGUC_TMP=" + tmpDir);
		String tmpLogFile = tmpDir + "adaguc-server-cgi-log" + instanceId;
		Debug.println("Logging to " + tmpLogFile);
		environmentVariables.add("ADAGUC_LOGFILE=" + tmpLogFile);
		environmentVariables.add("HOME=" + userHomeDir);
		environmentVariables.add("QUERY_STRING=" + queryString);
		environmentVariables.add("CONTENT_TYPE=" + request.getHeader("Content-Type"));
		if (serviceType == ADAGUCServiceType.WMS) {
			environmentVariables.add("ADAGUC_ONLINERESOURCE=" + homeURL + "/wms?");
		}
		if (serviceType == ADAGUCServiceType.WCS) {
			environmentVariables.add("ADAGUC_ONLINERESOURCE=" + homeURL + "/wcs?");
		}

		if (serviceType == ADAGUCServiceType.OPENDAP) {

			environmentVariables.add("ADAGUC_ONLINERESOURCE=" + homeURL + "/adagucopendap?");
			environmentVariables.add("REQUEST_URI=" + request.getRequestURI());
			environmentVariables.add("SCRIPT_NAME=");
			Debug.println(request.getRequestURI());
		}

		String[] configEnv = ADAGUCConfigurator.getADAGUCEnvironment();
		if (configEnv == null) {
			Debug.println("ADAGUC environment is not configured");
		} else {
			for (int j = 0; j < configEnv.length; j++) {
				if (!configEnv[j].startsWith("ADAGUC_LOGFILE") && !configEnv[j].startsWith("ADAGUC_TMP")
						&& !configEnv[j].startsWith("ADAGUC_ONLINERESOURCE")) {
					environmentVariables.add(configEnv[j]);
				} else {
					Debug.errprintln("[WARNING]: Environment " + configEnv[j] + " is controlled by adaguc-services.");
				}
			}
		}
		String commands[] = { adagucExecutableLocation };

		String[] environmentVariablesAsArray = new String[environmentVariables.size()];
		environmentVariables.toArray(environmentVariablesAsArray);
		long timeOutMs = ADAGUCConfigurator.getTimeOut();

		int statusCode = CGIRunner.runCGIProgram(commands, environmentVariablesAsArray, userHomeDir, response, outputStream,
				null, timeOutMs);
		if (statusCode != 143) {
			try {
				Debug.println("\n" + Tools.readFile(tmpLogFile));
				Tools.rmfile(tmpLogFile);
			} catch (Exception e) {
				Debug.println("[ADAGUC-Server]: No logfile");
			}
		} else {
			Debug.println("[ADAGUC-Server]: Timeout");
		}
	}

	public static void runADAGUC(String userHomeDir, String[] args, OutputStream outputStream)
			throws IOException, ElementNotFoundException, InterruptedException {
		String instanceId = UUID.randomUUID().toString();
		List<String> environmentVariables = new ArrayList<String>();
		String tmpDir = userHomeDir + "/adaguctmp/";
		Tools.mksubdirs(tmpDir);
		environmentVariables.add("ADAGUC_TMP=" + tmpDir);
		String tmpLogFile = tmpDir + "adaguc-server-cmd-log" + instanceId;
		Debug.println("Logging to " + tmpLogFile);
		environmentVariables.add("ADAGUC_LOGFILE=" + tmpLogFile);
		environmentVariables.add("HOME=" + userHomeDir);

		String[] configEnv = ADAGUCConfigurator.getADAGUCEnvironment();
		if (configEnv == null) {
			Debug.println("ADAGUC environment is not configured");
		} else {
			for (int j = 0; j < configEnv.length; j++) {
				if (!configEnv[j].startsWith("ADAGUC_LOGFILE") && !configEnv[j].startsWith("ADAGUC_TMP")
						&& !configEnv[j].startsWith("ADAGUC_ONLINERESOURCE")) {
					environmentVariables.add(configEnv[j]);
				} else {
					Debug.errprintln("[WARNING]: Environment " + configEnv[j] + " is controlled by adaguc-services.");
				}
			}
		}
		String adagucExecutableLocation = ADAGUCConfigurator.getADAGUCExecutable();
		List<String> commands = new ArrayList<String>();
		commands.add(adagucExecutableLocation);
		for (int j = 0; j < args.length; j++) {
			commands.add(args[j]);
		}

		String[] environmentVariablesAsArray = new String[environmentVariables.size()];
		environmentVariables.toArray(environmentVariablesAsArray);
		long timeOutMs = ADAGUCConfigurator.getTimeOut();

		class StdoutPrinter implements ProcessRunner.StatusPrinterInterface {
			public void setError(String message) {
			}

			public String getError() {
				return null;
			}

			public boolean hasData() {
				return false;
			}

			public void print(byte[] message, int bytesRead) {
				try {
					outputStream.write(message, 0, bytesRead);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		ProcessRunner processRunner = new ProcessRunner(new StdoutPrinter(), new StdoutPrinter(),
				environmentVariablesAsArray, userHomeDir, timeOutMs);
		processRunner.runProcess(commands.toArray(new String[0]), null);
		try {
			Tools.rmfile(tmpLogFile);
		} catch (Exception e) {
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		Debug.println("Handle ADAGUC WMS requests");
		OutputStream out1 = null;
		// response.setContentType("application/json");
		try {
			out1 = response.getOutputStream();
		} catch (IOException e) {
			Debug.errprint(e.getMessage());
			return;
		}

		try {
			ADAGUCServer.runADAGUCWMS(request, response, request.getQueryString(), out1);

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

	public static void runADAGUCOpenDAP(HttpServletRequest request, HttpServletResponse response, String queryString,
			OutputStream outputStream) throws Exception {
		runADAGUC(request, response, queryString, outputStream, ADAGUCServiceType.OPENDAP);
	}

	public static int getNumInstancesInQueue() {
		return numInstancesInQue.get();
	}

	public static int getNumInstancesRunning() {
		return numInstancesRunning.get();
	}

}
