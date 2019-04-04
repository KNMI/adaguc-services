package nl.knmi.adaguc;

import java.io.IOException;
import java.security.Security;
import java.util.Properties;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Debug.println("Error");
		return null;
	}


	public static void main(String[] args) {
//		try{
//			ConfigurationReader.readConfig(false);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		try {
			configureApplication(new SpringApplicationBuilder()).properties(getProperties()).run(args);
		} catch (ElementNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			Debug.errprintln(e.getMessage());
		}
	}

	private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder builder){
		try {
			return builder.sources(AdagucServicesApplication.class).properties(getProperties()).bannerMode(Banner.Mode.OFF);
		} catch (ElementNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
//		Debug.println("MainServicesConfigurator.getServerPort()" + MainServicesConfigurator.getServerPort());
//		Debug.println("MainServicesConfigurator.getServerPortHTTPS()" + MainServicesConfigurator.getServerPortHTTPS());
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

//		props.put("log4j.logger.httpclient.wire.header","WARN");
//		props.put("log4j.logger.httpclient.wire.content","WARN");
//		props.put("log4j.rootLogger","ERROR");
//
//		props.put("log4j.appender.stdout","org.apache.log4j.ConsoleAppender");
//		props.put("log4j.appender.stdout.layout","org.apache.log4j.PatternLayout");
//		props.put("log4j.appender.stdout.layout.ConversionPattern","%5p [%c] %m%n");
//
//		props.put("log4j.logger.org.apache.http","ERROR");
//		props.put("log4j.logger.org.apache.http.wire","ERROR");
//		props.put("log4j.logger.org.apache","ERROR");
		
//		Debug.errprintln("SecurityConfigurator.getTrustStore()" + SecurityConfigurator.getTrustStore());
//		
//		Enumeration e = props.propertyNames();
//	    while (e.hasMoreElements()) {
//	      String key = (String) e.nextElement();
//	      System.out.println(key + " -- " + props.getProperty(key));
//	    }
		return props;
	}

	
	
	
}
