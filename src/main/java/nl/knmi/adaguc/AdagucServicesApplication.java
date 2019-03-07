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
		}
	}

	private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder builder){
		try {
			return builder.sources(AdagucServicesApplication.class).properties(getProperties()).bannerMode(Banner.Mode.OFF);
		} catch (ElementNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	static Properties getProperties() throws ElementNotFoundException {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		try {
			PyWPSInitializer.ConfigurePyWPS3();
		} catch (IOException e) {
			throw new ElementNotFoundException("Unable to create config file for PyWPS");
		}
		Properties props = new Properties();

		if(MainServicesConfigurator.getServerPort()!=null)props.put("server.port", MainServicesConfigurator.getServerPort());
		if(SecurityConfigurator.getKeyStore()!=null)props.put("server.ssl.key-store", SecurityConfigurator.getKeyStore());
		if(SecurityConfigurator.getEnableSSL()!=null && SecurityConfigurator.getEnableSSL().equals("true")) {
			props.put("server.ssl.enabled", true);
		} else {
			props.put("server.ssl.enabled", false);
		}
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
