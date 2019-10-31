package explorer.scheduler;

import explorer.net.MessageSender;
import explorer.PaxosEvent;
import explorer.verifier.CassVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

public abstract class Scheduler {
  private static Logger log = LoggerFactory.getLogger(Scheduler.class);
  protected boolean IS_NOP = false; // Reset queries are run in NOP mode, without any faults injected

  protected ConcurrentHashMap<Integer, MessageSender> messageSenders = new ConcurrentHashMap<Integer, MessageSender>(); //connectionId to sender
  protected ConcurrentHashMap<PaxosEvent, Integer> events = new ConcurrentHashMap<PaxosEvent, Integer>(); // event to message sender map

  protected ConcurrentLinkedQueue<PaxosEvent> scheduled = new ConcurrentLinkedQueue<>();

  // synchronize when the messages are done on the receiver side : <receiverId, the list of messages sent to it>
  // once the receiver is free (sends and ACK), allow the next message to be sent to it
  // to allow this, we send the message to the sender of the onflight message!
  private ConcurrentHashMap<Integer, Queue<PaxosEvent>> onFlightToReceiver = new ConcurrentHashMap<Integer, Queue<PaxosEvent>>();
  // maps the onflight messages to its message senders
  private ConcurrentHashMap<PaxosEvent, Integer> onFlightMsgSenders = new ConcurrentHashMap<PaxosEvent, Integer>();

  private boolean isNumEventsBounded = false; //todo configure

  public final synchronized void onConnect(int connectionId, MessageSender sender) {
    messageSenders.put(connectionId, sender);
    onFlightToReceiver.put(connectionId - 1, new LinkedBlockingDeque<>()); // creceiver ids vary from 0 to onnectionId-1
  }

  public final synchronized void onDisconnect(int id) {
    messageSenders.remove(id);
    for(PaxosEvent e: events.keySet())
      System.out.println(e);
  }

  public synchronized void addNewEvent(int connectionId, PaxosEvent message) {
    events.put(message, connectionId); // the intercepted message will be sent back to the connection
  }

  public synchronized final void addAckEvent(int senderId, PaxosEvent message) {
    //System.out.println("Inside addAckEvent: senderId: " + senderId + " verb: " +  message.getVerb());
    // the connection says its finished with the previous event, send next to the connection its next event
    // receiver "connectionId" completed processing its current message
    PaxosEvent e = onFlightToReceiver.get(senderId).remove();

    if(!e.getVerb().equals(message.getVerb()))
      log.error("ERROR: " + " expecting verb: " + message.getVerb()) ;
    // if the receiver has more messages, send them
    if(!onFlightToReceiver.get(senderId).isEmpty()) {
      PaxosEvent next = onFlightToReceiver.get(senderId).peek();
      int senderConId = onFlightMsgSenders.get(next);
      doSendToReceiverNode(next, senderConId);
    }
  }

  protected abstract void checkForSchedule();

  protected synchronized void schedule(PaxosEvent message) {
    int connectionId = 0;
    try{
      connectionId = events.remove(message);
      scheduled.add(message);
      sendToReceiverNode(message, connectionId);
      Thread.sleep(10);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private synchronized void sendToReceiverNode(PaxosEvent message, int connectionId) {
    onFlightToReceiver.get((int)message.getRecv()).add(message);
    onFlightMsgSenders.put(message, connectionId);
    if(onFlightToReceiver.get((int)message.getRecv()).size() == 1) {
      doSendToReceiverNode(message, connectionId);
    }
  }

  private synchronized void doSendToReceiverNode(PaxosEvent message, int connectionId) {
    //log.info("=== Scheduling:  " + message);
    String jsonStr = PaxosEvent.toJsonStr(message);
    MessageSender ms = messageSenders.get(connectionId);
    ms.send(jsonStr);
  }

  public List<PaxosEvent> getSchedule() {
    return new ArrayList<>(scheduled);
  }

  public abstract boolean isScheduleCompleted();

  public boolean isExecutionCompleted() {
   if(isNumEventsBounded)
     return isScheduleCompleted();

    if (!isScheduleCompleted()) return false;

    for(Queue<PaxosEvent> queue: onFlightToReceiver.values()) {
      if(!queue.isEmpty()) return false;
    }
    return true;
  }

  public void onExecutionCompleted() {
    StringBuilder sb = new StringBuilder("Schedule: ");
    for(PaxosEvent e: scheduled) {
      //System.out.println(e);
      sb.append("\n").append(PaxosEvent.getEventId(e) + " " + e.getPayload());
    }
    log.debug(sb.toString());
    new CassVerifier().verify();
  }

  public void reset() {
    events.clear();
    scheduled.clear();
  }

  public void setISNOP(boolean v) {
    IS_NOP = v;
  }
}
