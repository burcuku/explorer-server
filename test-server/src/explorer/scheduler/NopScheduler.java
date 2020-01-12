package explorer.scheduler;

import explorer.ExplorerConf;
import explorer.PaxosEvent;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

public class NopScheduler extends Scheduler {

  @Override
  protected void checkForSchedule() {

  }

  @Override
  public boolean isScheduleCompleted() {
    return false;
  }

  @Override
  public void failNode(int nodeId) {
    throw new NotImplementedException();
  }

  @Override
  public void resumeNode(int nodeId) {
    throw new NotImplementedException();
  }


  @Override
  public synchronized void addNewEvent(int connectionId, PaxosEvent message) {
    super.addNewEvent(connectionId, message);
    schedule(message);
  }

  @Override
  public void runUntilRound(int i) {
    throw new NotImplementedException();
  }

  @Override
  public void runToCompletion() {

  }
}
