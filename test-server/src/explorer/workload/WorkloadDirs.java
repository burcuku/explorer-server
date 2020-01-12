package explorer.workload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WorkloadDirs {

  private static Logger log = LoggerFactory.getLogger(WorkloadDirs.class);

  private Path targetHome;
  private Path runDirectory;
  private Path initDataDir;

  public WorkloadDirs(String targetHome, String runDirectory, String initDataDir) {
    try {
      this.targetHome = verifyDirectoryExists(targetHome);
      this.initDataDir = verifyDirectoryExists(initDataDir);
      this.runDirectory = ensureDirectoryExists(runDirectory);

      log.info("Target system directory: {}", this.targetHome);
      log.info("Run directory: {}", this.runDirectory);
      log.info("Initialization data directory: {}", this.initDataDir);

    } catch (IOException e) {
      log.error("Cannot initialize workload directories.");
      e.printStackTrace();
      System.exit(-1);
    }

  }

  public Path nodeHome(int nodeId, String... paths) {
    return runDirectory.resolve(Paths.get("node_" + nodeId, paths)).normalize();
  }

  public Path initData(String path, String... paths) {
    return initDataDir.resolve(Paths.get(path, paths)).normalize();
  }

  public Path getTargetHome() {
    return targetHome;
  }

  public Path getRunDirectory() {
    return runDirectory;
  }

  public Path getInitDataDir() {
    return initDataDir;
  }

  public static Path verifyDirectoryExists(String dir) {
    if (dir == null || "".equals(dir)) {
      throw new IllegalArgumentException("Required directory isn't set");
    }
    Path targetDir = Paths.get(dir);
    if (Files.notExists(targetDir) || !Files.isDirectory(targetDir)) {
      throw new IllegalArgumentException("Directory doesn't exist or isn't accessible: " + dir);
    }
    return targetDir;
  }

  public static Path ensureDirectoryExists(String dir) throws IOException {
    Path targetDir = Paths.get(dir);
    if (Files.notExists(targetDir)) {
      Files.createDirectories(targetDir);
    }
    return targetDir;
  }
}
