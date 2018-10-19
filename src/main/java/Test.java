import java.io.IOException;

import nl.knmi.adaguc.tools.Debug;
import ucar.nc2.dataset.NetcdfDataset;

public class Test {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Debug.println("Hi");
		//Standaard FEWS OpenDAP geeft Exception in thread "main" java.io.IOException: Server does not support Content-Length:
		
		
	    //NetcdfDataset netcdfDataset = NetcdfDataset.openDataset("https://data.knmi.nl/wms/cgi-bin/wms.cgi/opendap/Rd1nrt_1/prediction");
		
        NetcdfDataset netcdfDataset = NetcdfDataset.openDataset("http://bhw485.knmi.nl:8080/cgi-bin/autoresource.cgi/opendap/testdata.nc");
		// NetcdfDataset netcdfDataset = NetcdfDataset.openDataset("http://opendap.knmi.nl/knmi/thredds/dodsC/ADAGUC/testsets/regulargrids/globem_nox_sa_hires-2010-months.nc");
		
	    System.out.println(netcdfDataset.getReferencedFile());
//	    
//	    //Alternatief (ook met .dds en .das geprobeerd):
//	    DConnect2 url = new DConnect2("https://data.knmi.nl/wms/cgi-bin/wms.cgi/opendap/Rd1nrt_1/", true);
//	    try {
//
//	               //Ook met andere url.getDataXXX() geprobeerd geeft: opendap.dap.DAP2Exception: Not a valid OPeNDAP server - Missing MIME Header fields! Either "XDAP" or "XDODS-Server." must be present.
//	        DataDDS dataDDX = url.getDataDDX();
//	        Enumeration variables = dataDDX.getVariables();
//	        System.out.println(variables.nextElement());
//	    } catch (DAP2Exception e) {
//	        System.out.println(e.getMessage());
//	    }        
	}

}
