
package nl.knmi.adaguc.cron;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.json.JSONArray;
import org.json.JSONObject;

import nl.knmi.adaguc.services.adagucserver.ADAGUCConfigurator;
import nl.knmi.adaguc.services.adagucserver.ADAGUCServer;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.MyXMLParser;
import nl.knmi.adaguc.tools.MyXMLParser.Options;
import nl.knmi.adaguc.tools.Tools;

public class AdagucServerScanLayers {
  /**
   * Reads and parses the adaguc configuration file based on adaguc datasets directory.
   * @param file
   * @throws JsonProcessingException
   * @throws IOException
   */
  public void readAdagucServerDatasetConfig(File file) throws JsonProcessingException, IOException {
    String config = Tools.readFile(file.getAbsolutePath());
    MyXMLParser.XMLElement rootElement = new MyXMLParser.XMLElement();
    try {
      rootElement.parseString(config);
      JSONObject json = rootElement.toJSONObject(Options.NONE);
      /* A single layer will result in a JSONObject */
      try {
        JSONObject layer = json.getJSONObject("Configuration").getJSONObject("Layer");
        takeLayer(layer, file);
      } catch (Exception e) {

      }
      /* Multiple layers will result in a JSONArray */
      try {
        JSONArray layers = json.getJSONObject("Configuration").getJSONArray("Layer");
        for (int j = 0; j < layers.length(); j++) {
          takeLayer(layers.getJSONObject(j), file);
        }
      } catch (Exception e) {

      }
    } catch (Exception e) {
    }

  }

  /**
   * Takes an adaguc-server layer configuration object and a dataset
   * configuration. It will start a scan for the layer.
   * 
   * @param layer
   * @param datasetConfigFile
   */
  private void takeLayer(JSONObject layer, File datasetConfigFile) {
    try {
      JSONObject filePath = layer.getJSONObject("FilePath");
      JSONObject autoScan = layer.getJSONObject("AutoScan");
      JSONObject autoScanAttributes = autoScan.getJSONObject("attr");
      String autoScanAttrEnabled = autoScanAttributes.getString("enabled");
      String autoScanAttrDirPattern = null;
      String autoScanAttrDuration = null;
      String autoScanAttrStep = null;
      try {
        autoScanAttrDirPattern = autoScanAttributes.getString("dirpattern");
        Debug.println("autoScanAttrDirPattern" + autoScanAttrDirPattern);
      } catch (Exception e) {
      }

      try {
        autoScanAttrDuration = autoScanAttributes.getString("duration");
        Debug.println("autoScanAttrDuration" + autoScanAttrDuration);
      } catch (Exception e) {
      }

      try {
        autoScanAttrStep = autoScanAttributes.getString("step");
        Debug.println("autoScanAttrStep" + autoScanAttrStep);
      } catch (Exception e) {
      }

      if ("true".equals(autoScanAttrEnabled)) {
        if (autoScanAttrDuration == null || autoScanAttrDirPattern == null) {
          /* Will update all files, no tailpath */
          scanAllFilesForLayer(datasetConfigFile, null);
        } else {
          /*
           * Will scan only files configured using tailpath based on duration and pattern
           */
          scanFilesForLayerUsingTailPath(datasetConfigFile, autoScanAttrDuration, autoScanAttrDirPattern, autoScanAttrStep);
        }
      }
    } catch (Exception e) {
      // Debug.printStackTrace(e);
    }
  }

  /**
   * For a certain dataset, scan files given by duration and dirpattern
   * @param datasetConfigFile
   * @param duration ISO8601 string
   * @param dirpattern directory pattern according to java's date matching (e.g. YYYY/mm/dd)
   * @throws ElementNotFoundException
   * @throws IOException
   * @throws InterruptedException
   */
  private void scanFilesForLayerUsingTailPath(File datasetConfigFile, String duration, String dirpattern, String step) throws ElementNotFoundException, IOException, InterruptedException {
    try {
      Debug.println("scanFilesForLayerUsingTailPath");
      java.time.Period d = java.time.Period.parse(duration);
      java.time.Period s = java.time.Period.parse(step);
      LocalDate minimumTime = LocalDate.now().minus(d);
      int maxIter = 10;
      LocalDate date1 = LocalDate.now();
      do {
        
        String tailPath = date1.format(DateTimeFormatter.ofPattern(dirpattern));
        scanAllFilesForLayer(datasetConfigFile, tailPath);
        date1 = date1.minus(s);
        maxIter--;
        if (maxIter<0){
          Debug.errprintln("Too many date iterations, stopping");
          break;
        }
      }while(date1.isAfter(minimumTime));
    }catch (Exception e) {
      Debug.printStackTrace(e);
    }
  }

  /**
   * For a certain dataset, scan all files.
   * @param datasetConfigFile The dataset configuration to scan.
   * @throws ElementNotFoundException
   * @throws IOException
   * @throws InterruptedException
   */
  private void scanAllFilesForLayer(File datasetConfigFile, String tailpath) throws ElementNotFoundException, IOException, InterruptedException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    String[] configEnv = ADAGUCConfigurator.getADAGUCEnvironment();
    String configFile = null;
    for(int j=0;j<configEnv.length;j++){
      if (configEnv[j].startsWith("ADAGUC_CONFIG=")) {
        configFile = configEnv[j].split("=")[1];
      }
    }
    List<String> args = new ArrayList<String>();
    args.add("--updatedb");
    args.add("--config");
    args.add(configFile+","+ datasetConfigFile.getName());
    if (tailpath != null) {
      args.add("--tailpath");
      args.add(tailpath);
    }
    String[] commands = args.toArray(new String[0]);
    ADAGUCServer.runADAGUC("/tmp/", commands, outputStream);
    outputStream.flush();
    String getCapabilities = new String(outputStream.toByteArray());
    Debug.println(getCapabilities);
    outputStream.close();
  }
}