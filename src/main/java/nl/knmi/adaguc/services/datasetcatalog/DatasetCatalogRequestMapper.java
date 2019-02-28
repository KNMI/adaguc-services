package nl.knmi.adaguc.services.datasetcatalog;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;

import nl.knmi.adaguc.services.datasetcatalog.DatasetDescription.DataOrganisation;
import nl.knmi.adaguc.services.datasetcatalog.DatasetDescription.DataSource;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.JSONResponse;


@SuppressWarnings("deprecation")
@RestController
@RequestMapping("catalog")
@CrossOrigin
public class DatasetCatalogRequestMapper {

	@ResponseBody
	@RequestMapping("/list")
	public void listCatalog(HttpServletResponse response, HttpServletRequest request) throws IOException, ElementNotFoundException{
		JSONResponse jsonResponse = new JSONResponse(request);
		ObjectMapper om=new ObjectMapper();
		om.registerModule(new JSR310Module());
		om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		//om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		Debug.println("Using catalog path " +DatasetCatalogConfigurator.getCatalogPath());
		DatasetCatalog catalog=new DatasetCatalog(DatasetCatalogConfigurator.getCatalogPath());
		String json=om.writeValueAsString(catalog);
		Debug.println("CAT:"+catalog.getCatalog().size());
		Debug.println("JSON:"+json);
		jsonResponse.setMessage(json);
		jsonResponse.print(response);
	}	

	public static void main(String[] args) {
		DatasetDescription d1=new DatasetDescription();
		d1.setName(UUID.randomUUID().toString());
		d1.setTitle("10 min observations");
		d1.setBoundingBox(new double[]{0,50,7,57});
		d1.setStart(LocalDateTime.of(2006,1,1,0,0,0));
		d1.setEnd(LocalDateTime.of(2016,12,31,23,59,0));
		List<DatasetDescription.Param>param=new ArrayList<DatasetDescription.Param>();
		param.add(new DatasetDescription.Param("air_temperature", "t2m"));
		param.add(new DatasetDescription.Param("precipitation", "precip"));
		d1.setParameterList(param);
		d1.setProjection("+proj=latlon");
		d1.setSpatialResolution(10.);
		d1.setTimeResolution(600L);
		d1.setDatatype(DataOrganisation.POINT_REGULAR_TIME);
		d1.setSource(DataSource.OBSERVATION);
		d1.setDatalocation("http://opendap.knmi.nl/knmi/thredds/dodsC/DATALAB/hackathon/10minTempStationData.nc");
		//d1.saveToFile("/nobackup/users/vreedede/adagucservices_dir/data/adaguc-services-base/catalog");
		DatasetDescription d2=new DatasetDescription();
		d2.setName(UUID.randomUUID().toString());
		d2.setTitle("radar precipitation");
		d2.setBoundingBox(new double[]{0.0, 49.3730, 9.743, 55.296});
		d2.setStart(LocalDateTime.of(2006,1,1,0,0,0));
		d2.setEnd(LocalDateTime.of(2016,12,31,23,59,0));
		List<DatasetDescription.Param>param2=new ArrayList<DatasetDescription.Param>();
		param2.add(new DatasetDescription.Param("precipitation", "precip"));
		d2.setParameterList(param2);
		d2.setProjection("+proj=stere +lat_0=90 +lon_0=0.0 +lat_ts=60.0 +a=6378.388 +b=6356.912 +x_0=0 +y_0=0");
		d2.setSpatialResolution(1.);
		d2.setTimeResolution(300L);
		d2.setDatatype(DataOrganisation.GRID);
		d2.setSource(DataSource.OBSERVATION);
		d2.setDatalocation("http://opendap.knmi.nl/knmi/thredds/dodsC/DATALAB/hackathon/radarFullWholeData.nc");
		d2.saveToFile("/nobackup/users/vreedede/adagucservices_dir/data/adaguc-services-base/catalog");
	}
}
