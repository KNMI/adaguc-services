package nl.knmi.adaguc.security.user;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.operator.OperatorCreationException;
import org.ietf.jgss.GSSException;
import org.json.JSONException;
import org.springframework.security.core.AuthenticationException;

import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.security.AuthenticationExceptionImpl;
import nl.knmi.adaguc.security.AuthenticatorFactory;
import nl.knmi.adaguc.security.AuthenticatorInterface;
import nl.knmi.adaguc.security.CertificateVerificationException;
import nl.knmi.adaguc.security.PemX509Tools;
import nl.knmi.adaguc.security.SecurityConfigurator;
import nl.knmi.adaguc.security.PemX509Tools.X509UserCertAndKey;
import nl.knmi.adaguc.services.oauth2.OAuth2Handler;



public class UserManager {
	private static Map<String, User> users = new ConcurrentHashMap<String,User>();
	  
	public synchronized static User getUser(String id) throws IOException, ElementNotFoundException, AuthenticationException{
		if(id == null){
			throw new AuthenticationExceptionImpl("No user information provided");
		}
		String harmonizedId = harmonizeUserId(id);
		
		User user = users.get(harmonizedId);
		if(user == null){
			users.put(harmonizedId, new User(id));
			return getUser(id);
		}
		return user;
	}

	private static String harmonizeUserId(String id) {
		return User.makePosixUserId(id);
	}

	public synchronized static User getUser(AuthenticatorInterface authenticator) throws IOException, ElementNotFoundException, AuthenticationException {
		return getUser(authenticator.getClientId());
	}
	
	public static String makeGetRequestWithUserFromServletRequest (HttpServletRequest servletRequest, String requestStr) throws ElementNotFoundException, AuthenticationException, IOException, KeyManagementException, UnrecoverableKeyException, InvalidKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, NoSuchProviderException, SignatureException, GSSException {
		String ts = SecurityConfigurator.getTrustStore();

		char [] tsPass = SecurityConfigurator.getTrustStorePassword().toCharArray();
		
		Debug.println("Running remote adaguc with truststore");

		X509UserCertAndKey userCertificate = null;
		
		AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(servletRequest);
		if(authenticator!=null){
			User user = UserManager.getUser(authenticator);
			if(user!=null){
				userCertificate = user.getCertificate();
				if (userCertificate == null) {
					try {
						OAuth2Handler.makeUserCertificate(user.userId);
					} catch (OperatorCreationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (CertificateVerificationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		if (userCertificate!=null) {
			Debug.println("Making request with user certificate");
		}
		
		
		CloseableHttpClient httpClient = (new PemX509Tools()).
				getHTTPClientForPEMBasedClientAuth(ts, tsPass, userCertificate);
		CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(requestStr));
		return EntityUtils.toString(httpResponse.getEntity());
	}
	


	
}
