package nl.knmi.adaguc;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
public class AdagucServicesApplication extends SpringBootServletInitializer{

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application)  {
		return application.sources(AdagucServicesApplication.class);
	}

	
	public static void main(String[] args) {
        configureApplication(new SpringApplicationBuilder()).run(args);
    }

    private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder builder) {
        return builder.sources(AdagucServicesApplication.class).bannerMode(Banner.Mode.OFF);
    }
//	public static void main(String[] args) {
//		SpringApplication.run(AdagucServicesApplication.class, args);
//	}
//	

//    private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder builder) {
//        return builder.sources(AdagucServicesApplication.class).bannerMode(Banner.Mode.OFF);
//    }

	
}
