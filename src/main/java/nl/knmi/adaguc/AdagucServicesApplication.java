package nl.knmi.adaguc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class AdagucServicesApplication {

	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(AdagucServicesApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(AdagucServicesApplication.class, args);
	}


}
