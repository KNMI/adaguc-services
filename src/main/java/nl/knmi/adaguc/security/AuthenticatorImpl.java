package nl.knmi.adaguc.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.AuthenticationException;

import nl.knmi.adaguc.security.PemX509Tools.X509Info;
import nl.knmi.adaguc.security.token.Token;
import nl.knmi.adaguc.security.token.TokenManager;
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
	public void init(HttpServletRequest request) {
//		Debug.println("Init");
		x509 = new PemX509Tools().getUserIdFromCertificate(request);
		Debug.println("No user info found from certificates");
		if(x509 == null){
			String path = request.getServletPath();
			
		    String tokenStr = new TokenManager().getTokenFromPath(path);
		    
		    if(tokenStr == null){
			    try {
					tokenStr = HTTPTools.getHTTPParam(request, "key");
				} catch (Exception e) {
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
				} catch (AuthenticationException | IOException | ElementNotFoundException e) {
					// TODO Auto-generated catch block
					Debug.printStackTrace(e);
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
