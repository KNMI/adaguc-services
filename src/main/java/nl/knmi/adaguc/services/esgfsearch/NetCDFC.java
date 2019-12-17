package nl.knmi.adaguc.services.esgfsearch;

import nl.knmi.adaguc.security.user.User;
import nl.knmi.adaguc.tools.Debug;
import  nl.knmi.adaguc.tools.ProcessRunner;

public class NetCDFC {
  static class NCDump{
   
     
    public String doNCDump(User user,String url) throws Exception{
    
      class StderrPrinter implements ProcessRunner.StatusPrinterInterface{
        String ncdumpError="";
        public void print(byte[] message,int bytesRead) {Debug.errprint(new String(message,0,bytesRead));ncdumpError+=new String(message,0,bytesRead);}
        public void setError(String message) {}
        public String getError() {return ncdumpError;}
		@Override
		public boolean hasData() {
			return false;
		}
      }
      class StdoutPrinter implements ProcessRunner.StatusPrinterInterface{
        String ncdumpResult="";
        public void print(byte[] message,int bytesRead) {ncdumpResult+=new String(message,0,bytesRead);}
        public void setError(String message) {}
        public String getError() {return ncdumpResult;}
		@Override
		public boolean hasData() {
			return false;
		}
      }

     
     ProcessRunner.StatusPrinterInterface stdoutPrinter = new StdoutPrinter();
      ProcessRunner.StatusPrinterInterface stderrPrinter = new StderrPrinter();
      String userHome="";
      try{
        userHome=user.getHomeDir();
      }catch(Exception e){
      }
      String[] environmentVariables = {"HOME="+userHome};
      ProcessRunner processRunner = new ProcessRunner (stdoutPrinter,stderrPrinter,environmentVariables,userHome, -1);
      try{
        String commands[]={"ncdump","-h","-x",url};
        Debug.println("starting ncdump for "+url);
        processRunner.runProcess(commands,null);
      }
      catch (Exception e){
        Debug.errprint(e.getMessage());
        throw new Exception ("Unable to do ncdump: "+e.getMessage());
      }

      String ncdumpResult=stdoutPrinter.getError();
      String ncdumpError=stderrPrinter.getError();
     
      if(ncdumpResult.length()==0){
        if(ncdumpError.length()>0){
          throw new Exception ("Unable to do ncdump, error occured:["+ncdumpError+"]");
        }else{
          throw new Exception ("Unable to do ncdump, no info available.");
        }
      }

      return ncdumpResult;        
    }
  }
  
  
  
  public static String executeNCDumpCommand(User user,String url) throws Exception{
    //Debug.println("Execute ncdump on "+url);
    String result;
    NCDump doNCDump = new NCDump();
    result=doNCDump.doNCDump(user,url);
    doNCDump = null;
    return result;
  }
}
