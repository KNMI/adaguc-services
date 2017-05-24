package nl.knmi.adaguc.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.knmi.adaguc.security.PemX509Tools.X509Info;
import nl.knmi.adaguc.tools.Debug;

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
		
		// TODO Auto-generated method stub
		x509 = new PemX509Tools().getUserIdFromCertificate(request);
		if(x509 != null){
			Debug.println("OK!");
		}
	}
	
	public String getClientId(){
		if(x509 == null){
			return null;
		}
		return x509.getCN();
	}
	

}
