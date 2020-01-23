package explorer.scheduler;

import explorer.ExplorerConf;
import explorer.PaxosEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.FileUtils;

import java.util.*;

/**
 * The NodeFailureInjector injects failures into the synchronous non-faulty executions of a system
 * (The current version introduces only node failures where a failing node cannot receive/send any messages)
 */
public class NodeFailureInjector extends Scheduler {
  private static final Logger log = LoggerFactory.getLogger(NodeFailureInjector.class);
  ExplorerConf conf = ExplorerConf.getInstance();

  // variables maintaining the current state of the protocol execution
  private int currentRound;        // between 0 to (NUM_LIVENESS_ROUNDS-1)
  private int currentPhase;    // between 0 to (NUM_PHASES - 1)
  private int toExecuteInCurRound;
  private int executedInCurRound;
  private int droppedFromNextRound; // incremented for the response events in case request events are dropped

  private final int period;  // period of clearing failed processes (set to NUM_LIVENESS_ROUNDS)

  private List<NodeFailureSettings.NodeFailure> failures;
  private Set<Integer> failedProcesses;

  private List<PaxosEvent.ProtocolRound> rounds;
  // not needed by the standard algorithm, used for optimization (eliminating timeouts for collecting the messages in a round) for Cassandra
  //private List<String> ballots = Arrays.asList("33d9f0f0-08c5-11e7-845e-", "33da1800-08c5-11e7-845e-", "33da3f10-08c5-11e7-845e-", "33da6620-08c5-11e7-845e-", "33da8d30-08c5-11e7-845e-");
  private List<String> ballots;

  // used for online control of the schedule by the user
  boolean suspended;
  final boolean online;

  // for stats:
  int numSuccessfulRounds = 0, numSuccessfulPhases = 0;

  public NodeFailureInjector(NodeFailureSettings settings) {
    this.settings = settings;
    failures = new ArrayList<>(settings.getFailures());

    failedProcesses = new HashSet<>();
    ballots = new ArrayList<>();
    rounds = new ArrayList<>();
    rounds.add(PaxosEvent.ProtocolRound.PAXOS_PREPARE);
    currentRound = 0;
    currentPhase = 0;
    toExecuteInCurRound = conf.NUM_PROCESSES;
    executedInCurRound = 0;
    droppedFromNextRound = 0;

    period = conf.linkEstablishmentPeriod;

    if(settings.equals(NodeFailureSettings.ONLINE_CONTROLLED)) {
      log.debug("Using online control of the failing nodes.");
      if(ExplorerConf.getInstance().logResult)
        FileUtils.writeToFile(ExplorerConf.getInstance().resultFile, "Using online control of the failing nodes.", true);
      online = true;
      suspended = true;
    } else {
      log.debug("Using failures: " + settings.getFailures() + " seed: " + settings.seed);
      if(ExplorerConf.getInstance().logResult)
        FileUtils.writeToFile(ExplorerConf.getInstance().resultFile, "Seed for failures: " + settings.seed, true);
      online = false;
      suspended = false;
    }
  }

  @Override
  synchronized public void addNewEvent(int connectionId, PaxosEvent message) {
    super.addNewEvent(connectionId, message);

    if(message.getVerb().equals("PAXOS_PREPARE") && ballots.size() == currentPhase) {
      ballots.add(message.getBallot());
      //log.debug("Added:  " + message.getBallot());
    }

    if(!suspended) checkForSchedule();
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

    NodeFailureSettings.NodeFailure match = null;
    for(NodeFailureSettings.NodeFailure nf: failures) {
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
      if(executedInCurRound >= ((NodeFailureSettings)settings).NUM_MAJORITY) numSuccessfulRounds ++;

      // inform coverage strategy
      coverageStrategy.onRoundComplete(rounds.get(currentRound-1).toString(), failedProcesses);

      // update the next round - state machine
      switch(rounds.get(currentRound-1)) {
        case PAXOS_PREPARE:
          rounds.add(PaxosEvent.ProtocolRound.PAXOS_PREPARE_RESPONSE);
          toExecuteInCurRound = conf.NUM_PROCESSES - droppedFromNextRound;
          break;
        case PAXOS_PREPARE_RESPONSE:
          if(toExecuteInCurRound < ((NodeFailureSettings)settings).NUM_MAJORITY) {
            coverageStrategy.onRequestPhaseComplete(rounds.get(currentRound-1).toString(), failedProcesses);
            rounds.add(PaxosEvent.ProtocolRound.PAXOS_PREPARE);
            moveToNextPhase();
          }
          else rounds.add(PaxosEvent.ProtocolRound.PAXOS_PROPOSE);
          toExecuteInCurRound = conf.NUM_PROCESSES;
          break;
        case PAXOS_PROPOSE:
          rounds.add(PaxosEvent.ProtocolRound.PAXOS_PROPOSE_RESPONSE);
          toExecuteInCurRound = conf.NUM_PROCESSES - droppedFromNextRound;
          break;
        case PAXOS_PROPOSE_RESPONSE: //todo make it more accurate with replies! (even with majority of replies can turn back to PREPARE)
          if(toExecuteInCurRound < ((NodeFailureSettings)settings).NUM_MAJORITY) {
            coverageStrategy.onRequestPhaseComplete(rounds.get(currentRound-1).toString(), failedProcesses);
            rounds.add(PaxosEvent.ProtocolRound.PAXOS_PREPARE);
            moveToNextPhase();
          }
          else rounds.add(PaxosEvent.ProtocolRound.PAXOS_COMMIT);
          toExecuteInCurRound = conf.NUM_PROCESSES;
          break;
        case PAXOS_COMMIT:
          rounds.add(PaxosEvent.ProtocolRound.PAXOS_COMMIT_RESPONSE);
          toExecuteInCurRound = conf.NUM_PROCESSES - droppedFromNextRound;
          break;
        case PAXOS_COMMIT_RESPONSE:
          coverageStrategy.onRequestPhaseComplete(rounds.get(currentRound-1).toString(), failedProcesses);
          rounds.add(PaxosEvent.ProtocolRound.PAXOS_PREPARE);
          toExecuteInCurRound = conf.NUM_PROCESSES;
          moveToNextPhase();
          break;
        default:
          log.error("Invalid protocol state");
      }

      // update values related to failures for the current round:
      executedInCurRound = 0;
      droppedFromNextRound = 0;

      // reset failed processes after each period number of rounds
      checkClearFailedProcesses();

      //log.debug("Moved to the next round: " + rounds.get(currentRound));
      // We moved to next round, notify the clients and update failure structures with requested settings
      if(online && runUntilRound == currentRound) {
        suspended = true;
        synchronized (o) {
          o.notify();
        }
      }
    }
  }


