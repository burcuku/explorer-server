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
    throw new RuntimeException("Not supported for this Scheduler: " + this.getClass().getName());
  }

  @Override
  public void resumeNode(int nodeId) {
    throw new RuntimeException("Not supported for this Scheduler: " + this.getClass().getName());
  }

  @Override
  public void runUntilRound(int i) {
    throw new RuntimeException("Not supported for this Scheduler: " + this.getClass().getName());
  }

  @Override
  public void runForRounds(int numRounds) {
    throw new RuntimeException("Not supported for this Scheduler: " + this.getClass().getName());
  }

  @Override
  public void runToCompletion() {
    throw new RuntimeException("Not supported for this Scheduler: " + this.getClass().getName());
  }

  @Override
  public String getStats() {
    return "";
  }
}
