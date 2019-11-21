package explorer.scheduler;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import explorer.ExplorerConf;
import utils.FileUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FailureInjectingSettings extends SchedulerSettings {
  ExplorerConf conf = ExplorerConf.getInstance();

  // derived from the parameters
  public final int NUM_EXPECTED_EVENTS = conf.NUM_LIVENESS_ROUNDS * conf.NUM_PHASES * conf.NUM_PROCESSES;
  public final int NUM_MAJORITY= (conf.NUM_PROCESSES / 2) + 1;

  private Random random;
  public final int depth = conf.bugDepth;

  @Expose
  public int seed = conf.getSeed();
  @Expose
  private List<FailureInjectingSettings.NodeFailure> failures = new ArrayList<>();
  @Expose
  public int mutationNo = 0; // the mutations of the same seed are enumerated for logging

  private int numMutators = 0;

  // to be used for deserialization (failures will be set)
  public FailureInjectingSettings() {
    seed = conf.getSeed();
    random = new Random(seed);
  }

  // to be used for creation
  public FailureInjectingSettings(int seed) {
    this.seed = seed;
    random = new Random(seed);

    if(ExplorerConf.getInstance().testMutations)
      failures = getMutationFailures();

    if(failures.size() == 0)
      failures = getRandomFailures();
    //failures = getFailuresToReproduceBug();
  }


  // constructor used when constructed from a mutation - written as json for next executions
  private FailureInjectingSettings(int seed, List<NodeFailure> failures, int mutationNo) {
    random = new Random(seed);
    this.mutationNo = mutationNo;
    this.failures = failures;
  }



  @Override
  public SchedulerSettings mutate() {
    int failureToRemove = random.nextInt(failures.size());

    List<NodeFailure> mutation = new ArrayList<>(failures);
    mutation.remove(failureToRemove);

    // add existing:
    int[] failurePerPhase = new int[conf.NUM_PHASES];
    List<Integer> phases = new ArrayList<>();

    for(int i = 0; i < conf.NUM_PHASES; i++) {
      phases.add(i);
    }

    for (NodeFailure nodeFailure : mutation) {
      int phaseToFailAt = nodeFailure.k;
      failurePerPhase[phaseToFailAt]++;
      if (failurePerPhase[phaseToFailAt] == conf.NUM_PROCESSES)
        phases.remove(phaseToFailAt);
    }

    int phaseToFailAt = random.nextInt(phases.size());
    int roundToFailAt = random.nextInt(conf.NUM_LIVENESS_ROUNDS);
    int processToFail = random.nextInt(conf.NUM_PROCESSES);

    mutation.add(new NodeFailure(phaseToFailAt, roundToFailAt, processToFail));

    return new FailureInjectingSettings(conf.getSeed(), mutation, ++numMutators);
  }

  private List<FailureInjectingSettings.NodeFailure> getRandomFailures() {
    List<FailureInjectingSettings.NodeFailure> f  = new ArrayList<>();

    int[] failurePerPhase = new int[conf.NUM_PHASES];
    List<Integer> phases = new ArrayList<>();

    for(int i = 0; i < conf.NUM_PHASES; i++) {
      phases.add(i);
    }

    for(int i = 0; i < depth; i++) {
      int phaseToFailAt = random.nextInt(phases.size());
      failurePerPhase[phaseToFailAt] ++;
      if(failurePerPhase[phaseToFailAt] == conf.NUM_PROCESSES)
        phases.remove(phaseToFailAt);

      int roundToFailAt = random.nextInt(conf.NUM_LIVENESS_ROUNDS);
      int processToFail = random.nextInt(conf.NUM_PROCESSES);

      f.add(new NodeFailure(phaseToFailAt, roundToFailAt, processToFail));
    }

    return f;
  }

  private List<FailureInjectingSettings.NodeFailure> getMutationFailures() {
    // read mutations file
    List<String> mutationStrs = FileUtils.readLinesFromFile("mutations");
    List<FailureInjectingSettings.NodeFailure> failuresToExecute = new ArrayList<>();

    if(mutationStrs.size() > 0) {
      // take the first mutation to execute
      failuresToExecute = ((FailureInjectingSettings)toObject(mutationStrs.get(0))).getFailures();

      // write back the rest
      //todo revise with a more efficient way
      StringBuilder sb = new StringBuilder();
      for(int i = 1; i < mutationStrs.size(); i++) {
        sb.append(mutationStrs.get(i)).append("\n");
      }
      FileUtils.writeToFile("mutations", sb.toString(), false);
      FileUtils.writeToFile("result.txt", "\nRunning mutation: " + mutationStrs.get(0), true);
    }

    return failuresToExecute;
  }

  public List<FailureInjectingSettings.NodeFailure> getFailures() {
    return failures;
  }

  private List<FailureInjectingSettings.NodeFailure> getFailuresToReproduceBug() {
    // depth is 6
    List<FailureInjectingSettings.NodeFailure> f  = new ArrayList<>();
    f.add(new NodeFailure(0, 4, 2));
    f.add(new NodeFailure(1, 2, 2));
    f.add(new NodeFailure(1, 4, 0));
    f.add(new NodeFailure(2, 0, 0));
    f.add(new NodeFailure(2, 0, 1));
    f.add(new NodeFailure(3, 0, 0));
    return f;
  }

  public static class NodeFailure {
    @Expose
    int k; // in which request does it happen?
    @Expose
    int r; // at which round does it happen?
    @Expose
    int process; // which process fails?

    public NodeFailure(int k, int r, int p) {
      this.k = k;
      this.r = r;
      this.process = p;
    }

    @Override
    public boolean equals(Object obj) {
      if(!(obj instanceof NodeFailure)) return false;

      return k == ((NodeFailure)obj).k
          && r == ((NodeFailure)obj).r
          && process == ((NodeFailure)obj).process;
    }

    @Override
    public int hashCode() {
      int result = 17;
      result = 31 * result + k;
      result = 31 * result + r;
      result = 31 * result + process;
      return result;
    }
  };




}
