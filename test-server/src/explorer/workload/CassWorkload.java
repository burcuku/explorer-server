package explorer.workload;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.policies.WhiteListPolicy;
import explorer.utils.ListenableFutureAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class CassWorkload {

  private static Logger log = LoggerFactory.getLogger(CassWorkload.class);

  static void execute6023() {
    try {
      CompletableFuture<Boolean> w1 = executeCql(0, "test", "UPDATE tests SET value_1 = 'A' WHERE name = 'testing' IF owner = 'user_1'");
      //executeCql(0, "test", "UPDATE tests SET value_1 = 'A' WHERE name = 'testing' IF owner = 'user_1';");//.get();
      //Thread.sleep(1000);
      CompletableFuture<Boolean> w2 = executeCql(1, "test", "UPDATE tests SET value_1 = 'B', value_2 = 'B' WHERE name = 'testing' IF  value_1 = 'A'");
      //executeCql(1, "test", "UPDATE tests SET value_1 = 'B', value_2 = 'B' WHERE name = 'testing' IF owner = 'user_1';");//.get();
      //Thread.sleep(1000);
      CompletableFuture<Boolean> w3 = executeCql(2, "test", "UPDATE tests SET value_3 = 'C' WHERE name = 'testing' IF owner = 'user_1'");
      //executeCql(2, "test", "UPDATE tests SET value_3 = 'C' WHERE name = 'testing' IF owner = 'user_1';");//.get();
      //Thread.sleep(1000);
      CompletableFuture.allOf(w1, w2, w3)
         .get();
    } catch (InterruptedException e) {
      log.error("Interrupted while sleeping", e);
    } catch (ExecutionException e) {
      log.error("Exception happened during query. Exiting...", e);
      throw new RuntimeException(e);
    }
  }

  private static CompletableFuture<Boolean> executeCql(int nodeId, String keyspace, String cql) {
    Cluster cluster = getCluster(nodeId).init();
    Session session = cluster.connect(keyspace).init();
    log.info("Executing query for cluster {}: {}", nodeId, cql);
    return ListenableFutureAdapter.toCompletable(session.executeAsync(cql))
        .thenApply(ResultSet::wasApplied)
        .thenApply(b -> {
          session.close();
          cluster.close();
          return b;
        });
  }

  private static Cluster getCluster(int nodeId) {
    String nodeIp = CassNodeConfig.address(nodeId);
    Cluster cluster = Cluster.builder()
        .addContactPoint(nodeIp)
        .withProtocolVersion(ProtocolVersion.V2)
        .withPort(9042)
        .withLoadBalancingPolicy(new WhiteListPolicy(new RoundRobinPolicy(), Collections.singleton(new InetSocketAddress(nodeIp, 9042))))
        .build();
    cluster.getConfiguration().getPoolingOptions().setPoolTimeoutMillis(120000);
    cluster.getConfiguration().getSocketOptions().setReadTimeoutMillis(30000);
    return cluster;
  }
}
