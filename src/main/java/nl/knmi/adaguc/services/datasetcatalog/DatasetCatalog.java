package nl.knmi.adaguc.services.datasetcatalog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import nl.knmi.adaguc.tools.Debug;

@Getter
public class DatasetCatalog {
	@JsonIgnore
	private String path;
	List<DatasetDescription>catalog;

	public DatasetCatalog(String path) {
		this.path=path;
		initCatalog();
	}

	private void initCatalog() {
		this.catalog=new ArrayList<DatasetDescription>();
		Debug.println("initCatalog" + this.path);
		File d=new File(this.path);
		
		Debug.println("d:"+d);
		if (d.isDirectory()) {
			String[] filesIndir=d.list();
			for (String fn: filesIndir) {
				String fullPath=path+"/"+fn;
				Debug.println("fn:"+fullPath);
				File f=new File(fullPath);
				if (f.isFile()) {
					DatasetDescription descr=DatasetDescription.getDatasetDescription(fullPath);
					if (descr!=null) {
						this.catalog.add(descr);
					}
				}
			}
		}
	}
}