package nl.knmi.adaguc.cron;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.joda.time.Instant;

import nl.knmi.adaguc.services.adagucserver.ADAGUCConfigurator;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.ProcessRunner;
import nl.knmi.adaguc.tools.Tools;

/**
 * Will execute bash scripts in their own thread. If the script is not yet finished, it will not be started again.
 * Environment will be the same as configured for adaguc-server.
 */
class BashJobRunner implements Runnable {
  private File file;

  /* A pointer to a set which keeps track about if a job is still running */
  private  Set<String> runningJobs;

  /**
   * Constructor for starting the bash job
   * @param file The location of the bash script
   * @param runningJobs The set which keeps track about if a job is still running, the used key in the set is 'file.getAbsolutePath()'
   */
  public BashJobRunner(File file, Set<String> runningJobs) {
      this.file = file;
      this.runningJobs = runningJobs;
  }

  /**
   * Method from Runnable, will start in a new thread.
   */
  public void run() {
    String key =  file.getAbsolutePath();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    String instanceId = UUID.randomUUID().toString();
    List<String> environmentVariables = new ArrayList<String>();
    String tmpDir = "/tmp/";
    try {
      Tools.mksubdirs(tmpDir);
      environmentVariables.add("ADAGUC_TMP="+tmpDir);
      String tmpLogFile = tmpDir + "adaguc-job" + instanceId;
      environmentVariables.add("ADAGUC_LOGFILE=" + tmpLogFile);
      environmentVariables.add("HOME="+tmpDir);
  
      String[] configEnv = ADAGUCConfigurator.getADAGUCEnvironment();
      if(configEnv != null){
        for(int j=0;j<configEnv.length;j++){
          if (!configEnv[j].startsWith("ADAGUC_LOGFILE") && !configEnv[j].startsWith("ADAGUC_TMP") && !configEnv[j].startsWith("ADAGUC_ONLINERESOURCE")) {
            environmentVariables.add(configEnv[j]);    
          } else {
            Debug.errprintln("[WARNING]: Environment " + configEnv[j] + " is controlled by adaguc-services.");
          }
        }
      }
  
      String[] environmentVariablesAsArray = new String[ environmentVariables.size() ];
      environmentVariables.toArray( environmentVariablesAsArray );
      long timeOutMs = ADAGUCConfigurator.getTimeOut();

      String name = System.lineSeparator() + "JOB [" + file.getName() + " " + Instant.now().toString()   + "]: ";
      outputStream.write(name.getBytes(), 0, name.length());
      class StdoutPrinter implements ProcessRunner.StatusPrinterInterface{
        public void setError(String message) {}
        public String getError() {				return null;	}
        public boolean hasData() { 			return false;	}
        public void print(byte[] message, int bytesRead) {
          /* Prepend each line with the name of the script */
          String m = new String(message, 0, bytesRead);
          m = (m.replaceAll(System.lineSeparator(), name));
          outputStream.write(m.getBytes(), 0, m.length());
        }
      }
      ProcessRunner processRunner = new ProcessRunner (new StdoutPrinter(),new StdoutPrinter(),environmentVariablesAsArray,tmpDir, timeOutMs);
      List<String> commands = new ArrayList<String>();
      commands.add("bash");
      commands.add(file.getAbsolutePath());
      processRunner.runProcess(commands.toArray(new String[0]), null);
      try {
        Tools.rmfile(tmpLogFile);
      } catch (Exception e) {
      }
      String jobOutput = new String(outputStream.toByteArray());
      if (jobOutput.endsWith(name)) jobOutput = jobOutput.substring(0, jobOutput.length() - name.length());
      System.out.println(jobOutput);
      outputStream.close();
    
  } catch (IOException | ElementNotFoundException e1) {
    e1.printStackTrace();
  } catch (InterruptedException e1) {
    e1.printStackTrace();
  }
  /* Indicate that the job is finished, so new ones for this file can be started */
  runningJobs.remove(key);
}

}