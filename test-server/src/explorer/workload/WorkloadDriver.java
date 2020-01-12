package explorer.workload;

import java.io.IOException;
import java.util.List;

public interface WorkloadDriver {
  void prepare(int testId);
  void startEnsemble();
  void sendWorkload(); // bulk workload to reproduce a buggy scenario (e.g. with multiple queries)
  public void submitQuery(int nodeId, String query);
  public void submitQueries(List<Integer> nodeIds, List<String> queries);
  void sendResetWorkload();
  void prepareNextTest();
  void stopEnsemble();
  void cleanup();
}
