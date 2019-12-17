/*
  Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php 

  Copyright (C) 2015 by Royal Netherlands Meteorological Institute (KNMI)

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
  THE SOFTWARE.
 */
/*
 Authors: Maarten Plieger (plieger at knmi.nl) and Ernst de Vreede, KNMI
 */

package nl.knmi.adaguc.services.oauth2;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.oltu.commons.encodedtoken.TokenDecoder;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.ietf.jgss.GSSException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.security.CertificateVerificationException;
import nl.knmi.adaguc.security.PemX509Tools;
import nl.knmi.adaguc.security.PemX509Tools.X509UserCertAndKey;
import nl.knmi.adaguc.security.SecurityConfigurator;
import nl.knmi.adaguc.security.token.Token;
import nl.knmi.adaguc.security.token.TokenManager;
import nl.knmi.adaguc.security.user.User;
import nl.knmi.adaguc.security.user.UserManager;
import nl.knmi.adaguc.services.oauth2.OAuthConfigurator.Oauth2Settings;
import nl.knmi.adaguc.tools.DateFunctions;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.HTTPTools;
import nl.knmi.adaguc.tools.JSONResponse;
import nl.knmi.adaguc.tools.KVPKey;
import nl.knmi.adaguc.tools.WebRequestBadStatusException;



/**
 * Class which helps handling OAuth requests. Uses APACHE oltu, bouncycastle and
 * java security.
 * 
 * @author Maarten Plieger and Ernst de Vreede, KNMI
 * 
 *         If you use parts of this code, please let us know :).
 *
 */
public class OAuth2Handler {

	/*
	 * Documentation:
	 * 
	 * === First of all: === !!! Remember to add accounts.google ssl certificate
	 * to truststore !!! And add other SSL certificates from configured Oauth2
	 * providers like CEDA
	 * 
	 * === Adding an SSL cert to the truststore can be done like: === echo |
	 * openssl s_client -connect accounts.google.com:443 2>&1 | sed -ne '/-BEGIN
	 * CERTIFICATE-/,/-END CERTIFICATE-/p' > accounts.google.com echo | openssl
	 * s_client -connect github.com:443 2>&1 | sed -ne '/-BEGIN
	 * CERTIFICATE-/,/-END CERTIFICATE-/p' > github.com keytool -import -v
	 * -trustcacerts -alias accounts.google.com -file accounts.google.com
	 * -keystore esg-truststore2.ts keytool -import -v -trustcacerts -alias
	 * github.com -file github.com -keystore esg-truststore2.ts
	 * 
	 * echo | openssl s_client -connect slcs.ceda.ac.uk:443 2>&1 | sed -ne
	 * '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > slcs.ceda.ac.uk keytool
	 * -import -v -trustcacerts -alias slcs.ceda.ac.uk -file slcs.ceda.ac.uk
	 * -keystore /usr/people/plieger/impactportal/esg-truststore2.ts
	 *
	 * 
	 * 
	 * === Test URLs to check which are restricted ===
	 * /impactportal/ImpactService?&source=http://vesg.ipsl.fr/thredds/dodsC/
	 * esg_dataroot/CMIP5/output1/IPSL/IPSL-CM5A-LR/1pctCO2/day/atmos/cfDay/
	 * r1i1p1/v20110427/albisccp/albisccp_cfDay_IPSL-CM5A-
	 * LR_1pctCO2_r1i1p1_19700101-19891231.nc&SERVICE=WMS&&SERVICE=WMS&VERSION=1
	 * .3.0&REQUEST=GetMap&LAYERS=albisccp&WIDTH=1635&HEIGHT=955&CRS=EPSG:4326&
	 * BBOX=-105.13761467889908,-180,105.13761467889908,180&STYLES=auto/nearest&
	 * FORMAT=image/png&TRANSPARENT=TRUE&&time=1989-11-27T12:00:00Z
	 *
	 * === wget example to climate4impact with an OAuth2 access_token used as
	 * bearer in the headers ===
	 * "http://climate4impact.eu/impactportal/ImpactService?&service=basket&request=getoverview&_dc=1424696174221&node=root"
	 * --header="Authorization: Bearer <access token>" -O info.txt
	 * --no-check-certificate
	 * 
	 * === wget example with a JWT ID Token to climate4impact === wget
	 * "http://climate4impact.eu/impactportal/ImpactService?&service=basket&request=getoverview&_dc=1424696174221&node=root"
	 * --header="Authorization: JWT <jwt id token>"
	 * 
	 * === wget example with an access_token to Google OpenID connect services
	 * === wget "https://www.googleapis.com/plus/v1/people/me/openIdConnect?"
	 * --header="Authorization: Bearer <access token>" -O info.txt
	 * --no-check-certificate
	 *
	 * === Useful links: === -
	 * http://self-issued.info/docs/draft-ietf-oauth-v2-bearer.html#authz-header
	 *
	 * - http://self-issued.info/docs/draft-jones-json-web-token-01.html#
	 * DefiningRSA - https://www.googleapis.com/oauth2/v2/certs -
	 * https://console.developers.google.com/project
	 * 
	 */

