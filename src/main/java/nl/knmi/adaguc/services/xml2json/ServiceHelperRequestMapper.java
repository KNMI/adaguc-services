package nl.knmi.adaguc.services.xml2json;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.ietf.jgss.GSSException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.security.AuthenticatorFactory;
import nl.knmi.adaguc.security.AuthenticatorInterface;
import nl.knmi.adaguc.security.PemX509Tools;
import nl.knmi.adaguc.security.PemX509Tools.X509UserCertAndKey;
import nl.knmi.adaguc.security.SecurityConfigurator;
import nl.knmi.adaguc.security.user.User;
import nl.knmi.adaguc.security.user.UserManager;
import nl.knmi.adaguc.services.adagucserver.ADAGUCServer;
import nl.knmi.adaguc.services.basket.Basket;
import nl.knmi.adaguc.services.joblist.JobListRequestMapper;
import nl.knmi.adaguc.services.pywpsserver.PyWPSServer;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.JSONResponse;
import nl.knmi.adaguc.tools.MyXMLParser;
import nl.knmi.adaguc.tools.MyXMLParser.Options;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;
import nl.knmi.adaguc.tools.Tools;


@RestController
public class ServiceHelperRequestMapper {
	//	@Bean
	//	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
	//		ObjectMapper mapper = new ObjectMapper();
	//		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
	//		MappingJackson2HttpMessageConverter converter = 
	//				new MappingJackson2HttpMessageConverter(mapper);
	//		return converter;
	//	}
	@ResponseBody
	@CrossOrigin
	@RequestMapping("xml2json")
	public void XML2JSON(
			@RequestParam(value="request")String request,
			@RequestParam(value="callback", 
			required=false)String callback, HttpServletRequest servletRequest, HttpServletResponse response){
		Debug.println("#### SERVLET /xml2json ####");
		/**
		 * Converts XML file pointed with request to JSON file
		 * @param requestStr
		 * @param out1
		 * @param response
		 */
		JSONResponse jsonResponse = new JSONResponse(servletRequest);
		String requestStr;
		try {
			requestStr=URLDecoder.decode(request,"UTF-8");
			MyXMLParser.XMLElement rootElement = new MyXMLParser.XMLElement();
			//Remote XML2JSON request to external WMS service
			boolean isLocal = false;
			Debug.println("xml2json " + requestStr);
			if(requestStr.startsWith(MainServicesConfigurator.getServerExternalURL()) && requestStr.toUpperCase().contains("SERVICE=WMS")){
				Debug.println("Running local adaguc for ["+requestStr+"]");
				isLocal = true;
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				String url = requestStr.substring(MainServicesConfigurator.getServerExternalURL().length());
				url = url.substring(url.indexOf("?")+1);
				Debug.println("url = ["+url+"]");
				ADAGUCServer.runADAGUCWMS(servletRequest, null, url, outputStream);
				String getCapabilities = new String(outputStream.toByteArray());
				outputStream.close();
				rootElement.parseString(getCapabilities);
			}

			User user = null;
			X509UserCertAndKey userCertificate = null;
			String ts = null;
			char [] tsPass = null;
			if(isLocal == false){


				if(requestStr.startsWith("https://")){
					ts = SecurityConfigurator.getTrustStore();
				}
				if(ts!=null ){
					tsPass = SecurityConfigurator.getTrustStorePassword().toCharArray();

					Debug.println("Setting up user cert with truststore");



					AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(servletRequest);
					if(authenticator!=null){

						try {
							user = UserManager.getUser(authenticator);
						} catch(Exception e) {

						}
						if(user!=null){
							userCertificate = user.getCertificate();
						}
					}
					String result = new String(makeRequest(requestStr, userCertificate, ts, tsPass));
					rootElement.parseString(result);
				}else{
					Debug.println("Running remote adaguc without truststore");

					rootElement.parse(new URL(requestStr));
				}
			}

			/* Hookup WPS request calls */
			if (requestStr.toUpperCase().contains("SERVICE=WPS")) {
				Debug.println("This is a WPS call");
				if (requestStr.toUpperCase().contains("REQUEST=EXECUTE")) {
					Debug.println("This is a WPS Execute call, store in jobs!");
					JobListRequestMapper.saveExecuteResponseToJob(requestStr, rootElement.toString(), servletRequest);
				}
			}

			/* Hookup WPS response calls */
			try{
				JSONObject test = PyWPSServer.statusLocationDataAsJSONElementToWPSStatusObject(null, rootElement.toJSONObject(Options.NONE));
				String wpsID = null;
				try{
					wpsID = test.getString("id");
				}catch(Exception e){
				}

				if (wpsID!=null && test.getString("wpsstatus").equals(PyWPSServer.WPSStatus.PROCESSSUCCEEDED.toString())) {
					rootElement = copyStatusLocationElements(servletRequest, rootElement.toString());

				}
				Debug.println(test.toString());
			}catch(Exception e){
				Debug.printStackTrace(e);
			}
			jsonResponse.setMessage(rootElement.toJSON(null));
		} catch (Exception e) {
			e.printStackTrace();
			jsonResponse.setException(e.getMessage(),e);
		}

		try {
			jsonResponse.print(response);
		} catch (Exception e1) {

		}

	}
	static public XMLElement copyStatusLocationElements(HttpServletRequest servletRequest, String statusLocationResult) throws Exception {
		MyXMLParser.XMLElement rootElement = new MyXMLParser.XMLElement();
		rootElement.parseString(statusLocationResult);
		JSONObject test = PyWPSServer.statusLocationDataAsJSONElementToWPSStatusObject(null, rootElement.toJSONObject(Options.NONE));
		Debug.println("============== OK WPS SUCCESFULLY FINISHED, START COPY TO BASKET ================ ");
		/* Parse outputs and copy them to local basket */
		String wpsID = null;
		try{
			wpsID = test.getString("id");
		}catch(Exception e){
		}
		User user = null;
		AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(servletRequest);
		if(authenticator!=null){
			try {
				user = UserManager.getUser(authenticator);
			} catch(Exception e) {
			}
		}
		if (user == null){
			throw new Exception("Error, user is null");
		}
		if (user.getDataDir() == null){
			throw new Exception("Error, user.getDataDir() is null");
		}

		Vector<XMLElement> processOutputs = rootElement.get("wps:ExecuteResponse").get("wps:ProcessOutputs").getList("wps:Output");
		for(int j=0;j<processOutputs.size();j++){
			//			Debug.println(j + ")" + processOutputs.get(j).toString());
			String identifier = processOutputs.get(j).get("ows:Identifier").getValue();
			String title = processOutputs.get(j).get("ows:Title").getValue();

			Debug.println("Identifying " + identifier + "/" + title);
			String processFolder = test.getString("processid")+"_"+ test.getString("creationtime").replaceAll(":", "").replaceAll("-", "")+"_"+ wpsID;
			try {
				XMLElement refObj = null;
				try {
					refObj = processOutputs.get(j).get("wps:Reference");
				}catch(Exception e){
					Debug.println("processOutput " + identifier + " has no wps:Reference");
				}
				if (refObj!=null) {
					String reference = refObj.getAttrValue("href");
					Debug.println("Remote reference is " + reference);
					if (reference!= null && reference.length() > 0 && reference.startsWith("http")) {
						String mimeType= refObj.getAttrValue("mimeType");
						Debug.println("Processfolder is " + processFolder);
						String destLoc = user.getDataDir() + "/" + "/" + processFolder;
						String basketLocalFilename = FilenameUtils.getBaseName(reference) + "." + FilenameUtils.getExtension(reference);
						Debug.println("basketLocalFilename: " + basketLocalFilename);
						if (basketLocalFilename.equals(".")) {
							basketLocalFilename = identifier;
							if (mimeType.equals("application/x-netcdf")) { basketLocalFilename += ".nc"; }
							if (mimeType.equals("image/png")) { basketLocalFilename += ".png"; }
							if (mimeType.equals("text/plain")) { basketLocalFilename += ".txt"; }
							if (mimeType.equals("application/zip")) { basketLocalFilename += ".zip"; }
							if (mimeType.equals("application/json")) { basketLocalFilename += ".json"; }
							if (mimeType.equals("application/yml")) { basketLocalFilename += ".yml"; }
							if (mimeType.equals("text/csv")) { basketLocalFilename += ".csv"; }

						}
						Debug.println("basketLocalFilename: " + basketLocalFilename);
						String fullPath = destLoc + "/" + basketLocalFilename;
						if (new File(fullPath).exists() == false) {
							Debug.println("Start copy " + reference);
							// TODO: ADD SECURITY CHECKS
							Tools.mksubdirs(destLoc);
							char [] tsPass = SecurityConfigurator.getTrustStorePassword().toCharArray();
							String ts = SecurityConfigurator.getTrustStore();
							X509UserCertAndKey userCertificate = user.getCertificate();
							Tools.writeFile(fullPath, makeRequest(reference, userCertificate, ts, tsPass));
						} else {
							Debug.println("Already copied " + reference + " with path [" + fullPath + ']');
						}
						String basketRemoteURL = Basket.GetRemotePrefix(user) + processFolder + "/" + basketLocalFilename;
						refObj.setAttr("href", basketRemoteURL);
					} else {
						Debug.errprintln("Warning reference is not set for " + identifier);
					}
				}
			}catch(Exception e){
				Debug.printStackTrace(e);
			}



		}
		return rootElement;
	}
	private static byte[] makeRequest(String requestStr, X509UserCertAndKey userCertificate, String ts, char[] tsPass) throws KeyManagementException, UnrecoverableKeyException, InvalidKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, NoSuchProviderException, SignatureException, IOException, GSSException {
		try {
			/* First try without user certificate */
			CloseableHttpClient httpClient = (new PemX509Tools()).
					getHTTPClientForPEMBasedClientAuth(ts, tsPass, null);
			CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(requestStr));
			return EntityUtils.toByteArray(httpResponse.getEntity());
		} catch (Exception e){
			if (userCertificate!=null) {
				/* Second, try with user certificate */
				CloseableHttpClient httpClient = (new PemX509Tools()).
						getHTTPClientForPEMBasedClientAuth(ts, tsPass, userCertificate);
				CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(requestStr));
				return EntityUtils.toByteArray(httpResponse.getEntity());

			} else{
				Debug.println("Request without user certificate failed " +e.getMessage());
				throw(e);
			}
		}
	}



}

