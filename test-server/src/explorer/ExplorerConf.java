package explorer;

import explorer.workload.WorkloadDirs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class ExplorerConf {

  private static final Logger log = LoggerFactory.getLogger(ExplorerConf.class);

  private static ExplorerConf INSTANCE;

  // parameters of the sampling algorithm
  public final int NUM_PROCESSES;
  public final int NUM_ROUNDS_IN_PROTOCOL;
  public final int NUM_PHASES;

  public final int randomSeed;
  public final int bugDepth;
  public final int linkEstablishmentPeriod;

  public final int portNumber;
  public final int numberOfClients;

  public final String schedulerClass;
  public final String schedulerFile;
  public final boolean schedulerFileHasMsgContent;

  public final String targetDirectory;
  public final String initialDataDirectory;
  public final String runDirectory;
  public final String javaPath;

  // Workload configuration parameters
  public final int clusterPort;
  public final int poolTimeoutMillis;
  public final int readTimeoutMillis;
  public final int timeBetweenQueriesMillis;

  // Logging configurations
  public final boolean logResult;
  public final String resultFile;

  public final int maxExecutionDuration;

  private ExplorerConf(String configFile, String[] args) {
    Properties prop = loadProperties(configFile);
    Map<String, String> overrideArgs = new HashMap();

    if(args != null && args.length != 0) {
      overrideArgs = Arrays.stream(args)
              .filter(s -> s.contains("="))
              .map(s -> Arrays.asList(s.split("=")))
              .collect(Collectors.toMap(kv -> kv.get(0), kv -> kv.get(1)));
    }

    randomSeed =  Integer.parseInt(overrideArgs.getOrDefault("randomSeed", prop.getProperty("randomSeed")));
    bugDepth =  Integer.parseInt(overrideArgs.getOrDefault("bugDepth", prop.getProperty("bugDepth")));
    linkEstablishmentPeriod =  Integer.parseInt(overrideArgs.getOrDefault("linkEstablishmentPeriod", prop.getProperty("linkEstablishmentPeriod")));

    NUM_PROCESSES = Integer.parseInt(overrideArgs.getOrDefault("numProcesses", prop.getProperty("numProcesses")));
    NUM_ROUNDS_IN_PROTOCOL = Integer.parseInt(overrideArgs.getOrDefault("numRoundsInProtocol", prop.getProperty("numRoundsInProtocol")));
    NUM_PHASES = Integer.parseInt(overrideArgs.getOrDefault("numPhases", prop.getProperty("numPhases")));

    portNumber = Integer.parseInt(overrideArgs.getOrDefault("portNumber", prop.getProperty("portNumber")));
    numberOfClients = Integer.parseInt(overrideArgs.getOrDefault("numberOfClients",prop.getProperty("numberOfClients")));

    schedulerClass = overrideArgs.getOrDefault("scheduler", prop.getProperty("scheduler"));
    schedulerFile = overrideArgs.getOrDefault("schedulerFile", prop.getProperty("scheduleFile"));

    schedulerFileHasMsgContent = Boolean.parseBoolean(overrideArgs.getOrDefault("schedulerFileHasMsgContent", prop.getProperty("scheduleHasMsgContent")));

    targetDirectory = overrideArgs.getOrDefault("targetDirectory", prop.getProperty("targetDirectory"));
    initialDataDirectory = overrideArgs.getOrDefault("initialDataDirectory", prop.getProperty("initialDataDirectory"));
    runDirectory = overrideArgs.getOrDefault("runDirectory", prop.getProperty("runDirectory"));
    javaPath = overrideArgs.getOrDefault("javaPath", prop.getProperty("javaPath"));

    log.info("Using scheduler: " + schedulerClass);
    if(schedulerClass.equals("explorer.scheduler.ReplayingScheduler"))
      log.info("using file " + schedulerFile);

    // Read cluster parameters
    clusterPort = Integer.parseInt(overrideArgs.getOrDefault("clusterPort", prop.getProperty("clusterPort")));
    poolTimeoutMillis = Integer.parseInt(overrideArgs.getOrDefault("poolTimeoutMillis", prop.getProperty("poolTimeoutMillis")));
    readTimeoutMillis = Integer.parseInt(overrideArgs.getOrDefault("readTimeoutMillis", prop.getProperty("readTimeoutMillis")));
    timeBetweenQueriesMillis = Integer.parseInt(overrideArgs.getOrDefault("timeBetweenQueriesMillis", prop.getProperty("timeBetweenQueriesMillis")));

    // Read logging parameters
    logResult = Boolean.parseBoolean(overrideArgs.getOrDefault("logResult", prop.getProperty("logResult")));
    String[] schedulerFullPath = schedulerClass.split(".");
    if(schedulerFullPath.length > 0)
      resultFile = overrideArgs.getOrDefault("resultFile",
              prop.getProperty("resultFile").concat(schedulerFullPath[schedulerFullPath.length-1]).concat("period" + linkEstablishmentPeriod).concat("d" + bugDepth));
    else
      resultFile = overrideArgs.getOrDefault("resultFile",
              prop.getProperty("resultFile").concat("Period" + linkEstablishmentPeriod).concat("D" + bugDepth));

    maxExecutionDuration = Integer.parseInt(overrideArgs.getOrDefault("maxExecutionDuration", prop.getProperty("maxExecutionDuration")));
  }

  public WorkloadDirs getWorkloadDirs() {
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
