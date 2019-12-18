package nl.knmi.adaguc.cron;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;
import nl.knmi.adaguc.services.autowms.AutoWMSConfigurator;
import nl.knmi.adaguc.tools.Debug;

public class AdagucJobs {

  /* A set which keeps track if a job is still running */
  static Set<String> runningJobs = new HashSet<String>();

  /**
   * Runs all bash files in the adaguc-datasets/jobs/ with extension '.sh'
   * The found bash scripts will run in their own thread. If the script is not yet finished, it will not be started again.
   * Environment will be the same as configured for adaguc-server.
   * 
   * stderr and stdout of bash script will be forwarded to stdout.
   */
  static public void executeAdagucJobs() {
    FilenameFilter fileNameFilter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(".sh");
      }
    };
    try {
      String adagucDatasetDir = AutoWMSConfigurator.getAdagucDataset();
      File adagucJobsDir = new File(adagucDatasetDir + "/jobs/");
      if (adagucJobsDir.exists() && adagucJobsDir.isDirectory()) {
        for (File job : adagucJobsDir.listFiles(fileNameFilter)) {
          String key = job.getAbsolutePath();
          if (runningJobs.contains(key)) {
            Debug.println("Job " + key + " not yet finished.");
          } else {
            runningJobs.add(key);
            Debug.println("Starting job " + key);
            BashJobRunner jobInThread = new BashJobRunner(job, runningJobs);
            Thread t = new Thread(jobInThread);
            t.start();            
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  } 
}