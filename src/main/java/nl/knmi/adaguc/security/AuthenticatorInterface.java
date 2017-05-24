package nl.knmi.adaguc.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface AuthenticatorInterface {

	void init(HttpServletRequest request);
	public String getClientId();
}
