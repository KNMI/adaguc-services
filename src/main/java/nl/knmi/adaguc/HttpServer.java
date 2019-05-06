package nl.knmi.adaguc;

import java.util.List;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import nl.knmi.adaguc.tools.Debug;

@Component
public class HttpServer {
	@Bean
	public ServletWebServerFactory servletContainer(@Value("${server.http.port:0}") int httpPort, @Value("${server.port}") int port) {
		
		TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
		if (httpPort != 0 && (port!=httpPort)) {
			Debug.println("configuring extra connector at port " + httpPort);
			List<Connector> tomcatConnectors = tomcat.getAdditionalTomcatConnectors();
			boolean portAlreadyUsed = false;
			for(Connector tomcatConnector : tomcatConnectors) {
				if (tomcatConnector.getPort()==httpPort) {
					portAlreadyUsed = true;
				}
			}
			if (portAlreadyUsed == false) {
				Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
				connector.setPort(httpPort);
				connector.setAttribute("relaxedPathChars", "<>[\\]^`{|}");
		        connector.setAttribute("relaxedQueryChars", "<>[\\]^`{|}");
				tomcat.addAdditionalTomcatConnectors(connector);
			}
		}
		return tomcat;
	}
}