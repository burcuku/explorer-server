package explorer;

import explorer.net.Handler;
import explorer.net.MessageSender;
import explorer.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionHandler implements Handler {

  private static final Logger log = LoggerFactory.getLogger(ConnectionHandler.class);
  final Scheduler scheduler;

  public ConnectionHandler(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  @Override
  public void onConnect(int id, MessageSender sender) {
    log.info("Connected Node: " + id);
    synchronized (scheduler) {
      scheduler.onConnect(id, sender);
    }
  }

  @Override
  public void onDisconnect(int id) {
    log.info("Disconnected Node: " + id);
    synchronized (scheduler) {
      scheduler.onDisconnect(id);
    }
  }

  @Override
  public void onReceive(int id, String message) {
    //System.out.println("==Received from Node: " + id + " Message: " + message );

    synchronized (scheduler) {
      PaxosEvent event = PaxosEvent.toObject(message);

      if(event.isAckEvent())
        scheduler.addAckEvent((int)event.getSender(), event); // the sender of the ack, event
      else  {
        log.debug("==Received from Node: " + id + " Message: " + message );
        scheduler.addNewEvent(id, event);
      }

    }
  }
}
