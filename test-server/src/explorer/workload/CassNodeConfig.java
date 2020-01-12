package explorer.workload;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class CassNodeConfig {

  private static Logger log = LoggerFactory.getLogger(CassNodeConfig.class);

  private final WorkloadDirs dirs;
  private final Path configTemplateFile;

  CassNodeConfig(WorkloadDirs dirs) {
    this.dirs = dirs;
    this.configTemplateFile = dirs.initData("cassandra.yaml");

    if (!Files.isRegularFile(this.configTemplateFile)) {
      throw new IllegalArgumentException("Cassandra configuration template doesn't exist");
    }
  }

  void applyNodeConfig(int nodeId, int nodeCount) throws IOException {
    Path destinationFile = dirs.nodeHome(nodeId, "config", "cassandra.yaml");

    Files.copy(configTemplateFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);

    List<String> appendLines = Arrays.asList(
        "data_file_directories:",
        "  - " + dirs.nodeHome(nodeId, "data").toString(),
        "commitlog_directory: " + dirs.nodeHome(nodeId, "commit_logs").toString(),
        "saved_caches_directory: " + dirs.nodeHome(nodeId, "saved_caches").toString(),
        "seed_provider:",
        "  - class_name: org.apache.cassandra.locator.SimpleSeedProvider",
        "    parameters:",
        "      - seeds: \"" + seeds(nodeId) + "\"",
        "listen_address: " + address(nodeId),
        "rpc_address: " + address(nodeId),
        "initial_token: " + token(nodeId, nodeCount)
    );

    Files.write(destinationFile, appendLines, StandardOpenOption.APPEND);

  }

  void prepareRuntime(int nodeId) throws IOException {

    FileUtils.copyDirectoryToDirectory(dirs.initData("config").toFile(), dirs.nodeHome(nodeId).toFile());
    Files.createDirectories(dirs.nodeHome(nodeId, "data"));
    Files.createDirectories(dirs.nodeHome(nodeId, "commit_logs"));
    Files.createDirectories(dirs.nodeHome(nodeId, "saved_caches"));
    Files.createDirectories(dirs.nodeHome(nodeId, "log"));

    prepareBallotFile();

    Files.copy(
        dirs.initData("cass_log.properties"),
        dirs.nodeHome(nodeId, "config", "cass_log.properties"),
        StandardCopyOption.REPLACE_EXISTING
    );

    FileUtils.copyDirectoryToDirectory(dirs.initData("nodes", "node_" + nodeId, "commit_logs").toFile(), dirs.nodeHome(nodeId).toFile());
    FileUtils.copyDirectoryToDirectory(dirs.initData("nodes", "node_" + nodeId, "data").toFile(), dirs.nodeHome(nodeId).toFile());
    Files.write(dirs.nodeHome(nodeId, "data", "myid"), String.valueOf(nodeId).getBytes(), StandardOpenOption.CREATE);
  }

  public void prepareBallotFile() {
    try {
      Files.copy(dirs.initData("ballot"), dirs.getRunDirectory().resolve("ballot"), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  String seeds(int nodeId) {
    return IntStream.rangeClosed(1, nodeId + 1)
        .mapToObj(s -> "127.0.0." + s)
        .collect(Collectors.joining(","));
  }

  static String address(int nodeId) {
    return "127.0.0." + (nodeId + 1);
  }

  String token(int nodeId, int nodes) {
    return new BigInteger("18446744073709551616")
        .divide(BigInteger.valueOf(nodes))
        .multiply(BigInteger.valueOf(nodeId))
        .subtract(BigInteger.valueOf(Long.MIN_VALUE).negate())
        .toString();
  }
}
