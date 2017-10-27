package nl.knmi.adaguc.services.pywpsserver;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.Tools;

public class PyWPSInitializer {
	public static void ConfigurePyWPS3 () throws ElementNotFoundException, IOException{
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
			throw new ElementNotFoundException("adaguc-services.pywps-server.pywpsconfigtemplate is invalid ["+pyWPSConfigTemplate+"]");
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
