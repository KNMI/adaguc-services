package nl.knmi.adaguc.cron;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.stereotype.Component;

import nl.knmi.adaguc.services.autowms.AutoWMSConfigurator;
import nl.knmi.adaguc.tools.ElementNotFoundException;

@Component
public class AdagucServicesCron {

 

  public class CronJob extends TimerTask {

    @Override
    public void run() {
      

      /* Scan adaguc datasets */
      File[] files = null;
      try {
        files = AutoWMSConfigurator.getDatasets();
      } catch (ElementNotFoundException e1) {
        e1.printStackTrace();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
      for (File file : files) {
        try {
          new AdagucServerScanLayers().readAdagucServerDatasetConfig(file);
        } catch (Exception e) {
          e.printStackTrace();
        }        
      }

   
    }
  }
  AdagucServicesCron () {
    new Timer().scheduleAtFixedRate(new CronJob(), 0, 10000);
  }


}