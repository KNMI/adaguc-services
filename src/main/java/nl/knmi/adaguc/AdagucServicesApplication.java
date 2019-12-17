package nl.knmi.adaguc;

import java.io.IOException;
import java.security.Security;
import java.util.Properties;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.security.SecurityConfigurator;
import nl.knmi.adaguc.services.pywpsserver.PyWPSInitializer;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;

@SpringBootApplication
public class AdagucServicesApplication extends SpringBootServletInitializer{

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application)  {
		try {
			return application.sources(AdagucServicesApplication.class).properties(getProperties());
		} catch (ElementNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Debug.println("Error");
		return null;
	}


	public static void main(String[] args) {
		// This task is scheduled to run every 10 seconds
		try {
			configureApplication(new SpringApplicationBuilder()).properties(getProperties()).run(args);
		} catch (ElementNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			Debug.errprintln(e.getMessage());
		}
	}

	private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder builder){
		try {
			return builder.sources(AdagucServicesApplication.class).properties(getProperties()).bannerMode(Banner.Mode.OFF);
		} catch (ElementNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	static Properties getProperties() throws ElementNotFoundException, IOException {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		try {
			PyWPSInitializer.ConfigurePyWPS3();
		}catch (IOException e1) {
			Debug.errprintln(e1.getMessage());
			System.exit(1);
		} catch (ElementNotFoundException e) {
			Debug.println("[INFO] Unable to create config file for PyWPS");
		}
		Properties props = new Properties();

		
		if(SecurityConfigurator.getKeyStore()!=null)props.put("server.ssl.key-store", SecurityConfigurator.getKeyStore());
		/*
		 * server.port is the default spring tomcat connector, it can be both used for http and https
		 * server.http.port is the adaguc-services added connector, it is meant for http only.
		 * For backwards compatibility with previous releases, http.port is used for https and server.http.port is used for http *
		 */
		if(SecurityConfigurator.getEnableSSL()!=null && SecurityConfigurator.getEnableSSL().equals("true")) {
			/* If SSL (HTTPS support) is enabled, configure http.port for https and optionaly server.http.port for http */
			props.put("server.ssl.enabled", true);
			if(MainServicesConfigurator.getServerPort()!=null)props.put("server.http.port", MainServicesConfigurator.getServerPort());
			if(MainServicesConfigurator.getServerPortHTTPS()!=null)props.put("server.port", MainServicesConfigurator.getServerPortHTTPS()); else
				if(MainServicesConfigurator.getServerPort()!=null)props.put("server.port", MainServicesConfigurator.getServerPort());
		} else {
			/* If SSL (HTTPS support) is not enabled, configure as normal */
			props.put("server.ssl.enabled", false);
			if(MainServicesConfigurator.getServerPort()!=null)props.put("server.port", MainServicesConfigurator.getServerPort());
			if(MainServicesConfigurator.getServerPort()!=null)props.put("server.http.port", MainServicesConfigurator.getServerPort());
		}
		
		if(MainServicesConfigurator.getContextPath()!=null)props.put("server.servlet.context-path", MainServicesConfigurator.getContextPath());
		if(SecurityConfigurator.getKeyStorePassword()!=null)props.put("server.ssl.key-store-password", SecurityConfigurator.getKeyStorePassword());
		if(SecurityConfigurator.getKeyStoreType()!=null)props.put("server.ssl.keyStoreType",SecurityConfigurator.getKeyStoreType());
		if(SecurityConfigurator.getKeyAlias()!=null)props.put("server.ssl.keyAlias", SecurityConfigurator.getKeyAlias());
		if(SecurityConfigurator.getTrustStore()!=null)props.put("server.ssl.trust-store", SecurityConfigurator.getTrustStore());
		if(SecurityConfigurator.getTrustStorePassword()!=null)props.put("server.ssl.trust-store-password", SecurityConfigurator.getTrustStorePassword());
		props.put("server.ssl.client-auth", "want");
		props.put("spring.http.multipart.max-file-size","100MB");
		props.put("spring.http.multipart.max-request-size","100MB");
		return props;
	}

	
	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> 
	    containerCustomizer(){
	    return new EmbeddedTomcatCustomizer();
	}

	private static class EmbeddedTomcatCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
	    @Override
	    public void customize(TomcatServletWebServerFactory factory) {
	        factory.addConnectorCustomizers((TomcatConnectorCustomizer) connector -> {
	            connector.setAttribute("relaxedPathChars", "<>[\\]^`{|}");
	            connector.setAttribute("relaxedQueryChars", "<>[\\]^`{|}");
	        });
	    }
	}	
}
