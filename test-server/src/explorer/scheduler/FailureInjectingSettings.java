package explorer.scheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import explorer.ExplorerConf;
import org.omg.PortableServer.THREAD_POLICY_ID;
import utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FailureInjectingSettings extends SchedulerSettings {
  private ExplorerConf conf;
  // parameters of the sampling algorithm
  public final int NUM_PROCESSES; // n
  public final int NUM_LIVENESS_ROUNDS; // l
  public final int NUM_PHASES;  // k
  // derived from the parameters
  public final int NUM_EXPECTED_EVENTS;
  public final int NUM_MAJORITY;

  private Random random;
  public final int depth;

  @Expose
  public int seed;
  @Expose
  private List<FailureInjectingSettings.NodeFailure> failures ;
  @Expose
  public int mutationNo; // the mutations of the same seed are enumerated for logging

  public FailureInjectingSettings() {
    NUM_PROCESSES = 3;
    NUM_LIVENESS_ROUNDS = 6;
    NUM_PHASES = 4;
    NUM_EXPECTED_EVENTS = NUM_LIVENESS_ROUNDS * NUM_PHASES * NUM_PROCESSES;
    NUM_MAJORITY = (NUM_PROCESSES / 2) + 1;
    depth = 6;
    mutationNo = 0;
  }

  public FailureInjectingSettings(ExplorerConf conf) {
    this(3, 6, 4, 6, conf.getSeed());
    this.conf = conf;
  }

  private FailureInjectingSettings(ExplorerConf conf, List<NodeFailure> failures, int mutationNo) {
    seed = conf.getSeed();
    NUM_PROCESSES = 3;
    NUM_LIVENESS_ROUNDS = 6;
    NUM_PHASES = 4;
    NUM_EXPECTED_EVENTS = NUM_LIVENESS_ROUNDS * NUM_PHASES * NUM_PROCESSES;
    NUM_MAJORITY = (NUM_PROCESSES / 2) + 1;
    depth = 6;
    this.mutationNo = mutationNo;
    this.failures = failures;
  }

  int numMutators = 0;

  @Override
  public SchedulerSettings mutate() {
    int failureToRemove = random.nextInt(failures.size());

    List<NodeFailure> mutation = new ArrayList<>(failures);
    mutation.remove(failureToRemove);

    // add existing:
    int[] failurePerPhase = new int[NUM_PHASES];
    List<Integer> phases = new ArrayList<>();

    for(int i = 0; i < NUM_PHASES; i++) {
      phases.add(i);
    }

    for(int i = 0; i < mutation.size(); i++) {
      int phaseToFailAt = mutation.get(i).k;
      failurePerPhase[phaseToFailAt]++;
      if (failurePerPhase[phaseToFailAt] == NUM_PROCESSES)
        phases.remove(phaseToFailAt);
    }

      int phaseToFailAt = random.nextInt(phases.size());
      int roundToFailAt = random.nextInt(NUM_LIVENESS_ROUNDS);
      int processToFail = random.nextInt(NUM_PROCESSES);

    mutation.add(new NodeFailure(phaseToFailAt, roundToFailAt, processToFail));

    return new FailureInjectingSettings(conf, mutation, ++numMutators);
  }

  public FailureInjectingSettings(int numProcesses, int numLivenessRounds, int numPhases, int bugDepth, int randomSeed) {
    NUM_PROCESSES = numProcesses;
    NUM_LIVENESS_ROUNDS = numLivenessRounds;
    NUM_PHASES = numPhases;
    NUM_EXPECTED_EVENTS = NUM_LIVENESS_ROUNDS * NUM_PHASES * NUM_PROCESSES;
    NUM_MAJORITY = (NUM_PROCESSES / 2) + 1;

    seed = randomSeed;
    depth = bugDepth;

    random = new Random(seed);

    failures = getRandomFailures();
    //failures = getFailuresToReproduceBug();
  }

  public List<FailureInjectingSettings.NodeFailure> getFailures() {
    return failures;
  }

  private List<FailureInjectingSettings.NodeFailure> getRandomFailures() {
    List<FailureInjectingSettings.NodeFailure> f  = new ArrayList<>();

    int[] failurePerPhase = new int[NUM_PHASES];
    List<Integer> phases = new ArrayList<>();

    for(int i = 0; i < NUM_PHASES; i++) {
      phases.add(i);
    }

    for(int i = 0; i < depth; i++) {
      int phaseToFailAt = random.nextInt(phases.size());
      failurePerPhase[phaseToFailAt] ++;
      if(failurePerPhase[phaseToFailAt] == NUM_PROCESSES)
        phases.remove(phaseToFailAt);

      int roundToFailAt = random.nextInt(NUM_LIVENESS_ROUNDS);
      int processToFail = random.nextInt(NUM_PROCESSES);

      f.add(new NodeFailure(phaseToFailAt, roundToFailAt, processToFail));
    }

    return f;
  }

  private List<FailureInjectingSettings.NodeFailure> getFailuresToReproduceBug() {
    List<FailureInjectingSettings.NodeFailure> f  = new ArrayList<>();
    f.add(new NodeFailure(0, 4, 2));
    f.add(new NodeFailure(1, 2, 2));
    f.add(new NodeFailure(1, 4, 0));
    f.add(new NodeFailure(2, 0, 0));
    f.add(new NodeFailure(2, 0, 1));
    f.add(new NodeFailure(3, 0, 0));
    return f;
  }

  public class NodeFailure {
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
  };

  public String toJsonStr() {
    Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create();
    return gson.toJson(this);
  }

  public SchedulerSettings toObject(String json) {
    Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create();
    return gson.fromJson(json, FailureInjectingSettings.class);
  }
}
