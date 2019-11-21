package explorer.scheduler;

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

  // variables maintaining the current state of the protocol execution
  private int currentRound;        // between 0 to (NUM_LIVENESS_ROUNDS-1)
  private int currentPhase;    // between 0 to (NUM_PHASES - 1)
  private int toExecuteInCurRound;
  private int executedInCurRound;
  private int droppedFromNextRound; // incremented for the response events in case request events are dropped

  private List<FailureInjectingSettings.NodeFailure> failures;
  private Set<Integer> failedProcesses;

  private List<PaxosEvent.ProtocolRound> rounds;
  // not needed by the standard algorithm, used for optimization (eliminating timeouts for collecting the messages in a round) for Cassandra
  //private List<String> ballots = Arrays.asList("33d9f0f0-08c5-11e7-845e-", "33da1800-08c5-11e7-845e-", "33da3f10-08c5-11e7-845e-", "33da6620-08c5-11e7-845e-", "33da8d30-08c5-11e7-845e-");
  private List<String> ballots;

  public FailureInjectingScheduler(FailureInjectingSettings settings) {
    this.settings = settings;
    failures = new ArrayList<>(settings.getFailures());

    failedProcesses = new HashSet<>();
    ballots = new ArrayList<>();
    rounds = new ArrayList<>();
    rounds.add(PaxosEvent.ProtocolRound.PAXOS_PREPARE);
    currentRound = 0;
    currentPhase = 0;
    toExecuteInCurRound = settings.NUM_PROCESSES;
    executedInCurRound = 0;
    droppedFromNextRound = 0;
  }

  @Override
  synchronized public void addNewEvent(int connectionId, PaxosEvent message) {
    super.addNewEvent(connectionId, message);

    if(message.getVerb().equals("PAXOS_PREPARE") && ballots.size() == currentPhase) {
      ballots.add(message.getBallot());
      //log.debug("Added:  " + message.getBallot());
    }

    checkForSchedule();
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

    FailureInjectingSettings.NodeFailure match = null;
    for(FailureInjectingSettings.NodeFailure nf: failures) {
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

      // inform coverage strategy
      coverageStrategy.onRoundComplete(rounds.get(currentRound-1).toString(), failedProcesses);

      // update the next round - state machine
      switch(rounds.get(currentRound-1)) {
        case PAXOS_PREPARE:
          rounds.add(PaxosEvent.ProtocolRound.PAXOS_PREPARE_RESPONSE);
          toExecuteInCurRound = ((FailureInjectingSettings)settings).NUM_PROCESSES - droppedFromNextRound;
          break;
        case PAXOS_PREPARE_RESPONSE:
          if(toExecuteInCurRound < ((FailureInjectingSettings)settings).NUM_MAJORITY) {
            coverageStrategy.onRequestPhaseComplete(rounds.get(currentRound-1).toString(), failedProcesses);
            rounds.add(PaxosEvent.ProtocolRound.PAXOS_PREPARE);
            currentPhase ++;
            failedProcesses.clear();
          }
          else rounds.add(PaxosEvent.ProtocolRound.PAXOS_PROPOSE);
          toExecuteInCurRound = ((FailureInjectingSettings)settings).NUM_PROCESSES;
          break;
        case PAXOS_PROPOSE:
          rounds.add(PaxosEvent.ProtocolRound.PAXOS_PROPOSE_RESPONSE);
          toExecuteInCurRound = ((FailureInjectingSettings)settings).NUM_PROCESSES - droppedFromNextRound;
          break;
        case PAXOS_PROPOSE_RESPONSE: //todo make it more accurate with replies! (even with majority of replies can turn back to PREPARE)
          if(toExecuteInCurRound < ((FailureInjectingSettings)settings).NUM_MAJORITY) {
            coverageStrategy.onRequestPhaseComplete(rounds.get(currentRound-1).toString(), failedProcesses);
            rounds.add(PaxosEvent.ProtocolRound.PAXOS_PREPARE);
            currentPhase ++;
            failedProcesses.clear();
          }
          else rounds.add(PaxosEvent.ProtocolRound.PAXOS_COMMIT);
          toExecuteInCurRound = ((FailureInjectingSettings)settings).NUM_PROCESSES;
          break;
        case PAXOS_COMMIT:
          rounds.add(PaxosEvent.ProtocolRound.PAXOS_COMMIT_RESPONSE);
          toExecuteInCurRound = ((FailureInjectingSettings)settings).NUM_PROCESSES - droppedFromNextRound;
          break;
        case PAXOS_COMMIT_RESPONSE:
          coverageStrategy.onRequestPhaseComplete(rounds.get(currentRound-1).toString(), failedProcesses);
          rounds.add(PaxosEvent.ProtocolRound.PAXOS_PREPARE);
          toExecuteInCurRound = ((FailureInjectingSettings)settings).NUM_PROCESSES;
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

}
