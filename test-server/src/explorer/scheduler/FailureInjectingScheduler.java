package explorer.scheduler;

import explorer.ExplorerConf;
import explorer.PaxosEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The FailureInjectingScheduler injects failures into the synchronous non-faulty executions of a system
 * (The current version introduces only node failures where a failing node cannot receive/send any messages)
 */
public class FailureInjectingScheduler extends Scheduler {
  private static final Logger log = LoggerFactory.getLogger(FailureInjectingScheduler.class);

  // parameters of the sampling algorithm
  public final int NUM_PROCESSES; // n
  public final int NUM_LIVENESS_ROUNDS; // l
  public final int NUM_PHASES;  // k
  // derived from the parameters
  public final int NUM_EXPECTED_EVENTS;
  public final int NUM_MAJORITY;

  private Random random;
  public final int depth;  // d
  public int seed;

  // variables maintaining the current state of the protocol execution
  private int currentRound;        // between 0 to (NUM_LIVENESS_ROUNDS-1)
  private int currentPhase;    // between 0 to (NUM_PHASES - 1)
  private int toExecuteInCurRound;
  private int executedInCurRound;
  private int droppedFromNextRound; // incremented for the response events in case request events are dropped

  private List<NodeFailure> failures ;
  private Set<Integer> failedProcesses;


  // for customized round identification for Cassandra's Paxos
  enum ProtocolRound {PAXOS_PREPARE, PAXOS_PREPARE_RESPONSE, PAXOS_PROPOSE, PAXOS_PROPOSE_RESPONSE, PAXOS_COMMIT, PAXOS_COMMIT_RESPONSE};
  private List<ProtocolRound> rounds;
  // not needed by the standard algorithm, used for optimization (eliminating timeouts for collecting the messages in a round) for Cassandra
  //private List<String> ballots = Arrays.asList("33d9f0f0-08c5-11e7-845e-", "33da1800-08c5-11e7-845e-", "33da3f10-08c5-11e7-845e-", "33da6620-08c5-11e7-845e-", "33da8d30-08c5-11e7-845e-");

  private List<String> ballots;

  private class NodeFailure {
    int k; // in which request does it happen?
    int r; // at which round does it happen?
    int process; // which process fails?

    public NodeFailure(int k, int r, int p) {
      this.k = k;
      this.r = r;
      this.process = p;
    }
  };

  public FailureInjectingScheduler(ExplorerConf conf) {
    this(3, 6, 4, 6, conf.getSeed());
  }

  public FailureInjectingScheduler(int numProcesses, int numLivenessRounds, int numPhases, int bugDepth, int randomSeed) {
    NUM_PROCESSES = numProcesses;
    NUM_LIVENESS_ROUNDS = numLivenessRounds;
    NUM_PHASES = numPhases;
    NUM_EXPECTED_EVENTS = NUM_LIVENESS_ROUNDS * NUM_PHASES * NUM_PROCESSES;
    NUM_MAJORITY = (NUM_PROCESSES / 2) + 1;

    seed = randomSeed;
    depth = bugDepth;

    reset();
  }

  @Override
  synchronized public void reset() {
    random = new Random(seed);
    seed ++;
    failures = new ArrayList<>();
    failedProcesses = new HashSet<>();
    ballots = new ArrayList<>();

    setRandomFailures();
    //setFailuresToReproduceBug();

    rounds = new ArrayList<>();
    rounds.add(ProtocolRound.PAXOS_PREPARE);
    currentRound = 0;
    currentPhase = 0;
    toExecuteInCurRound = NUM_PROCESSES;
    executedInCurRound = 0;
    droppedFromNextRound = 0;
  }

