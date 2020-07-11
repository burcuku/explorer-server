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
  public String getStats() {
    return "";
  }
}
