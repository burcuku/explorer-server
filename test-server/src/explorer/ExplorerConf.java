package explorer;

import explorer.workload.WorkloadDirs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class ExplorerConf {

  private static final Logger log = LoggerFactory.getLogger(ExplorerConf.class);

  private static ExplorerConf INSTANCE;

  public final int portNumber;
  public final int numberOfClients;

  public final String schedulerClass;
  public final String schedulerFile;

  public final String targetDirectory;
  public final String initialDataDirectory;
  public final String runDirectory;
  public final String javaPath;

  private ExplorerConf(String configFile, String[] args) {
    Properties prop = loadProperties(configFile);
    Map<String, String> overrideArgs = Arrays.stream(args)
        .filter(s -> s.contains("="))
        .map(s -> Arrays.asList(s.split("=")))
        .collect(Collectors.toMap(kv -> kv.get(0), kv -> kv.get(1)));

    portNumber = Integer.parseInt(prop.getProperty("portNumber"));
    numberOfClients = Integer.parseInt(prop.getProperty("numberOfClients"));

    schedulerClass = prop.getProperty("scheduler");
    schedulerFile = prop.getProperty("scheduleFile");

    targetDirectory = overrideArgs.getOrDefault("targetDirectory", prop.getProperty("targetDirectory"));
    initialDataDirectory = overrideArgs.getOrDefault("initialDataDirectory", prop.getProperty("initialDataDirectory"));
    runDirectory = overrideArgs.getOrDefault("runDirectory", prop.getProperty("runDirectory"));
    javaPath = overrideArgs.getOrDefault("javaPath", prop.getProperty("javaPath"));
  }

  public WorkloadDirs getWorkloadDirs() throws IOException {
    return new WorkloadDirs(targetDirectory, initialDataDirectory, runDirectory);
  }

  private static Properties loadProperties(String configFile) {
    Properties prop = new Properties();
    try (FileInputStream ip = new FileInputStream(configFile)) {
      prop.load(ip);
    } catch (IOException e) {
      log.error("Can't load properties file: {}", configFile);
    }
    return prop;
  }

  public synchronized static ExplorerConf initialize(String configFile, String[] args) {
    INSTANCE = new ExplorerConf(configFile, args);
    return INSTANCE;
  }

  public synchronized static ExplorerConf getInstance() {
    if (INSTANCE == null) {
      throw new IllegalStateException("Configuration not initialized");
    }
    return INSTANCE;
  }
}
