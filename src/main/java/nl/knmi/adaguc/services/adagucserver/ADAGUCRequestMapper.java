package nl.knmi.adaguc.services.adagucserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.JSONResponse;

@RestController
public class ADAGUCRequestMapper {
	
	@ResponseBody
	@CrossOrigin
	@RequestMapping("wms")
	public void ADAGUCSERVERWMS(HttpServletResponse response, HttpServletRequest request){
		Debug.println("/wms");
		try {
			ADAGUCServer.runADAGUCWMS(request,response,null,null);
		} catch (Exception e) {
			JSONResponse jsonResponse = new JSONResponse(request);
			jsonResponse.setException("ADAGUCServer WMS request failed",e);
			try {
				jsonResponse.print(response);
			} catch (Exception e1) {

			}
		}

	}
	@ResponseBody
	@CrossOrigin
	@RequestMapping("adagucserver")
	public void ADAGUCSERVER(HttpServletResponse response, HttpServletRequest request){
		Debug.println("/adagucserver");
		try {
			ADAGUCServer.runADAGUCWMS(request,response,null,null);
		} catch (Exception e) {
			JSONResponse jsonResponse = new JSONResponse(request);
			jsonResponse.setException("ADAGUCServer WMS request failed",e);
			try {
				jsonResponse.print(response);
			} catch (Exception e1) {

			}
		}

	}
	@ResponseBody
	@CrossOrigin
	@RequestMapping("wcs")
	public void ADAGUCSERVERWCS(HttpServletResponse response, HttpServletRequest request){
		Debug.println("/wcs");
		try {
			ADAGUCServer.runADAGUCWCS(request,response,null,null);
		} catch (Exception e) {
			JSONResponse jsonResponse = new JSONResponse(request);
			jsonResponse.setException("ADAGUCServer WCS request failed",e);
			try {
				jsonResponse.print(response);
			} catch (Exception e1) {

			}
		}

	}
	@ResponseBody
	@CrossOrigin
	@RequestMapping("adagucopendap/**")
	public void ADAGUCSERVEROPENDAP(HttpServletResponse response, HttpServletRequest request){
		Debug.println("/adagucopendap");
		try {
			ADAGUCServer.runADAGUCOpenDAP(request,response,null,null);
		} catch (Exception e) {
			JSONResponse jsonResponse = new JSONResponse(request);
			jsonResponse.setException("ADAGUCServer OPENDAP request failed",e);
			try {
				jsonResponse.print(response);
			} catch (Exception e1) {

			}
		}

	}
}
