package explorer.scheduler;

import explorer.ExplorerConf;
import explorer.PaxosEvent;

public class NopScheduler extends Scheduler {

  public NopScheduler(ExplorerConf conf) {

  }

  public NopScheduler() {

  }

  @Override
  protected void checkForSchedule() {

  }

  @Override
  public boolean isScheduleCompleted() {
    return false;
  }

  @Override
  public synchronized void addNewEvent(int connectionId, PaxosEvent message) {
    super.addNewEvent(connectionId, message);
    schedule(message);
  }
}
