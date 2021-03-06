package explorer.workload;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class CassWorkloadDriver implements WorkloadDriver {

  private static Logger log = LoggerFactory.getLogger(CassWorkloadDriver.class);

  private final WorkloadDirs dirs;
  private final int numNodes;
  private final String classpath;
  private String javaPath;

  private final Map<Integer, Process> nodeProcesses = new HashMap<>();

  private int testId;

  public CassWorkloadDriver(WorkloadDirs dirs, int numNodes, String javaPath) {
    this.numNodes = numNodes;
    this.dirs = dirs;
    this.classpath = getClasspath();
    this.javaPath = javaPath;
  }

  @Override
  public void prepare(int testId) {
    this.testId = testId;
    CassNodeConfig template = new CassNodeConfig(dirs);
    try {
      for (int i = 0; i < numNodes; i++) {
        template.prepareRuntime(i);
        template.applyNodeConfig(i, numNodes);
      }
    } catch(IOException e) {
      log.error("Cannot prepare test folders");
      e.printStackTrace();
      System.exit(-1);
    }

  }

  @Override
  public void startEnsemble() {
    for (int i = 0; i < numNodes; i++) {
      startNode(i);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        log.warn("Interrupted during starting node {}", i);
      }
    }
  }

  @Override
  public void sendWorkload() {
    CassWorkload.execute6023();
  }

  @Override
  public void submitQuery(int nodeId, String query) {
    if(nodeId >= numNodes)
      log.error("Cannot submit query to node " + nodeId + ". No such node.");
    else
      CassWorkload.submitQuery(nodeId, query);
  }

  @Override
  public void submitQueries(List<Integer> nodeIds, List<String> queries) {
    CassWorkload.submitQueries(nodeIds, queries);
  }

  @Override
  public void sendResetWorkload() {
    CassWorkload.reset6023();
  }

  @Override
  public void prepareNextTest() {
    CassNodeConfig template = new CassNodeConfig(dirs);
    template.prepareBallotFile();
  }

  @Override
  public void stopEnsemble() {
    nodeProcesses.forEach((id, process) -> {
      log.info("Stopping process {}...", id);
      process.destroy();
    });
    nodeProcesses.clear();
  }

  @Override
  public void cleanup() {
    testId = 0;
    try {
      FileUtils.deleteDirectory(dirs.getRunDirectory().toFile());
    } catch (IOException e) {
      log.error("Can't delete run directory.", e);
    }
  }

  private void startNode(int nodeId) {
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.directory(dirs.getRunDirectory().toFile());
    processBuilder.redirectOutput(dirs.nodeHome(nodeId, "console.out").toFile());
    processBuilder.redirectError(dirs.nodeHome(nodeId, "console.err").toFile());

    List<String> command = new ArrayList<>();
    //command.add("/home/paper387/.jenv/shims/java");
    command.add(javaPath);
    /*
    if (nodeId == 2) {
      command.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005");
    }
     */
    command.addAll(nodeArguments(nodeId));
    command.add("org.apache.cassandra.service.CassandraDaemon");

    processBuilder.command(command);
    // processBuilder.inheritIO();

    Process process;
    try {
      process = processBuilder.start();

      nodeProcesses.put(nodeId, process);
    } catch (IOException e) {
      log.error("Error starting node", e);
    }
  }

  private String getClasspath() {
    List<String> list = new ArrayList<>();
    try {
      list.add(dirs.getTargetHome().resolve("build/classes/main").toString());
      list.add(dirs.getTargetHome().resolve("build/classes/thrift").toString());
      list.addAll(Files.list(dirs.getTargetHome().resolve("lib"))
              .filter(path -> path.toString().endsWith(".jar"))
              .map(path -> path.toString())
              .collect(Collectors.toList()));
    } catch (IOException e) {
      log.error("Cannot get classpath for initializing data folders.");
      e.printStackTrace();
      System.exit(-1);
    }

    return list.stream().map(Object::toString)
        .collect(Collectors.joining(":"));
  }

  private List<String> nodeArguments(int nodeId) {
    return Arrays.asList(
        "-Dcassandra.jmx.local.port=" + (7199 + nodeId),
        "-Dlogback.configurationFile=logback.xml",
        "-Dcassandra.logDir=" + dirs.nodeHome(nodeId, "log"),
        "-Dlog4j.configuration=cass_log.properties", // + Paths.get(runDirectory.getPath(), "config", "cass_log.properties").toUri().toString(),
        "-Dcassandra.storagedir=" + dirs.nodeHome(nodeId, "data"),
        "-Dcassandra-foreground=no",
        "-cp",
        dirs.nodeHome(nodeId, "config") + ":" + classpath,
        "-Dlog4j.defaultInitOverride=true");
  }
}
