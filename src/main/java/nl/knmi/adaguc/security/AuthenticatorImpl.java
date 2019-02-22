package nl.knmi.adaguc.security;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.ietf.jgss.GSSException;
import org.springframework.security.core.AuthenticationException;

import nl.knmi.adaguc.security.PemX509Tools.X509Info;
import nl.knmi.adaguc.security.PemX509Tools.X509UserCertAndKey;
import nl.knmi.adaguc.security.token.Token;
import nl.knmi.adaguc.security.token.TokenManager;
import nl.knmi.adaguc.security.user.User;
import nl.knmi.adaguc.security.user.UserManager;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.HTTPTools;

public class AuthenticatorImpl implements AuthenticatorInterface{

	X509Info x509 = null;
	public AuthenticatorImpl(HttpServletRequest request) {
		init(request);
	}

	public AuthenticatorImpl() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public synchronized void init(HttpServletRequest request) {
		if (request == null ) {
			return;
		}
		/* Get user from session */
		String sessionId = null;
		HttpSession session = request.getSession();
		if (session!=null) {
			sessionId = (String) session.getAttribute("user_identifier");
		}
		if (sessionId!=null) {
			x509 = new PemX509Tools().new X509Info(sessionId, sessionId);
			Debug.println("Got userid from session");
			return;
		}

		/* Get user from header (Set by SSL client cert verification in NGINX)*/
		try {
//			Enumeration headerNames = request.getHeaderNames();
//			while(headerNames.hasMoreElements()) {
//			  String headerName = (String)headerNames.nextElement();
//			  Debug.println("" + headerName);
//			  Debug.println("" + request.getHeader(headerName));
//			}
			String userHeader = SecurityConfigurator.getUserHeader();
			if (userHeader != null) {
				String userIdFromHeader = request.getHeader(userHeader);
				if (userIdFromHeader != null && userIdFromHeader.length() > 4) {
					String userID = new PemX509Tools().getUserIdFromSubjectDN(userIdFromHeader);
					Debug.println("Found user from header: " + userID);
					x509 = new PemX509Tools().new X509Info(userID, userID);
					return;
				}
			}
		} catch (ElementNotFoundException e) {
		}

		x509 = new PemX509Tools().getUserIdFromCertificate(request);
		Debug.println("No user info found from certificates");
		if(x509 == null){
			String path = request.getServletPath();

			String tokenStr = new TokenManager().getTokenFromPath(path);

			if(tokenStr == null){
				try {
					tokenStr = HTTPTools.getHTTPParam(request, "key");
				} catch (Exception e1) {
					Debug.println("No access token set in URL via key=<accesstoken> KVP");
				}
			}

			if(tokenStr!=null){
				Token token = null;
				try {
					token = TokenManager.getToken(tokenStr);
					//					Debug.println("Found token "+token);
					x509 = new PemX509Tools().new X509Info(token.getUserId(), token.getToken());
					//					Debug.println("Found user "+x509.getCN());
				} catch (AuthenticationException | IOException | ElementNotFoundException e1) {
					// TODO Auto-generated catch block
					Debug.printStackTrace(e1);
				}



			}else{
				Debug.println("Unable to find user info from certificate or accesstoken");
			}



		}

	}

	public String getClientId(){
		if(x509 == null){
			return null;
		}
		return x509.getCN();
	}


}
