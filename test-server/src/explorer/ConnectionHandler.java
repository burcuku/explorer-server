package explorer;

import explorer.net.Handler;
import explorer.net.MessageSender;
import explorer.scheduler.Scheduler;

public class ConnectionHandler implements Handler {

  final Scheduler scheduler;

  public ConnectionHandler(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  @Override
  public void onConnect(int id, MessageSender sender) {
    System.out.println("Connected Node: " + id);
    synchronized (scheduler) {
      scheduler.onConnect(id, sender);
    }
  }

  @Override
  public void onDisconnect(int id) {
    System.out.println("Disconnected Node: " + id);
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
        System.out.println("==Received from Node: " + id + " Message: " + message );
        scheduler.addNewEvent(id, event);
      }

    }
  }
}
