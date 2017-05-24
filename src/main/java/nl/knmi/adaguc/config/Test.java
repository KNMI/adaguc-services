package nl.knmi.adaguc.config;

import nl.knmi.adaguc.services.adagucserver.ADAGUCConfigurator;
import nl.knmi.adaguc.tools.Debug;

public class Test {
	public static void main(String[] args) throws ConfigurationItemNotFoundException {
		Debug.println(ADAGUCConfigurator.getADAGUCExecutable());
		Debug.println(MainServicesConfigurator.getServerExternalURL());


	}
}