  private void setRandomFailures() {
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

      failures.add(new NodeFailure(phaseToFailAt, roundToFailAt, processToFail));
    }
  }

  private void setFailuresToReproduceBug() {
    failures.add(new NodeFailure(0, 4, 2));
    failures.add(new NodeFailure(1, 2, 2));
    failures.add(new NodeFailure(1, 4, 0));
    failures.add(new NodeFailure(2, 0, 0));
    failures.add(new NodeFailure(2, 0, 1));
    failures.add(new NodeFailure(3, 0, 0));
  }

  @Override
  synchronized public void addNewEvent(int connectionId, PaxosEvent message) {
    super.addNewEvent(connectionId, message);

    if(message.getVerb().equals("PAXOS_PREPARE") && ballots.size() == currentPhase) {
      ballots.add(message.getBallot());
      //log.debug("Added:  " + message.getBallot());
    }

    if(IS_NOP) schedule(message);
    else checkForSchedule();
  }

  @Override
  synchronized protected void checkForSchedule() {
    Set<PaxosEvent> keys = events.keySet();
    for(PaxosEvent m: keys) {
      if(isOfCurrentRound(m)) {
        if(isToDrop(m)) {
          log.info("Dropped message: " + m.toString() + " " + m.getPayload());
          events.remove(m);
          toExecuteInCurRound --;
          if(m.isRequest()) droppedFromNextRound ++;
        } else {
          log.debug("=== Scheduling:  " + m);
          if(ballots.get(currentPhase) == null)
            ballots.add(currentPhase, m.getBallot());
          schedule(m);
          executedInCurRound ++;
        }
        checkUpdateRound();
        checkForSchedule();
        return;
      }
    }
  }

  // The current phase is determined by the ballot numbers and round of the protocol step
  // After the PREPARE of the first request,
  //   all the messages regarding that request are executed before SCHEDULING the PREPARE of another one
  // has side effect!
  synchronized private boolean isToDrop(PaxosEvent message) {
    assert(isOfCurrentRound(message));
    int processOfMessage = (int)(message.isRequest() ? message.getRecv() : message.getSender());
    if(failedProcesses.contains(processOfMessage)) return true;

    NodeFailure match = null;
    for(NodeFailure nf: failures) {
      if(nf.k == currentPhase && rounds.get(currentRound).ordinal() == nf.r && nf.process == processOfMessage) {
        match = nf;
        break;
      }
    }

    if(match != null) {
      failures.remove(match);
      failedProcesses.add(match.process);
      return true;
    }

    return false;
  }

  // the standard algorithm increases the rounds by just collecting messages sent in the round - this is an optimization
  synchronized private void checkUpdateRound() {
    if((toExecuteInCurRound - executedInCurRound) == 0) { // move to next round
      currentRound ++;

      // update the next round - state machine
      switch(rounds.get(currentRound-1)) {
        case PAXOS_PREPARE:
          rounds.add(ProtocolRound.PAXOS_PREPARE_RESPONSE);
          toExecuteInCurRound = NUM_PROCESSES - droppedFromNextRound;
          break;
        case PAXOS_PREPARE_RESPONSE:
          if(toExecuteInCurRound < NUM_MAJORITY) {
            rounds.add(ProtocolRound.PAXOS_PREPARE);
            currentPhase ++;
            failedProcesses.clear();
          }
          else rounds.add(ProtocolRound.PAXOS_PROPOSE);
          toExecuteInCurRound = NUM_PROCESSES;
          break;
        case PAXOS_PROPOSE:
          rounds.add(ProtocolRound.PAXOS_PROPOSE_RESPONSE);
          toExecuteInCurRound = NUM_PROCESSES - droppedFromNextRound;
          break;
        case PAXOS_PROPOSE_RESPONSE: //todo make it more accurate with replies! (even with majority of replies can turn back to PREPARE)
          if(toExecuteInCurRound < NUM_MAJORITY) {
            rounds.add(ProtocolRound.PAXOS_PREPARE);
            currentPhase ++;
            failedProcesses.clear();
          }
          else rounds.add(ProtocolRound.PAXOS_COMMIT);
          toExecuteInCurRound = NUM_PROCESSES;
          break;
        case PAXOS_COMMIT:
          rounds.add(ProtocolRound.PAXOS_COMMIT_RESPONSE);
          toExecuteInCurRound = NUM_PROCESSES - droppedFromNextRound;
          break;
        case PAXOS_COMMIT_RESPONSE:
          rounds.add(ProtocolRound.PAXOS_PREPARE);
          toExecuteInCurRound = NUM_PROCESSES;
          currentPhase ++;
          failedProcesses.clear();
          break;
        default:
          log.error("Invalid protocol state");
      }

      //log.debug("Moved to the next round: " + rounds.get(currentRound));

      executedInCurRound = 0;
      droppedFromNextRound = 0;
    }
  }

  // Customized for Cassandra example - the default way of detecting the messages in a round is to collect messages for some timeout
  synchronized private boolean isOfCurrentPhase(PaxosEvent m) {
    boolean isPrepareOfCurrentPhase = (m.getVerb().equals(ProtocolRound.PAXOS_PREPARE.toString()) && m.getBallot().equals(ballots.get(currentPhase)));
    boolean isNonPrepare = !m.getVerb().equals(ProtocolRound.PAXOS_PREPARE.toString()); // if the preceding messages are scheduled, it is of current phase
    return isPrepareOfCurrentPhase || isNonPrepare;
  }

  // Customized for Cassandra example - the default way of detecting the messages in a round is to collect messages for some timeout
  synchronized private boolean isOfCurrentRound(PaxosEvent m) {
      return isOfCurrentPhase(m) && m.getVerb().equals(rounds.get(currentRound).toString());
  }

  @Override
  public boolean isScheduleCompleted() {
    // NOTE: Execution goes beyond this if there are events to onFlight to deliver
    return scheduled.size() >= 36; // todo parametrize
  }

}