	static Map<String, StateObject> oauthStatesMapper = new ConcurrentHashMap<String, StateObject>();// Remembered
	// states

	public static class StateObject {
		StateObject(String redirectURL) {
			this.returnURL = redirectURL;
			creationTimeMillies = DateFunctions.getCurrentDateInMillis();
		}

		public String returnURL = "";
		long creationTimeMillies;
	}

	private static void cleanStateObjects() {
		long currentTimeMillis = DateFunctions.getCurrentDateInMillis();
		for (Map.Entry<String, StateObject> entry : oauthStatesMapper.entrySet()) {
			StateObject stateObject = entry.getValue();
			if (currentTimeMillis - stateObject.creationTimeMillies > 1000 * 60) {
				Debug.println("Removing unused state with key" + entry.getKey());
				oauthStatesMapper.remove(entry.getKey());
			}
		}
	}

	static String defaultOAuthCallbackURL = "/oauth"; // The external Servlet location

	/**
	 * UserInfo object used to share multiple userinfo attributes over
	 * functions.
	 * 
	 * @author plieger
	 *
	 */
	public static class UserInfo {
		public String user_openid = null;
		public String user_identifier = null;
		public String user_email = null;
		public String certificate;
		public String oauth_access_token;
		public String certificate_notafter;
	}

