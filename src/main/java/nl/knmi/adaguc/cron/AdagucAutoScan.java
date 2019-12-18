package nl.knmi.adaguc.cron;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Component;

import nl.knmi.adaguc.services.autowms.AutoWMSConfigurator;
import nl.knmi.adaguc.tools.ElementNotFoundException;

@Component
public class AdagucAutoScan {

  /**
   * Find all configured adaguc-server datasets, loop through them and do something with them.
   * 
   * For example if a dataset has a Layer with the following element:
   * 
   * <AutoScan enabled="true" dirpattern="yyyy/MM/dd" duration="P6D" step="P1D" />
   * 
   * It will automatically scan last 6 days until current time.
   */
    public static void autoScanAdagucDatasets() {
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