package explorer.scheduler;

import explorer.ExplorerConf;

public abstract class SchedulerSettings {

  public SchedulerSettings() { }

  public SchedulerSettings(ExplorerConf conf) { }

  public abstract SchedulerSettings mutate();

  public abstract String toJsonStr();

  public abstract SchedulerSettings toObject(String json);
}
