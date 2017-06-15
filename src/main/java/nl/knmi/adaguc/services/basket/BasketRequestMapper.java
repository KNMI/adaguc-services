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
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;

import nl.knmi.adaguc.security.AuthenticatorFactory;
import nl.knmi.adaguc.security.AuthenticatorInterface;
import nl.knmi.adaguc.security.user.UserManager;
import nl.knmi.adaguc.tools.HTTPTools;
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
		om.registerModule(new JSR310Module());
		om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		try {
			boolean enabled = BasketConfigurator.getEnabled();
			if(!enabled){
				jsonResponse.setMessage(new JSONObject().put("error","ADAGUC basket is not enabled"));
			}else{
				System.err.println("getoverview");
				String tokenStr=null;
				try {
					tokenStr = HTTPTools.getHTTPParam(request, "key");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);

				String userDataDir = UserManager.getUser(authenticator).getDataDir();
				String user=UserManager.getUser(authenticator).getUserId();
				//					Basket basket=new Basket("/nobackup/users/vreedede/testimpactspace/data", "ernst");
				Basket basket=new Basket(userDataDir, user, tokenStr);
				jsonResponse.setMessage(om.writeValueAsString(basket.getRootNode()));
			}
		} catch (Exception e) {
			jsonResponse.setException("error: "+e.getMessage(), e);
		}
		jsonResponse.print(response);
	}

	@ResponseBody
	@RequestMapping(value="/mkdir")
	public void uploadBasket(HttpServletResponse response, HttpServletRequest request, @RequestParam("path")String path) throws IOException{
		JSONResponse jsonResponse = new JSONResponse(request);
		try {
			boolean enabled = BasketConfigurator.getEnabled();
			if(!enabled){
				jsonResponse.setMessage(new JSONObject().put("error","ADAGUC basket is not enabled"));
			}else{
				System.err.println("uploadToBasket");
				if (path!=null) {
					String cleanPath=cleanPathName(path);
					AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);
					String userDataDir = UserManager.getUser(authenticator).getDataDir();
					String destPath=userDataDir;
					destPath+="/"+cleanPath;
					File testDir=new File(destPath);
					if (!testDir.isDirectory()) {
						if (!testDir.mkdirs()) {
							System.err.println("mkdirs("+testDir+") failed");
							jsonResponse.setMessage(new JSONObject().put("error", "mkdir failed"));

						}
						jsonResponse.setMessage(new JSONObject().put("status", "OK"));
					} else {
						jsonResponse.setMessage(new JSONObject().put("error", "directory already exists"));
					}
				}
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
		try {
			boolean enabled = BasketConfigurator.getEnabled();
			if(!enabled){
				jsonResponse.setMessage(new JSONObject().put("error","ADAGUC basket is not enabled"));
			}else{
				System.err.println("uploadToBasket");
				AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);
				String userDataDir = UserManager.getUser(authenticator).getDataDir();
				String cleanPath=cleanPathName(path);
				for (MultipartFile mpf: uploadFiles) {
					String fn=mpf.getOriginalFilename();
					//					fn=mpf.getName();
					if ((fn!=null)&&(fn.length()>0)){
						System.err.println("uploading:"+fn);
						String destPath=userDataDir;
						if (cleanPath!=null) {
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

	private void deleteDir(File file) {
		File[] contents = file.listFiles();
		if (contents != null) {
			for (File f : contents) {
				deleteDir(f);
			}
		}
		file.delete();
	}

	// cleanPathName: cleans pathname from . and .. and leading /
	private String cleanPathName(String fn) {
		if (fn==null) {
			return null;
		}
		String[]terms=fn.split("/");
		StringBuilder sb=new StringBuilder();
		for (String t: terms) {
			if (t.equals(".")||t.equals("..")||(t.trim().length()==0)) {
				//skip
			} else {
				if (sb.length()>0){
					sb.append("/");
				}
				sb.append(t);
			}
		}
		return sb.toString();
	}

	@ResponseBody
	@RequestMapping("/remove")
	public void removeFromBasket(HttpServletResponse response, HttpServletRequest request, @RequestParam("path") String path) throws IOException{
		JSONResponse jsonResponse = new JSONResponse(request);
		try {
			boolean enabled = BasketConfigurator.getEnabled();
			if(!enabled){
				jsonResponse.setMessage(new JSONObject().put("error","ADAGUC basket is not enabled"));
			}else{
				System.err.println("removeFromBasket()");
				if (path!=null) {
					AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);
					String userDataDir = UserManager.getUser(authenticator).getDataDir();
					String cleanPath=cleanPathName(path);
					File f=new File(userDataDir+"/"+cleanPath);
					System.err.println("removing:"+ f.getPath());
					if (f.isDirectory()){
						deleteDir(f);
						jsonResponse.setMessage(new JSONObject().put("message", "dir deleted"));
					}else if (f.isFile()) {
						jsonResponse.setMessage(new JSONObject().put("message", "file deleted"));
						f.delete();
					}else {
						jsonResponse.setErrorMessage("path not found", 200);
					}
				} else {
					jsonResponse.setErrorMessage("path parameter missing", 200);
				}
			}
		} catch (Exception e) {
			jsonResponse.setException("error: "+e.getMessage(), e);
		}
		jsonResponse.print(response);
	}
}