	/**
	 * Endpoint which should directly be called by the servlet.
	 * 
	 * @param request
	 * @param response
	 * @throws ElementNotFoundException
	 */
	public static void doGet(HttpServletRequest request, HttpServletResponse response) throws ElementNotFoundException, IOException {

		// Check if we are dealing with getting JSON request for building up the
		// login form
		String makeform = null;
		try {
			makeform = HTTPTools.getHTTPParam(request, "makeform");
		} catch (Exception e) {
		}
		if (makeform != null) {
			makeForm(request, response);
			return;
		}

		// Check if we are dealing with step1 or step2 in the OAuth process.
		String code = null;
		try {
			code = HTTPTools.getHTTPParam(request, "code");
		} catch (Exception e) {
		}

		if (code == null) {
			// Step 1
			Debug.println("Step 1: start GetCode request for " + request.getQueryString());
			try {
				getCode(request, response);
			} catch (OAuthSystemException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// Step 2
			Debug.println("Step 2: start makeOauthzResponse for " + request.getQueryString());
			makeOauthzResponse(request, response);

		}
	};

	/**
	 * Step 1: Starts Oauth2 authentication request. It retrieves a one time
	 * usable code which can be used to retrieve an access token or id token
	 * 
	 * @param httpRequest
	 * @return
	 * @throws OAuthSystemException
	 * @throws IOException
	 * @throws ElementNotFoundException
	 */
	static void getCode(HttpServletRequest httpRequest, HttpServletResponse response)
			throws OAuthSystemException, IOException, ElementNotFoundException {

		Debug.println("getQueryString:" + httpRequest.getQueryString());

		String returnURL = "";
		try {
			returnURL = HTTPTools.getHTTPParam(httpRequest, "returnurl");
		} catch (Exception e1) {
			Debug.println("Note: No redir URL given");
		}

		cleanStateObjects();

		String stateID = UUID.randomUUID().toString();

		Debug.println("Putting info in stateID [" + stateID + "]");
		oauthStatesMapper.put(stateID, new StateObject(returnURL));

		String provider = null;
		try {
			provider = HTTPTools.getHTTPParam(httpRequest, "provider");
		} catch (Exception e) {
		}
		Debug.println("  OAuth2 Step 1 getCode: Provider is " + provider);

		OAuthConfigurator.Oauth2Settings settings = OAuthConfigurator.getOAuthSettings(provider);
		if (settings == null) {
			Debug.errprintln("  OAuth2 Step 1 getCode: No Oauth settings set");
			return;
		}

		String oAuthCallbackURL = MainServicesConfigurator.getServerExternalURL() + defaultOAuthCallbackURL;
		if (settings.oauthCallbackURL != null && settings.oauthCallbackURL.length() > 0) {
			oAuthCallbackURL = settings.oauthCallbackURL;  
		}

		Debug.println("  OAuth2 Step 1 getCode: Using " + settings.id);

		JSONObject state = new JSONObject();
		try {
			state.put("provider", provider);
			state.put("state_id", stateID);
		} catch (JSONException e) {
			
			e.printStackTrace();
		}

		OAuthClientRequest oauth2ClientRequest = OAuthClientRequest.authorizationLocation(settings.OAuthAuthLoc)
				.setClientId(settings.OAuthClientId)
				.setRedirectURI(oAuthCallbackURL)
				.setScope(settings.OAuthClientScope).setResponseType("code").setState(state.toString())
				.buildQueryMessage();

		Debug.println("  OAuth2 Step 1 getCode: locationuri = " + oauth2ClientRequest.getLocationUri());
		response.sendRedirect(oauth2ClientRequest.getLocationUri());
	}

	/**
	 * Step 2: Get authorization response. Here the access_tokens and possibly
	 * id_tokens are retrieved with the previously retrieved code.
	 * 
	 * @param request
	 * @param response
	 */
	public static void makeOauthzResponse(HttpServletRequest request, HttpServletResponse response) {
		try {
			OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);

			String stateResponseAsString = oar.getState();
			if (stateResponseAsString == null) {
				stateResponseAsString = "";
			}
			if (stateResponseAsString.equals("")) {
				Debug.errprintln("  OAuth2 Step 2 OAuthz:  FAILED");
				return;
			}

			Debug.println("  OAuth2 Step 2 OAuthz:  State is " + stateResponseAsString);

			JSONObject stateResponseAsJSONObject = (JSONObject) new JSONTokener(stateResponseAsString).nextValue();

			String stateID = stateResponseAsJSONObject.getString("state_id");

			Debug.println("  OAuth2 Step 2 OAuthz: stateID=" + stateID);

			if (request.getParameter("r") != null) {
				Debug.println("  OAuth2 Step 2 OAuthz:  Token request already done, stopping");
				return;
			}

			String currentProvider = stateResponseAsJSONObject.getString("provider");
			Debug.println("  OAuth2 Step 2 OAuthz: Provider=" + currentProvider);

			Debug.println("  OAuth2 Step 2 OAuthz:  Starting token request");

			OAuthConfigurator.Oauth2Settings settings = OAuthConfigurator.getOAuthSettings(currentProvider);
			Debug.println("  OAuth2 Step 2 OAuthz:  Using " + settings.id);
			// Debug.println(" OAuth2 Step 2 OAuthz: OAuthTokenLoc " +
			// settings.OAuthTokenLoc);

			// Debug.println(settings.OAuthTokenLoc);
			// Debug.println(settings.OAuthClientSecret);
			String oAuthCallbackURL = MainServicesConfigurator.getServerExternalURL() + defaultOAuthCallbackURL;
			if (settings.oauthCallbackURL != null && settings.oauthCallbackURL.length() > 0) {
				oAuthCallbackURL = settings.oauthCallbackURL;  
			}



			OAuthClientRequest tokenRequest = OAuthClientRequest.tokenLocation(settings.OAuthTokenLoc)
					.setGrantType(GrantType.AUTHORIZATION_CODE)
					.setRedirectURI(oAuthCallbackURL)
					.setCode(oar.getCode()).setScope(settings.OAuthClientScope).setClientId(settings.OAuthClientId)
					.setClientSecret(settings.OAuthClientSecret)

					.buildBodyMessage();

			OAuthClient oauthclient = new OAuthClient(new URLConnectionClient());

			// Debug.println(tokenRequest.getBody());
			//
			// Map<String, String> map = tokenRequest.getHeaders();
			//
			// for (Map.Entry<String, String> entry : map.entrySet()) {
			// System.out.println("Key : " + entry.getKey() +
			// " ,Value : " + entry.getValue());
			// }

			OAuthAccessTokenResponse oauth2Response = oauthclient.accessToken(tokenRequest);

			Debug.println("  OAuth2 Step 2 OAuthz:  Token request succeeded");

			Debug.println("  OAuth2 Step 2 OAuthz:  oauth2Response.getBody():" + oauth2Response.getBody());

			Debug.println("  OAuth2 Step 2 OAuthz:  ACCESS TOKEN:" + oauth2Response.getAccessToken());

			Debug.println("Found stateID [" + stateID + "]");
			StateObject stateObject = oauthStatesMapper.get(stateID);

			if (oauthStatesMapper.remove(stateID) == null) {
				Debug.println("Could not remove stateID [" + stateID + "]");
			} else {
				Debug.println("removeD stateID [" + stateID + "]");
			}

			if (stateObject == null) {
				throw new Exception("  OAuth2 Step 2 OAuthz:  Given STATE parameter is not matching, incorrect!!!");
			} else {
				Debug.println("  OAuth2 Step 2 OAuthz:  Found state object with key " + stateID);
			}

			handleSpecificProviderCharacteristics(request, settings, oauth2Response);

			String returnURL = stateObject.returnURL;
			if (returnURL.equals("") == false) {
				Debug.println("Returning to " + returnURL);
				response.sendRedirect(returnURL);
			} else {
				response.sendRedirect("http://returnURLNotDefined.knmi.nl:1");
			}

		} catch (Exception e) {
			request.getSession().setAttribute("message", "Error in OAuth2 service:\n" + e.getMessage());
			try {
				Debug.errprintln("Error in OAuth2 service");
				response.sendRedirect("/impactportal/exception.jsp");
			} catch (IOException e1) {
				
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	};

	/**
	 * All providers are handled a bit different. One of them is CEDA, which
	 * offers a certificate issuing service for ESGF.
	 * 
	 * @param request
	 * @param settings
	 * @param oauth2Response
	 * @throws Exception
	 */
	private static void handleSpecificProviderCharacteristics(HttpServletRequest request, Oauth2Settings settings,
			OAuthAccessTokenResponse oauth2Response) throws Exception {
		if (settings.id.equals("ceda")) {

			UserInfo userInfo = makeSLCSCertificateRequest(settings.id,oauth2Response.getAccessToken());
			Token token = TokenManager.registerToken(UserManager.getUser(userInfo.user_identifier));
			ObjectMapper om = new ObjectMapper();
			String result = om.writeValueAsString(token);
			Debug.println(result);
			JSONObject accessToken1 = new JSONObject(result);
			if (accessToken1.has("error")) {
				Debug.errprintln("Error getting user cert: " + accessToken1.toString());
				request.getSession().setAttribute("services_access_token", accessToken1.toString());
			} else {
				Debug.println("makeUserCertificate succeeded: " + accessToken1.toString());
				request.getSession().setAttribute("services_access_token", accessToken1.get("token"));
				Debug.println("makeUserCertificate succeeded: " + accessToken1);
			}


			setSessionInfo(request, userInfo);
			return;
		}

		if (settings.id.equals("google")) {
			try {
				/* Google */
				String id_token = oauth2Response.getParam("id_token");

				if (id_token == null) {
					Debug.errprintln("ID TOKEN == NULL!");
				}
				if (id_token != null) {
					if (id_token.indexOf(".") != -1) {
						UserInfo userInfo = getIdentifierFromJWTPayload(
								TokenDecoder.base64Decode(id_token.split("\\.")[1]));

						if (userInfo == null) {
							Debug.errprintln(
									"Error in OAuth2 service getIdentifierFromJWTPayload failed. Check logs!!!");
							return;
						}
						setSessionInfo(request, userInfo);

						try {
							makeUserCertificate(User.makePosixUserId(userInfo.user_identifier));
							Token token = TokenManager.registerToken(UserManager.getUser(userInfo.user_identifier));
							ObjectMapper om = new ObjectMapper();
							String result = om.writeValueAsString(token);
							Debug.println(result);
							JSONObject accessToken = new JSONObject(result);
							if (accessToken.has("error")) {
								Debug.errprintln("Error getting user cert: " + accessToken.toString());
								request.getSession().setAttribute("services_access_token", accessToken.toString());
							} else {
								Debug.println("makeUserCertificate succeeded: " + accessToken.toString());
								request.getSession().setAttribute("services_access_token", accessToken.get("token"));
								Debug.println("makeUserCertificate succeeded: " + accessToken);
							}

						} catch (Exception e) {
							request.getSession().setAttribute("services_access_token", null);
							Debug.errprintln("makeUserCertificate Failed");
							Debug.printStackTrace(e);
						}

						String accessToken = oauth2Response.getAccessToken();
						Debug.println("ACCESS TOKEN:" + accessToken);
						Debug.println("EXPIRESIN:" + oauth2Response.getExpiresIn());

					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		Debug.errprintln("Provider with id [" + settings.id + "] not recognized");
	};

	/**
	 * Step 3 - Make SLCS certificate request to external OAuth2 service A -
	 * generate keypair B - generate certificate signing request (CSR) C -
	 * Request certificate from CEDA service using CSR and access_token D -
	 * Retrieve user identifier from retrieved SLCS
	 * 
	 * @param currentProvider
	 * @param accessToken
	 * @return
	 * @throws IOException 
	 * @throws OperatorCreationException 
	 * @throws NoSuchAlgorithmException 
	 * @throws ElementNotFoundException 
	 * @throws CertificateException 
	 * @throws Exception
	 */

	private static UserInfo makeSLCSCertificateRequest(String id, String accessToken) throws IOException, OperatorCreationException, NoSuchAlgorithmException, ElementNotFoundException, CertificateException {
		Debug .println("Step 3 - Make SLCS certificate request to external OAuth2 service");

		UserInfo userInfo = new UserInfo();
		userInfo.user_identifier = null;//retrieved from slc x509 CN

		/* Step 1 - Initialize security provider and key generator*/
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		int keySize = 2048;
		KeyPairGenerator keyGenkeyGeneratorRSA = KeyPairGenerator.getInstance("RSA");
		keyGenkeyGeneratorRSA.initialize(keySize, new SecureRandom());

		/* Step 2 - Generate KeyPair for CSR */
		KeyPair keyPairCSR = keyGenkeyGeneratorRSA.generateKeyPair();

		/* Step 3 - Generate CSR */
		PKCS10CertificationRequest csr = PemX509Tools.createCSR("CN=Requested Test Certificate", keyPairCSR);

		/* Create post body */
		String postData = "certificate_request=" + URLEncoder.encode(PemX509Tools.certificateToPemString(csr), "UTF-8");

		String shortLivedCertFromOAuthServiceInPemFormat = null;

		/* Make HTTP post request to CEDA OAuth service with short lived certificate service */
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			HttpPost httppost = new HttpPost("https://slcs.ceda.ac.uk/oauth/certificate/");
			httppost.addHeader("Authorization", "Bearer " + accessToken);
			httppost.addHeader("Content-Type","application/x-www-form-urlencoded ");
			httppost.setEntity(new StringEntity(postData));
			CloseableHttpResponse response = httpclient.execute(httppost);
			try {
				shortLivedCertFromOAuthServiceInPemFormat = EntityUtils.toString(response.getEntity()); 
			} finally {
				response.close();
			}
		} finally {
			httpclient.close();
		}	    	 

		if (shortLivedCertFromOAuthServiceInPemFormat != null) {
			Debug.println("Succesfully retrieved an SLCS\n");
		}else{
			throw new IOException("Unable to retrieve SLC from SLCS");
		}
		
		/* Convert the certificate in PEM format to a X509Certificate object */
		Debug.println(shortLivedCertFromOAuthServiceInPemFormat);
		X509Certificate cert = PemX509Tools.readCertificateFromPEMString(shortLivedCertFromOAuthServiceInPemFormat);
		
		/* Get user id from certificate */
		String identifierFromSLCSCertificate = new PemX509Tools().getUserIdFromSubjectDN(cert.getSubjectDN().toString());
		
		/* Set user id in userInfo object */		
		userInfo.user_identifier = User.makePosixUserId(identifierFromSLCSCertificate);
		userInfo.user_openid = identifierFromSLCSCertificate;
		
		/* Find user object matching to openid */
		User user = UserManager.getUser(identifierFromSLCSCertificate);
		
		/* Write cert and private key to disk and configure netcdf library */
		X509UserCertAndKey userCert = new PemX509Tools().new X509UserCertAndKey(cert,keyPairCSR.getPrivate());
		user.setCertificate(userCert);
		return userInfo;
	}

	/**
	 * Sets session parameters for the impactportal
	 * 
	 * @param request
	 * @param userInfo
	 * @throws JSONException
	 */
	public static void setSessionInfo(HttpServletRequest request, UserInfo userInfo) throws JSONException {
		request.getSession().setAttribute("openid_identifier", userInfo.user_openid);
		request.getSession().setAttribute("user_identifier", userInfo.user_identifier);
		request.getSession().setAttribute("emailaddress", userInfo.user_email);
		request.getSession().setAttribute("certificate", userInfo.certificate);
		request.getSession().setAttribute("oauth_access_token", userInfo.oauth_access_token);
		request.getSession().setAttribute("login_method", "oauth2");

	};

	public static int makeUserCertificate(String clientId) throws CertificateException, IOException,
	InvalidKeyException, NoSuchAlgorithmException, OperatorCreationException, KeyManagementException,
	UnrecoverableKeyException, KeyStoreException, NoSuchProviderException, SignatureException, GSSException,
	ElementNotFoundException, CertificateVerificationException, JSONException {

		User user = UserManager.getUser(clientId);
		Debug.println("Making user cert for " + clientId);
		X509Certificate caCertificate = PemX509Tools.readCertificateFromPEMFile(SecurityConfigurator.getCACertificate());
		PrivateKey privateKey = PemX509Tools.readPrivateKeyFromPEM(SecurityConfigurator.getCAPrivateKey());
		X509UserCertAndKey userCert = new PemX509Tools().setupSLCertificateUser(clientId, caCertificate, privateKey);
		user.setCertificate(userCert);

		Debug.println("Created user cert");
		return 0;
	}

	/**
	 * Verifies a signed JWT Id_token with RSA SHA-256
	 * 
	 * @param id_token
	 * @return true if verified
	 * @throws JSONException
	 * @throws WebRequestBadStatusException
	 * @throws IOException
	 * @throws SignatureException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	@SuppressWarnings("unused")
	private static boolean verify_JWT_IdToken(String id_token) throws JSONException, WebRequestBadStatusException,
	IOException, SignatureException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException {
		// http://self-issued.info/docs/draft-jones-json-web-token-01.html#DefiningRSA
		// The JWT Signing Input is always the concatenation of a JWT Header
		// Segment, a period ('.') character, and the JWT Payload Segment
		// RSASSA-PKCS1-V1_5-VERIFY

		// 8.2. Signing a JWT with RSA SHA-256
		//
		// This section defines the use of the RSASSA-PKCS1-v1_5 signature
		// algorithm
		// as defined in RFC 3447 [RFC3447], Section 8.2 (commonly known as
		// PKCS#1),
		// using SHA-256 as the hash function. Note that the use of the
		// RSASSA-PKCS1-v1_5 algorithm is described in FIPS 186-3 [FIPS.186‑3],
		// Section 5.5, as is the SHA-256 cryptographic hash function, which is
		// defined in FIPS 180-3 [FIPS.180‑3]. The reserved "alg" header
		// parameter
		// value "RS256" is used in the JWT Header Segment to indicate that the
		// JWT
		// Crypto Segment contains an RSA SHA-256 signature.
		//
		// A 2048-bit or longer key length MUST be used with this algorithm.
		//
		// The RSA SHA-256 signature is generated as follows:
		//
		// Let K be the signer's RSA private key and let M be the UTF-8
		// representation of the JWT Signing Input.
		// Compute the octet string S = RSASSA-PKCS1-V1_5-SIGN (K, M) using
		// SHA-256
		// as the hash function.
		// Base64url encode the octet string S, as defined in this document.
		//
		// The output is placed in the JWT Crypto Segment for that JWT.
		//
		// The RSA SHA-256 signature on a JWT is validated as follows:
		//
		// Take the JWT Crypto Segment and base64url decode it into an octet
		// string
		// S. If decoding fails, then the token MUST be rejected.
		// Let M be the UTF-8 representation of the JWT Signing Input and let
		// (n, e)
		// be the public key corresponding to the private key used by the
		// signer.
		// Validate the signature with RSASSA-PKCS1-V1_5-VERIFY ((n, e), M, S)
		// using
		// SHA-256 as the hash function.
		// If the validation fails, the token MUST be rejected.
		//
		// Signing with the RSA SHA-384 and RSA SHA-512 algorithms is performed
		// identically to the procedure for RSA SHA-256 - just with
		// correspondingly
		// longer key and result values.

		Debug.println("Starting verification of id_token");
		Debug.println("[" + id_token + "]");
		String JWTHeader = TokenDecoder.base64Decode(id_token.split("\\.")[0]);
		String JWTPayload = TokenDecoder.base64Decode(id_token.split("\\.")[1]);
		String JWTSigningInput = id_token.split("\\.")[0] + "." + id_token.split("\\.")[1];
		String JWTSignature = id_token.split("\\.")[2];

		Debug.println("Decoded JWT IDToken Header:" + JWTHeader);
		Debug.println("Decoded JWT IDToken Payload:" + JWTPayload);

		// Find the discovery page
		JSONObject JWTPayLoadObject = (JSONObject) new JSONTokener(JWTPayload).nextValue();
		String iss = JWTPayLoadObject.getString("iss");
		Debug.println("iss=" + iss);

		// Load the OpenId discovery page
		String discoveryURL = "https://" + iss + "/.well-known/openid-configuration";
		JSONObject openid_configuration = (JSONObject) new JSONTokener(HTTPTools.makeHTTPGetRequest(discoveryURL))
				.nextValue();
		String jwks_uri = openid_configuration.getString("jwks_uri");
		Debug.println("jwks_uri:" + jwks_uri);

		// Load the jwks uri
		JSONObject certs = (JSONObject) new JSONTokener(HTTPTools.makeHTTPGetRequest(jwks_uri)).nextValue();
		JSONArray jwks_keys = certs.getJSONArray("keys");
		Debug.println("jwks_keys:" + jwks_keys.length());

		JSONObject JWTHeaderObject = (JSONObject) new JSONTokener(JWTHeader).nextValue();
		String kid = JWTHeaderObject.getString("kid");
		Debug.println("kid=" + kid);

		String modulus = null;
		String exponent = null;

		for (int j = 0; j < jwks_keys.length(); j++) {
			if (jwks_keys.getJSONObject(j).getString("kid").equals(kid)) {
				Debug.println("Found kid in jwks");
				modulus = jwks_keys.getJSONObject(j).getString("n");
				exponent = jwks_keys.getJSONObject(j).getString("e");
				break;
			}
		}
		return RSASSA_PKCS1_V1_5_VERIFY(modulus, exponent, JWTSigningInput, JWTSignature);
	};

	/**
	 * RSASSA-PKCS1-V1_5-VERIFY ((n, e), M, S) using SHA-256
	 * 
	 * @param modulus_n
	 * @param exponent_e
	 * @param signinInput_M
	 * @param signature_S
	 * @return
	 * @throws SignatureException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	static boolean RSASSA_PKCS1_V1_5_VERIFY(String modulus_n, String exponent_e, String signinInput_M,
			String signature_S)
					throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException {
		Debug.println("Starting verification");
		/* RSA SHA-256 RSASSA-PKCS1-V1_5-VERIFY */
		// Modulus (n from https://www.googleapis.com/oauth2/v2/certs)
		String n = modulus_n;
		// Exponent (e from https://www.googleapis.com/oauth2/v2/certs)
		String e = exponent_e;
		// The JWT Signing Input (JWT Header and JWT Payload concatenated with
		// ".")
		byte[] M = signinInput_M.getBytes();
		// Signature (JWT Crypto)
		byte[] S = Base64.decodeBase64(signature_S);

		byte[] modulusBytes = Base64.decodeBase64(n);
		byte[] exponentBytes = Base64.decodeBase64(e);
		BigInteger modulusInteger = new BigInteger(1, modulusBytes);
		BigInteger exponentInteger = new BigInteger(1, exponentBytes);

		RSAPublicKeySpec rsaPubKey = new RSAPublicKeySpec(modulusInteger, exponentInteger);
		KeyFactory fact = KeyFactory.getInstance("RSA");
		PublicKey pubKey = fact.generatePublic(rsaPubKey);
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initVerify(pubKey);
		signature.update(M);
		boolean isVerified = signature.verify(S);
		Debug.println("Verify result [" + isVerified + "]");
		return isVerified;
	}

	/**
	 * Returns unique user identifier from id_token (JWTPayload). The JWT token
	 * is *NOT* verified. Several impact portal session attributes are set: -
	 * user_identifier - emailaddress
	 * 
	 * @param request
	 * @param JWT
	 * @return
	 * @throws ElementNotFoundException
	 */
	private static UserInfo getIdentifierFromJWTPayload(String JWT) throws ElementNotFoundException, IOException{
		JSONObject id_token_json = null;
		try {
			id_token_json = (JSONObject) new JSONTokener(JWT).nextValue();
		} catch (JSONException e1) {
			Debug.errprintln("Unable to convert JWT Token to JSON");
			return null;
		}

		String email = "null";
		String userSubject = null;
		String aud = "";
		try {
			email = id_token_json.get("email").toString();
		} catch (JSONException e) {
		}
		try {
			userSubject = id_token_json.get("sub").toString();
		} catch (JSONException e) {
		}

		try {
			aud = id_token_json.get("aud").toString();
		} catch (JSONException e) {
		}

		if (aud == null) {
			Debug.errprintln("Error: aud == null");
			return null;
		}
		if (userSubject == null) {
			Debug.errprintln("Error: userSubject == null");
			return null;
		}

		// Get ID based on aud (client id)
		String clientId = null;

		Vector<String> providernames = OAuthConfigurator.getProviders();

		for (int j = 0; j < providernames.size(); j++) {
			OAuthConfigurator.Oauth2Settings settings = OAuthConfigurator.getOAuthSettings(providernames.get(j));
			if (settings.OAuthClientId.equals(aud)) {
				clientId = settings.id;
			}
		}

		if (clientId == null) {
			Debug.errprintln("Error: could not match OAuthClientId to aud");
			return null;

		}

		String user_identifier = clientId + "/" + userSubject;
		String user_openid = null;
		UserInfo userInfo = new UserInfo();
		userInfo.user_identifier = user_identifier;
		userInfo.user_openid = user_openid;
		userInfo.user_email = email;

		Debug.println("getIdentifierFromJWTPayload (id_token): Found unique ID " + user_identifier);

		return userInfo;

	}

	/**
	 * Makes a JSON object and sends it to response with information needed for
	 * building the OAuth2 login form.
	 * 
	 * @param request
	 * @param response
	 * @throws ElementNotFoundException
	 */
	private static void makeForm(HttpServletRequest request, HttpServletResponse response)
			throws ElementNotFoundException, IOException {
		JSONResponse jsonResponse = new JSONResponse(request);

		JSONObject form = new JSONObject();
		try {

			JSONArray providers = new JSONArray();
			form.put("providers", providers);
			Vector<String> providernames = OAuthConfigurator.getProviders();

			for (int j = 0; j < providernames.size(); j++) {
				OAuthConfigurator.Oauth2Settings settings = OAuthConfigurator.getOAuthSettings(providernames.get(j));
				JSONObject provider = new JSONObject();
				provider.put("id", providernames.get(j));
				provider.put("description", settings.description);
				provider.put("logo", settings.logo);
				provider.put("registerlink", settings.registerlink);
				providers.put(provider);

			}
		} catch (JSONException e) {
		}
		jsonResponse.setMessage(form);

		try {
			jsonResponse.print(response);
		} catch (Exception e1) {

		}

	};

	/*
	 * Check if an access token was provided in the HttpServletRequest object
	 * and return a user identifier on success.
	 *
	 * It returns the unique user identifier. It does this by calling the
	 * userinfo_endpoint using the access_token. All endpoints are discovered by
	 * reading the open-id Discovery service. This is one of the OpenId-Connect
	 * extensions on OAuth2
	 *
	 * @param request
	 * 
	 * @return
	 * 
	 * @throws JSONException
	 * 
	 * @throws WebRequestBadStatusException
	 * 
	 * @throws IOException
	 */
	public static UserInfo verifyAndReturnUserIdentifier(HttpServletRequest request)
			throws JSONException, WebRequestBadStatusException, IOException, ElementNotFoundException {

		// 1) Find the Authorization header containing the access_token
		String access_token = request.getHeader("Authorization");
		if (access_token == null) {
			// No access token, probably not an OAuth2 request, skip.
			return null;
		}
		Debug.println("Authorization    : " + access_token);

		// 2) Find the Discovery service, it might have been passed in the
		// request headers:
		String discoveryURL = request.getHeader("Discovery");
		if (discoveryURL == null) {
			discoveryURL = "https://accounts.google.com/.well-known/openid-configuration";
		}
		Debug.println("Discovery        : " + discoveryURL);

		// 3 Retrieve the Discovery service, so we get all service endpoints
		String discoveryData = HTTPTools.makeHTTPGetRequest(discoveryURL);
		JSONObject jsonObject = (JSONObject) new JSONTokener(discoveryData).nextValue();

		// 4) Retrieve userinfo endpoint
		String userInfoEndpoint = jsonObject.getString("userinfo_endpoint");
		Debug.println("userInfoEndpoint:" + userInfoEndpoint);

		// 5) Make a get request with Authorization headers set, the
		// access_token is used here as Bearer.
		KVPKey key = new KVPKey();
		key.addKVP("Authorization", access_token);
		Debug.println("Starting request");
		String id_token = HTTPTools.makeHTTPGetRequestWithHeaders(userInfoEndpoint, key);// ,"Authorization:
		// Bearer
		// "+access_token);
		Debug.println("Finished request");

		// 6) The ID token is retrieved, now return the identifier from this
		// token.
		Debug.println("Retrieved id_token=" + id_token);
		return getIdentifierFromJWTPayload(id_token);
	};

}
