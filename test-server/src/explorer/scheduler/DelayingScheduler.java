package explorer.scheduler;

import explorer.PaxosEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DelayingScheduler extends Scheduler {
    //todo parametrize numPhases, numNodes per phase

    //todo scheduler conf
    private int NUM_ROUNDS = 18;
    private int NUM_MESSAGES_PER_ROUND = 3;

    private ConcurrentHashMap<PaxosEvent, Integer> toDelay = new ConcurrentHashMap<>(); //message, newRoundNo
    private ConcurrentHashMap<PaxosEvent, Integer> delayed = new ConcurrentHashMap<>(); //message, newRoundNo

    private int[] numRegMessagesPerRound = new int[NUM_ROUNDS];
    private int[] numIrregMessagesPerRound = new int[NUM_ROUNDS];

    private AtomicInteger currentRound = new AtomicInteger(0);
    private AtomicInteger numRegProcessedInCurrentRound = new AtomicInteger(0);
    private AtomicInteger numIrregProcessedInCurrentRound = new AtomicInteger(0);

    private List<List<Integer>> idsOfDelayedMsgs = new ArrayList<List<Integer>>();
    private List<PaxosEvent> responsesToDelay = new ArrayList<PaxosEvent>(); //keeps the request messages whose messages will be delayed

    // d values - number of messages to delay from each round
    private int[] d = new int[NUM_ROUNDS];

    Random[] random = new Random[NUM_MESSAGES_PER_ROUND ];

    private boolean GUIDE_FOR_BUG = true;

    public DelayingScheduler() {
        Arrays.fill(numRegMessagesPerRound, NUM_MESSAGES_PER_ROUND);
        Arrays.fill(numIrregMessagesPerRound, 0);

        for(int i = 0; i < NUM_MESSAGES_PER_ROUND; i++)
            random[i] = new Random(12345678 + i);

        for(int i = 0; i < NUM_ROUNDS; i++)
            idsOfDelayedMsgs.add(new ArrayList<>());


        // todo read in d's
        d[4] = 1;
        d[8] = 1;
        d[10] = 2;

        // all delays are from requests!
        for(int i = 0; i < NUM_ROUNDS; i++) {
            Set<Integer> events = new HashSet<>(Arrays.asList(0, 1, 2));
            for(int j = 0; j < d[i]; j++) {
                int selected = random[0].nextInt(events.size());
                idsOfDelayedMsgs.get(i).add(selected);
                events.remove(selected);
            }
        }
    }

    // the internal id of a message in a round is:  (sender+receiver-request) in {0, 1, 2}
    private int getInternalIdInRound(PaxosEvent e) {
        return (int)(e.getRecv() + e.getSender() - e.getClientRequest());
    }

    public synchronized void addNewEvent(int connectionId, PaxosEvent message) {
        super.addNewEvent(connectionId, message);

        // check if it will be delayed:
        if(idsOfDelayedMsgs.get(message.getRoundNumber()).contains(getInternalIdInRound(message))) {
            toDelay.put(message, -1);
            numRegMessagesPerRound[message.getRoundNumber()] --;
            //System.err.println("Delayed: " + message + " numMessagesPerRound " + numMessagesPerRound[message.getRoundNumber()]);
            if(message.isRequest()) { // the response is delayed to the same round
                numRegMessagesPerRound[message.getRoundNumber() + 1] --;
                responsesToDelay.add(message);
            }
            checkUpdateRound();
        }

        // we are sure that the delayed ones are requests (the rounds we delay from are requests)
        // check if the message is transitively delayed:
        PaxosEvent request = null;
        for(PaxosEvent m: responsesToDelay) {
            if(message.isResponseOf(m)) {
                toDelay.put(message, delayed.get(m)); // put it to the same round, matches numMessagesPerRound
                request = m;
            }
        }
        if(request != null) responsesToDelay.remove(request);

        checkForSchedule();
    }

    private synchronized void selectDelayedToSchedule() {
        Set<PaxosEvent> delayedKeys = toDelay.keySet();

        for(PaxosEvent m: delayedKeys) { // executed once for each delayed event per round
            if(toDelay.get(m) != -1) return; // its round is already assigned (it is a response)

            //schedule now with probability (1 / (k-j-1)) where we are in round j
            int r = random[(int)m.getRecv()].nextInt(NUM_ROUNDS - currentRound.get());
            boolean scheduleNow = (r == 0);

            if(scheduleNow) {
                toDelay.put(m, currentRound.get());
                numIrregMessagesPerRound[currentRound.get()] ++;
                numIrregMessagesPerRound[currentRound.get()] ++; // its response
            }
        }
    }

  protected synchronized void checkForSchedule() {
        //System.out.println("Checking for normal events schedule"+ " In thread: " + Thread.currentThread().getId()) ;
        Set<PaxosEvent> keys = events.keySet();
        for(PaxosEvent m: keys) {
            if(isOkToSchedule(m)) {
                schedule(m);
                checkUpdateRound();
                checkForSchedule();
                return;
            }
        }
    }

    // returns true if the schedule has completed
    private synchronized void checkUpdateRound() {
        if(numRegProcessedInCurrentRound.get() == numRegMessagesPerRound[currentRound.get()]
            && numIrregProcessedInCurrentRound.get() == numIrregMessagesPerRound[currentRound.get()]) {
            currentRound.incrementAndGet();
            numRegProcessedInCurrentRound.set(0);
            numIrregProcessedInCurrentRound.set(0);

            selectDelayedToSchedule();
        }
    }

    private synchronized boolean isOkToSchedule(PaxosEvent message) {
        // after all regular messages, schedule the delayed ones
        if(numRegProcessedInCurrentRound.get() == numRegMessagesPerRound[currentRound.get()] && toDelay.containsKey(message) && currentRound.get() == toDelay.get(message)) {
            //delayedToRoundFrom.get(currentRound.get()).remove((Integer)message.getRoundNumber());
            int round = toDelay.remove(message);
            delayed.put(message, round);
            numIrregProcessedInCurrentRound.incrementAndGet();
            return true;
        }

        // not-delayed and its turn
        if(message.getRoundNumber() == currentRound.get() && !toDelay.containsKey(message)) {
           numRegProcessedInCurrentRound.incrementAndGet();
            return true;
        }
        return false;
    }

    private void printEvents() {
        System.out.println("--- Increasing round to: " + currentRound.get());
        System.out.println("Events to schedule: ");
        for(PaxosEvent e: events.keySet()) {
            System.out.println(e);
        }
        System.out.println("toDelay: ");
        for(PaxosEvent e: toDelay.keySet()) {
            System.out.println(e + " R: " + toDelay.get(e) + " Its orig round was: " + e.getRoundNumber());
        }
    }

    public List<PaxosEvent> getSchedule() {
        return new ArrayList<PaxosEvent>(scheduled);
    }

}
