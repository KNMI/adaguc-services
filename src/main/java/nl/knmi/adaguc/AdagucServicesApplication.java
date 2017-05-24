package nl.knmi.adaguc;

import java.security.Security;
import java.util.Properties;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

import nl.knmi.adaguc.config.ConfigurationItemNotFoundException;
import nl.knmi.adaguc.config.ConfigurationReader;
import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.security.SecurityConfigurator;
import nl.knmi.adaguc.tools.Debug;

@SpringBootApplication
public class AdagucServicesApplication extends SpringBootServletInitializer{

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application)  {
		try {
			return application.sources(AdagucServicesApplication.class).properties(getProperties());
		} catch (ConfigurationItemNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Debug.println("Error");
		return null;
	}


	public static void main(String[] args) throws ConfigurationItemNotFoundException {
		try{
			ConfigurationReader.readConfig();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		configureApplication(new SpringApplicationBuilder()).properties(getProperties()).run(args);
	}

	private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder builder) throws ConfigurationItemNotFoundException {
		return builder.sources(AdagucServicesApplication.class).properties(getProperties()).bannerMode(Banner.Mode.OFF);
	}

	static Properties getProperties() throws ConfigurationItemNotFoundException {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		
		Properties props = new Properties();

		props.put("server.port", MainServicesConfigurator.getServerPort());
		props.put("server.ssl.key-store", SecurityConfigurator.getKeyStore());
		props.put("server.ssl.key-store-password", SecurityConfigurator.getKeyStorePassword());
		props.put("server.ssl.keyStoreType",SecurityConfigurator.getKeyStoreType());
		props.put("server.ssl.keyAlias", SecurityConfigurator.getKeyAlias());
		props.put("server.ssl.trust-store", SecurityConfigurator.getTrustStore());
		props.put("server.ssl.trust-store-password", SecurityConfigurator.getTrustStorePassword());
		props.put("server.ssl.client-auth", "want");

		props.put("log4j.logger.httpclient.wire.header","WARN");
		props.put("log4j.logger.httpclient.wire.content","WARN");
		props.put("log4j.rootLogger","ERROR");

		props.put("log4j.appender.stdout","org.apache.log4j.ConsoleAppender");
		props.put("log4j.appender.stdout.layout","org.apache.log4j.PatternLayout");
		props.put("log4j.appender.stdout.layout.ConversionPattern","%5p [%c] %m%n");

		props.put("log4j.logger.org.apache.http","ERROR");
		props.put("log4j.logger.org.apache.http.wire","ERROR");
		props.put("log4j.logger.org.apache","ERROR");

		return props;
	}

}
