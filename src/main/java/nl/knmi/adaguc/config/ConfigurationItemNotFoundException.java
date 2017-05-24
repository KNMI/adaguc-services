package nl.knmi.adaguc.config;

public class ConfigurationItemNotFoundException extends Exception{
	private String configItem = null;
	public ConfigurationItemNotFoundException(String string) {
		configItem = string;
	}
	
	 public String getMessage() {
	      
	        return "Configuration item missing or misconfigured: "+configItem;
	     }

	/**
	 * 
	 */
	private static final long serialVersionUID = 5420129800584623839L;

}
