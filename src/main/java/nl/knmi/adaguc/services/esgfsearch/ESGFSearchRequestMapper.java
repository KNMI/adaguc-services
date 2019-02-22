package nl.knmi.adaguc.services.esgfsearch;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.JSONResponse;

@RestController
@RequestMapping("esgfsearch")
@CrossOrigin
public class ESGFSearchRequestMapper implements DisposableBean {
	private static ExecutorService threadPool = null;
	private  static Search esgfSearch = null;


	public ESGFSearchRequestMapper() throws ElementNotFoundException {
		super();
		
	}
	
	public void destroy() {
		Debug.println("Shutting down");
		threadPool.shutdown();
		esgfSearch = null;
	}


	@ResponseBody
	@RequestMapping("/search")
	public void search(HttpServletResponse response, HttpServletRequest request) throws IOException{
		JSONResponse jsonResponse = new JSONResponse(request);


		Debug.println("esgfsearch/search received");
		try {
			boolean enabled = ESGFSearchConfigurator.getEnabled();
			if(!enabled){
				jsonResponse.setMessage(new JSONObject().put("error", "ADAGUC esgfsearch is not enabled"));
			}else{
				Debug.println("getoverview");
				getESGFSearchInstance().doGet(request,response);
			}
		} catch (Exception e) {
			jsonResponse.setException("error: "+e.getMessage(), e);
		}
	}
	
	@ResponseBody
	@RequestMapping("/catalog")
	public void catalog(HttpServletResponse response, HttpServletRequest request) throws IOException{
		JSONResponse jsonResponse = new JSONResponse(request);


		Debug.println("esgfsearch/catalog received");
		try {
			boolean enabled = ESGFSearchConfigurator.getEnabled();
			if(!enabled){
				jsonResponse.setMessage(new JSONObject().put("error", "ADAGUC esgfsearch is not enabled"));
			}else{
				Debug.println("catalog");
				
				(new THREDDSCatalogToHTML()).handleCatalogBrowserRequest(request,response);
			}
		} catch (Exception e) {
			jsonResponse.setException("error: "+e.getMessage(), e);
		}
	}
	@ResponseBody
	@RequestMapping("/getvariables")
	public void getvariables(HttpServletResponse response, HttpServletRequest request) throws IOException{
		JSONResponse jsonResponse = new JSONResponse(request);


		Debug.println("esgfsearch/getvariables received");
		try {
			boolean enabled = ESGFSearchConfigurator.getEnabled();
			if(!enabled){
				jsonResponse.setMessage(new JSONObject().put("error", "ADAGUC esgfsearch is not enabled"));
			}else{
				Debug.println("catalog");
				 OpendapViewer viewer = new OpendapViewer( ESGFSearchConfigurator.getCacheLocation());
			     viewer.doGet(request, response);
			}
		} catch (Exception e) {
			jsonResponse.setException("error: "+e.getMessage(), e);
		}
	}
	
	


	public static synchronized Search getESGFSearchInstance() throws ElementNotFoundException {
		if(esgfSearch!=null)return esgfSearch;

		Debug.println("Creating new ESGF search instance with endpoint "+ESGFSearchConfigurator.getEsgfSearchURL());
		threadPool = Executors.newFixedThreadPool(4);
		esgfSearch = new Search(ESGFSearchConfigurator.getEsgfSearchURL(), MainServicesConfigurator.getBaseDir()+"/diskCache/",threadPool);
		return esgfSearch;
	}

}
