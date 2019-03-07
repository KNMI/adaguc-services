package nl.knmi.adaguc.config;

public class ConfiguratorImpl {
	static boolean configDone = false;

	static public void setConfigDone() {
		configDone = true;
	}
	
	static public boolean getConfigDone() {
		return configDone;
	}
}
