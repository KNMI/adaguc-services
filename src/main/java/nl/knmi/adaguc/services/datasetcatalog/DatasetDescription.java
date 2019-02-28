package nl.knmi.adaguc.services.datasetcatalog;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("deprecation")
@Setter
@Getter
public class DatasetDescription implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -488540239570320546L;


	public enum DataSource {OBSERVATION, MODEL};
	public enum DataOrganisation {POINT, POINT_REGULAR_TIME, GRID, SWATH};
	@Getter
	@Setter
	public static class Param {
		private String standardName;
		private String varName;
		public Param(String standardName, String varName) {
			this.standardName=standardName;
			this.varName=varName;
		}
		public Param(){}
	}
	
	private String name;
	private String title;
	private Long timeResolution; //typical time resolution in s 
	private Double spatialResolution; //typical spatial resolution in km
	private LocalDateTime start;
	private LocalDateTime end;
	private List<Param>parameterList;
    private String projection;
    private double[] boundingBox;
    private DataOrganisation datatype;
    private DataSource source;
    private String datalocation;
    
    public DatasetDescription() {
    	this.name="a";
    	this.boundingBox=new double[]{-180,-90,180,90};
    	this.projection="+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs";
    	this.parameterList=new ArrayList<Param>();
    }
    
    public static DatasetDescription getDatasetDescription(String fn) {
        ObjectMapper om=getObjectMapper();
		DatasetDescription descr=null;
		try {
			descr = om.readValue(new File(fn), DatasetDescription.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return descr;
    }
    
    public String getVarName(String standardName) {
    	for (Param p: this.parameterList) {
    		if (p.getStandardName().equals(standardName)){
    			return p.getVarName();
    		}
    	}
    	return null;
    }
    
    public String[] listStandardNames() {
    	Vector<String>standardNames=new Vector<String>();
    	for (Param p: this.parameterList) {
    		standardNames.add(p.getStandardName());
    	}
    	return standardNames.toArray(new String[0]);
    }
    
    private static ObjectMapper getObjectMapper() {
		ObjectMapper om=new ObjectMapper();
		om.registerModule(new JSR310Module());
		om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return om;
    }
    
     
    public void saveToFile(String path) {
    	File f=new File(path+"/"+this.name+".json");
    	ObjectMapper om=getObjectMapper();
    	try {
			om.writeValue(f, this);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
}
