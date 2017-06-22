package nl.knmi.adaguc.services.tinyopendapserver;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import nl.knmi.adaguc.security.AuthenticatorFactory;
import nl.knmi.adaguc.security.AuthenticatorInterface;
import nl.knmi.adaguc.security.token.TokenManager;
import nl.knmi.adaguc.security.user.User;
import nl.knmi.adaguc.security.user.UserManager;
import nl.knmi.adaguc.tools.Debug;


@RestController
@CrossOrigin
public class TinyDapRequestMapper {
	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
		MappingJackson2HttpMessageConverter converter = 
				new MappingJackson2HttpMessageConverter(mapper);
		return converter;
	}
	@ResponseBody
	@RequestMapping("opendap/**")
	public void runTinyDap(HttpServletResponse response, HttpServletRequest request) throws IOException{
		/* Three ways of authentication are possible:
		 * 1) - browser session based
		 * 2) - X509, for command line
		 * 3) - Access token, for commandline/browsers
		 * 
		 * When an access token is provided, it is part of the path. 
		 */

		/*Get User ID from tokenstore*/ 
		//	    JSONObject token = null;
		//	    try {
		//	      token = AccessTokenStore.checkIfTokenIsValid(request);
		//	    } catch (AccessTokenIsNotYetValid e1) {
		//	    } catch (AccessTokenHasExpired e1) {
		//	    }



		String path = request.getPathInfo();
		if(path == null){
			path = request.getServletPath();
		}
		if(path==null){
			Debug.errprintln("No Path info");
			return;
		}


		String userIdFromPath = "";
		String cleanPath = "";/*Complete string*/
		String[] pathParts = path.split("/");
		int pathPartsIndex = 2; /* In case of standardPath, set to zero */
		String tokenStr = new TokenManager().getTokenFromPath(path);
		if(tokenStr != null){
			pathPartsIndex++;
		}
		while(pathPartsIndex<pathParts.length){
			String pathParth = pathParts[pathPartsIndex];
			if(pathParth.length()>0){
				if(pathParth.length()>0){
					if(userIdFromPath.length()==0){
						userIdFromPath = pathParth;
					}
					cleanPath+="/"+pathParth;
				}
			}
			pathPartsIndex++;
		}

		//	    token = null;
		String baseNameWithOpenDapSuffixes = cleanPath.substring(cleanPath.lastIndexOf("/")+1);
		String baseName = null;
		if(baseNameWithOpenDapSuffixes.endsWith(".das")||
				baseNameWithOpenDapSuffixes.endsWith(".ddx")||
				baseNameWithOpenDapSuffixes.endsWith(".dds")||
				baseNameWithOpenDapSuffixes.endsWith(".dods")){
			baseName = baseNameWithOpenDapSuffixes.substring(0,baseNameWithOpenDapSuffixes.lastIndexOf("."));
		}else{
			baseName = baseNameWithOpenDapSuffixes;
		}
		String filePath = cleanPath.substring(userIdFromPath.length()+1);
		filePath = filePath.substring(0,filePath.lastIndexOf("/"));
		String localNetCDFFileName = null;
		try {
			AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);
			User user = null;
			if(authenticator != null){
				user = UserManager.getUser(authenticator);   
			}

			localNetCDFFileName = user.getDataDir()+"/"+filePath+"/"+baseName;
			if(!userIdFromPath.startsWith(user.getUserId())){
				Debug.println("Comparing "+user.getUserId() + "==" + userIdFromPath+ " UNEQUAL");
				Debug.errprintln("403, Unauthorized: "+userIdFromPath+"!="+user.getUserId());
				response.setStatus(403);
				response.getOutputStream().print("403 Forbidden (Wrong user id)");
				return;
			}
		} catch (Exception e) {
			String message = "401 No user information provided: "+e.getMessage();
			response.setStatus(401);
			Debug.errprintln(message);
			response.getOutputStream().print(message);
			return;
		}
		TinyDapServer.handleOpenDapReqeuests(localNetCDFFileName,baseName,cleanPath,request,response);

	}
}
