package nl.knmi.adaguc.services.basket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import nl.knmi.adaguc.config.ConfigurationItemNotFoundException;
import nl.knmi.adaguc.config.ConfigurationReader;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;

public class BasketConfigurator implements nl.knmi.adaguc.config.ConfiguratorInterface{
	private static boolean enabled=false;
	@Override
	public void doConfig(XMLElement configReader) throws ConfigurationItemNotFoundException {
		if(configReader.getNodeValue("adaguc-services.basket") == null){
			return;
		}
		String enabledStr=configReader.getNodeValue("adaguc-services.basket.enabled");
		if(enabledStr != null && enabledStr.equals("true")){
			enabled = true;
		}
		
		if(enabled){
		}
	}
	
	public static boolean getEnabled() throws ConfigurationItemNotFoundException {
		ConfigurationReader.readConfig();
		return enabled;
	}
	
	@Configuration
	public class JacksonConfig {

	    @Bean
	    @Primary
	    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
	        System.out.println("Config is starting.");
	        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
	        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
	        return objectMapper;
	    }
	}
}
