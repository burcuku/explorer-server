package explorer.scheduler;

import explorer.ExplorerConf;
import explorer.PaxosEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Injects random link failures (i.e., drops random messages) at each round
 * Injects bugDepth number of failures (bugDepth = 7 reproduces the bug)
 */
public class LinkFailureInjector extends Scheduler {
  private static final Logger log = LoggerFactory.getLogger(LinkFailureInjector.class);
  ExplorerConf conf = ExplorerConf.getInstance();

  // variables maintaining the current state of the protocol execution
  private int currentRound;        // between 0 to (NUM_LIVENESS_ROUNDS-1)
  private int currentPhase;    // between 0 to (NUM_PHASES - 1)
  private int toExecuteInCurRound;
  private int executedInCurRound;
  private int droppedFromNextRound; // incremented for the response events in case request events are dropped

  private List<LinkFailureSettings.LinkFailure> failures;

  private List<PaxosEvent.ProtocolRound> rounds;
  // not needed by the standard algorithm, used for optimization (eliminating timeouts for collecting the messages in a round) for Cassandra
  //private List<String> ballots = Arrays.asList("33d9f0f0-08c5-11e7-845e-", "33da1800-08c5-11e7-845e-", "33da3f10-08c5-11e7-845e-", "33da6620-08c5-11e7-845e-", "33da8d30-08c5-11e7-845e-");
  private List<String> ballots;

  // for stats:
  int numSuccessfulRounds = 0, numSuccessfulPhases = 0;

  public LinkFailureInjector(LinkFailureSettings settings) {
    this.settings = settings;
    failures = new ArrayList<>(settings.getFailures());

    ballots = new ArrayList<>();
    rounds = new ArrayList<>();
    rounds.add(PaxosEvent.ProtocolRound.PAXOS_PREPARE);
    currentRound = 0;
    currentPhase = 0;
    toExecuteInCurRound = conf.NUM_PROCESSES;
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

    LinkFailureSettings.LinkFailure match = null;
    for(LinkFailureSettings.LinkFailure nf: failures) {
      if(nf.k == currentPhase && rounds.get(currentRound).ordinal() == nf.r
              && nf.fromProcess == message.getSender() && nf.toProcess == message.getRecv() ) {
        match = nf;
        break;
      }
    }

    if(match != null) {
      failures.remove(match);
      return true;
    }

    return false;
  }

  // the standard algorithm increases the rounds by just collecting messages sent in the round - this is an optimization
  synchronized private void checkUpdateRound() {
    if((toExecuteInCurRound - executedInCurRound) == 0) { // move to next round
      currentRound ++;
      if(executedInCurRound >= ((LinkFailureSettings)settings).NUM_MAJORITY) numSuccessfulRounds ++;

      // update the next round - state machine
      switch(rounds.get(currentRound-1)) {
        case PAXOS_PREPARE:
          rounds.add(PaxosEvent.ProtocolRound.PAXOS_PREPARE_RESPONSE);
          toExecuteInCurRound = conf.NUM_PROCESSES - droppedFromNextRound;
          break;
        case PAXOS_PREPARE_RESPONSE:
          if(toExecuteInCurRound < ((LinkFailureSettings)settings).NUM_MAJORITY) {
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
          if(toExecuteInCurRound < ((LinkFailureSettings)settings).NUM_MAJORITY) {
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

    }
  }

  // refreshes the set of failed processes after each unsuccessful round
  synchronized void moveToNextPhase() {
    currentPhase ++;
    if(executedInCurRound >= ((LinkFailureSettings)settings).NUM_MAJORITY) numSuccessfulPhases ++;
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
