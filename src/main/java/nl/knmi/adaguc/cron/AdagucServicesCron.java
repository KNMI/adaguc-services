package nl.knmi.adaguc.cron;

import java.util.Timer;
import java.util.TimerTask;

import org.springframework.stereotype.Component;

@Component
public class AdagucServicesCron {
  public class CronJob extends TimerTask {

    @Override
    public void run() {
      /* Execute adaguc jobs (They are started in different threads) 
       * Environment is the same as for adaguc-server
       * stderr and stdout of bash script will be forwarded to stdout of spring boot.
       */
      AdagucJobs.executeAdagucJobs ();

      /* Scan adaguc datasets */
      AdagucAutoScan.autoScanAdagucDatasets ();
    }
  }
  AdagucServicesCron () {
    new Timer().scheduleAtFixedRate(new CronJob(), 0, 60000);
  }
}