  synchronized void checkClearFailedProcesses() {
    // reset failed processes after each period number of rounds if
    // it does not get reset in moveToNextPhase (in the case when period == conf.NUM_ROUNDS_IN_PROTOCOL)
    if(period != conf.NUM_ROUNDS_IN_PROTOCOL && (currentRound + 1) % period == 0)
      failedProcesses.clear();
  }

  // refreshes the set of failed processes after each unsuccessful round
  synchronized void moveToNextPhase() {
    currentPhase ++;
    if(executedInCurRound >= ((NodeFailureSettings)settings).NUM_MAJORITY) numSuccessfulPhases ++;

    // reset the quorum of nodes if
    // the settings are not assigned online AND
    // the reestablishment of the links correspond to the actual number of rounds in a phase
    // (assumption: the completion of an unsuccessful phase (together with its timeouts) corresponds to the full-length execution of a successful exec)
    // timeout-based scheduler will reestablish links in period number of rounds * expected time
    // this Cassandra-specific implementation is aware of the phase executions and optimizes/increases accuracy using this info
    if(!online && period == conf.NUM_ROUNDS_IN_PROTOCOL) failedProcesses.clear();
  }

  // Customized for Cassandra example - the default way of detecting the messages in a round is to collect messages for some timeout
  synchronized private boolean isOfCurrentPhase(PaxosEvent m) {
    boolean isPrepareOfCurrentPhase = (m.getVerb().equals(PaxosEvent.ProtocolRound.PAXOS_PREPARE.toString()) && m.getBallot().equals(ballots.get(currentPhase)));
    boolean isNonPrepare = !m.getVerb().equals(PaxosEvent.ProtocolRound.PAXOS_PREPARE.toString()); // if the preceding messages are scheduled, it is of current phase
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

  Object o = new Object(); // used for notification of the client

  public void failNode(int nodeId) {
    if(nodeId >= conf.NUM_PROCESSES) {
      log.error("Cannot fail node " + nodeId + ". No such node.");
    } else {
      failedProcesses.add(nodeId);
    }
  }

  public void resumeNode(int nodeId) {
    if(nodeId >= conf.NUM_PROCESSES) {
      log.error("Cannot resume node " + nodeId + ". No such node.");
    } else {
      failedProcesses.remove(nodeId);
    }
  }

  int runUntilRound = 0;

  public void runUntilRound(int i) {
    if(currentRound >= i) {
      log.error("Round " + i + " has already been executed. Is scheduler suspended ? " + suspended);
      return;
    }
    runUntilRound = i;// blocks until the round is reached and the runnable is executed (Failures to inject are set)
    suspended = false;

    Thread t = new Thread(() -> checkForSchedule());
    t.start();

    try {
      synchronized (o) {
        while(currentRound < i)
          o.wait();  // blocks until the round is reached and the runnable is executed (Failures to inject are set)
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

  public void runForRounds(int numRounds) {
    if(numRounds <= 0) {
      log.error("Cannot run for " + numRounds + "rounds. Is scheduler suspended ? " + suspended);
      return;
    }
    runUntilRound = currentRound + numRounds;// blocks until the round is reached and the runnable is executed (Failures to inject are set)
    suspended = false;

    Thread t = new Thread(() -> checkForSchedule());
    t.start();

    try {
      synchronized (o) {
        while(currentRound < runUntilRound) // expected to call by a single test method/thread
          o.wait();  // blocks until the round is reached and the runnable is executed (Failures to inject are set)
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

  @Override
  public synchronized void runToCompletion() {
    suspended = false;
    checkForSchedule();
  }

  @Override
  public String getStats() {
    StringBuffer sb = new StringBuffer();
    sb.append("Num successful rounds: ").append(numSuccessfulRounds).append("\n");
    sb.append("Num rounds: ").append(currentRound).append("\n");
    sb.append("Num successful phases: ").append(numSuccessfulPhases).append("\n");
    sb.append("Num phases: ").append(currentPhase).append("\n");
    sb.append("Num messages: ").append(scheduled.size()).append("\n");
    sb.append("\n");
    return sb.toString();
  }


}
