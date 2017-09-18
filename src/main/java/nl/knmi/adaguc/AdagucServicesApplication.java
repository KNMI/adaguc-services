package nl.knmi.adaguc;

import java.io.IOException;
import java.security.Security;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

import nl.knmi.adaguc.config.ConfigurationItemNotFoundException;
import nl.knmi.adaguc.config.ConfigurationReader;
import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.security.SecurityConfigurator;
import nl.knmi.adaguc.services.pywpsserver.PyWPSConfigurator;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;

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


	public static void main(String[] args) {
		try{
			ConfigurationReader.readConfig();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			configureApplication(new SpringApplicationBuilder()).properties(getProperties()).run(args);
		} catch (ConfigurationItemNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder builder){
		try {
			return builder.sources(AdagucServicesApplication.class).properties(getProperties()).bannerMode(Banner.Mode.OFF);
		} catch (ConfigurationItemNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	static Properties getProperties() throws ConfigurationItemNotFoundException {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		try {
			ConfigurePyWPS();
		} catch (IOException e) {
			throw new ConfigurationItemNotFoundException("Unable to create config file for PyWPS");
		}
		Properties props = new Properties();

		if(MainServicesConfigurator.getServerPort()!=null)props.put("server.port", MainServicesConfigurator.getServerPort());
		if(SecurityConfigurator.getKeyStore()!=null)props.put("server.ssl.key-store", SecurityConfigurator.getKeyStore());
		if( SecurityConfigurator.getKeyStorePassword()!=null)props.put("server.ssl.key-store-password", SecurityConfigurator.getKeyStorePassword());
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

		return props;
	}

	
	static void ConfigurePyWPS () throws ConfigurationItemNotFoundException, IOException{
		String pyWPSConfigTemplate = PyWPSConfigurator.getPyWPSConfigTemplate();
		if(pyWPSConfigTemplate == null){
			return;
		}
		String tempDir = PyWPSConfigurator.getTempDir();
		String pyWPSOutputDir = PyWPSConfigurator.getPyWPSOutputDir();
		String homeURL=MainServicesConfigurator.getServerExternalURL();
		String pyWPSServerURL = homeURL+"/wps";
		String configTemplate = Tools.readFile(pyWPSConfigTemplate);
		String pyWPSConfig = PyWPSConfigurator.getPyWPSConfig();
		String pyWPSProcessesDir = PyWPSConfigurator.getPyWPSProcessesDir();
		if(configTemplate == null){
			throw new ConfigurationItemNotFoundException("adaguc-services.pywps-server.pywpsconfigtemplate is invalid ["+pyWPSConfigTemplate+"]");
		}
		String[] configLines = configTemplate.split("\n");

		for( int j=0;j< configLines.length;j++){
			String line = configLines[j];
			if(line.startsWith("serveraddress")){
				configLines[j]="serveraddress=" + pyWPSServerURL;
			}
			if(line.startsWith("tempPath")){
				configLines[j]="tempPath=" + tempDir;
			}
			if(line.startsWith("outputUrl")){
				configLines[j]="outputUrl=" + pyWPSServerURL+"?OUTPUT=";
			}
			if(line.startsWith("outputPath")){
				configLines[j]="outputPath=" + pyWPSOutputDir;
			}
			if(line.startsWith("outputPath")){
				configLines[j]="outputPath=" + pyWPSOutputDir;
			}
			if(line.startsWith("processesPath")){
				configLines[j]="processesPath=" + pyWPSProcessesDir;
			}
			if(line.startsWith("maxinputparamlength")){
				configLines[j]="maxinputparamlength=32768";
			}
			if(line.startsWith("maxfilesize")){
				configLines[j]="maxfilesize=500mb";
			}
			if(line.startsWith("logFile")){
				configLines[j]="logFile="+tempDir+"/pywps.log";
			}
			if(line.startsWith("logLevel")){
				configLines[j]="logLevel=DEBUG";
			}
		}


		String newConfig = StringUtils.join(configLines, "\n");
		Tools.writeFile(pyWPSConfig, newConfig);
	
	}
	
}
