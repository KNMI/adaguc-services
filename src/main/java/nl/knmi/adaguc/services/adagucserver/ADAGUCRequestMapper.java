package nl.knmi.adaguc.services.adagucserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.JSONResponse;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

@RestController
public class ADAGUCRequestMapper {

	@ResponseBody
	@CrossOrigin
	@RequestMapping("wms")
	public void ADAGUCSERVERWMS(HttpServletResponse response, HttpServletRequest request) {
		Debug.println("#### SERVLET /wms ####");
		try {
			ADAGUCServer.runADAGUCWMS(request, response, null, null);
		} catch (Exception e) {
			Debug.printStackTrace(e);
			JSONResponse jsonResponse = new JSONResponse(request);
			jsonResponse.setException("[ADAGUC-Server] WMS request failed", e);
			try {
				jsonResponse.print(response);
			} catch (Exception e1) {

			}
		}

	}

	@ResponseBody
	@CrossOrigin
	@RequestMapping("adagucload")
	public void adagucLoad(HttpServletResponse response, HttpServletRequest request) {
		JSONResponse jsonResponse = new JSONResponse(request);
		try {
			OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

			jsonResponse.setMessage(new JSONObject().put("adagucServer",
					new JSONObject().put("instancesInQueue", ADAGUCServer.getNumInstancesInQueue())
							.put("instancesRunning", ADAGUCServer.getNumInstancesRunning())
							.put("maxQueueSize", ADAGUCConfigurator.getMaxInstancesInQueue())
							.put("maxInstances", ADAGUCConfigurator.getMaxInstances())
							.put("instanceTimeout", ADAGUCConfigurator.getTimeOut())
							.put("getSystemLoadAverage", operatingSystemMXBean.getSystemLoadAverage())
							.put("getAvailableProcessors", operatingSystemMXBean.getAvailableProcessors())));
		} catch (Exception e) {
			jsonResponse.setException("ADAGUCServer ADAGUC Load request failed", e);
		}
		try {
			jsonResponse.print(response);
		} catch (Exception e1) {

		}
	}

	@ResponseBody
	@CrossOrigin
	@RequestMapping("adagucserver")
	public void ADAGUCSERVER(HttpServletResponse response, HttpServletRequest request) {
		Debug.println("/adagucserver");
		try {
			ADAGUCServer.runADAGUCWMS(request, response, null, null);
		} catch (Exception e) {
			JSONResponse jsonResponse = new JSONResponse(request);
			jsonResponse.setException("[ADAGUC-Server] WMS request failed", e);
			try {
				jsonResponse.print(response);
			} catch (Exception e1) {

			}
		}
	}

	@ResponseBody
	@CrossOrigin
	@RequestMapping("adaguc-server")
	public void ADAGUCDASHSERVER(HttpServletResponse response, HttpServletRequest request) {
		ADAGUCSERVER(response, request);
	}

	@ResponseBody
	@CrossOrigin
	@RequestMapping("wcs")
	public void ADAGUCSERVERWCS(HttpServletResponse response, HttpServletRequest request) {
		Debug.println("/wcs");
		try {
			ADAGUCServer.runADAGUCWCS(request, response, null, null);
		} catch (Exception e) {
			JSONResponse jsonResponse = new JSONResponse(request);
			jsonResponse.setException("ADAGUCServer WCS request failed", e);
			try {
				jsonResponse.print(response);
			} catch (Exception e1) {

			}
		}

	}

	@ResponseBody
	@CrossOrigin
	@RequestMapping("adagucopendap/**")
	public void ADAGUCSERVEROPENDAP(HttpServletResponse response, HttpServletRequest request) {
		Debug.println("/adagucopendap");
		try {
			ADAGUCServer.runADAGUCOpenDAP(request, response, null, null);
		} catch (Exception e) {
			JSONResponse jsonResponse = new JSONResponse(request);
			jsonResponse.setException("ADAGUCServer OPENDAP request failed", e);
			try {
				jsonResponse.print(response);
			} catch (Exception e1) {

			}
		}

	}
}
