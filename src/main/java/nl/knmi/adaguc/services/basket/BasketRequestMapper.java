package nl.knmi.adaguc.services.basket;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import nl.knmi.adaguc.security.AuthenticatorFactory;
import nl.knmi.adaguc.security.AuthenticatorInterface;
import nl.knmi.adaguc.security.user.UserManager;
import nl.knmi.adaguc.tools.JSONResponse;

@RestController
@RequestMapping("basket")
public class BasketRequestMapper {
	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
		MappingJackson2HttpMessageConverter converter = 
				new MappingJackson2HttpMessageConverter(mapper);
		return converter;
	}

	@ResponseBody
	@RequestMapping("/list")
	public void listBasket(HttpServletResponse response, HttpServletRequest request) throws IOException{
		JSONResponse jsonResponse = new JSONResponse(request);
		ObjectMapper om=new ObjectMapper();
		try {
			boolean enabled = BasketConfigurator.getEnabled();
			if(!enabled){
				jsonResponse.setMessage(new JSONObject().put("error","ADAGUC basket is not enabled"));
			}else{
				System.err.println("getoverview");
				AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);

				String userDataDir = UserManager.getUser(authenticator).getDataDir();
				String user=UserManager.getUser(authenticator).getUserId();
				//					Basket basket=new Basket("/nobackup/users/vreedede/testimpactspace/data", "ernst");
				Basket basket=new Basket(userDataDir, user);
				jsonResponse.setMessage(om.writeValueAsString(basket.getRootNode()));
			}
		} catch (Exception e) {
			jsonResponse.setException("error: "+e.getMessage(), e);
		}
		jsonResponse.print(response);
	}

	@ResponseBody
	@RequestMapping(value="/upload", method=RequestMethod.POST)
	public void uploadBasket(HttpServletResponse response, HttpServletRequest request, @RequestParam(value="files")MultipartFile[] uploadFiles, @RequestParam(value="path", required=false)String path) throws IOException{
		JSONResponse jsonResponse = new JSONResponse(request);
		ObjectMapper om=new ObjectMapper();
		try {
			boolean enabled = BasketConfigurator.getEnabled();
			if(!enabled){
				jsonResponse.setMessage(new JSONObject().put("error","ADAGUC basket is not enabled"));
			}else{
				System.err.println("uploadToBasket");
				AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);
				String userDataDir = UserManager.getUser(authenticator).getDataDir();
				int fileCnt=0;
				for (MultipartFile mpf: uploadFiles) {
					String fn=mpf.getOriginalFilename();
//					fn=mpf.getName();
					if ((fn!=null)&&(fn.length()>0)){
						System.err.println("uploading:"+fn);
						String destPath=userDataDir;
						if (path!=null) {
							destPath+="/"+path;
							File testDir=new File(destPath);
							if (!testDir.isDirectory()) {
								if (!testDir.mkdirs()) {
									System.err.println("mkdirs("+testDir+") failed");
								}
							}
						}
						destPath+="/"+fn;
						File dest=new File(destPath);
						mpf.transferTo(dest);
					}
				}
			}
		} catch (Exception e) {
			jsonResponse.setException("error: "+e.getMessage(), e);
		}
		jsonResponse.print(response);
	}

	@ResponseBody
	@RequestMapping("/remove")
	public void removeFromBasket(HttpServletResponse response, HttpServletRequest request) throws IOException{
		JSONResponse jsonResponse = new JSONResponse(request);
		ObjectMapper om=new ObjectMapper();
		try {
			boolean enabled = BasketConfigurator.getEnabled();
			if(!enabled){
				jsonResponse.setMessage(new JSONObject().put("error","ADAGUC basket is not enabled"));
			}else{
				System.err.println("removeFromBasket()");
			}
		} catch (Exception e) {
			jsonResponse.setException("error: "+e.getMessage(), e);
		}
		jsonResponse.print(response);
	}

}
