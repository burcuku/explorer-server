package explorer.scheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import explorer.ExplorerConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SchedulerSettings {
  private static final Logger log = LoggerFactory.getLogger(Scheduler.class);

  public SchedulerSettings() { }

  public SchedulerSettings(ExplorerConf conf) { }

  public abstract SchedulerSettings mutate();

  public static String toJsonStr(SchedulerSettings obj) {
    if(obj instanceof NodeFailureSettings) {
      Gson gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()
          .create();
      return gson.toJson(obj);
    } else {
      log.error("Not supported SchedulerSetting for serialization.");
      return "";
    }
  }

  //todo add SchedulerSettings type and deserialize accordingly
  public static SchedulerSettings toObject(String json) {
    Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create();
    return gson.fromJson(json, NodeFailureSettings.class);
  }
}
