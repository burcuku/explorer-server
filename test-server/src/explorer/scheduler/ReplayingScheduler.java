package explorer.scheduler;

import explorer.ExplorerConf;
import explorer.PaxosEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ReplayingScheduler extends Scheduler {

  private static final Logger log = LoggerFactory.getLogger(ReplayingScheduler.class);

  private final List<String> scheduleToReplay;
  private final List<String> eventsToDrop;
  private Map<String, PaxosEvent> eventsToSchedule;
  private String nextMessage;

  private final String DROP_MARK = "@D";


  public ReplayingScheduler(ExplorerConf conf) {
    eventsToSchedule = new HashMap<String, PaxosEvent>();
    scheduleToReplay = readSchedule(conf.schedulerFile);
    eventsToDrop = readEventsToDrop();
    nextMessage = scheduleToReplay.get(scheduled.size());
  }

  private List<String> readSchedule(String schedulerFile) {
    try {
      return Files.readAllLines(Paths.get(schedulerFile))
          .stream()
          .map(String::trim)
          .filter(s -> !s.isEmpty() && !s.startsWith("//"))
          .map(s -> s.split("//")[0].trim())
          .collect(Collectors.toList());
    } catch (IOException e) {
      log.error("Can't read schedule file", e);
    }
    return new ArrayList<>();
  }

  private List<String> readEventsToDrop() {
    try {
      return Files.readAllLines(Paths.get(ExplorerConf.getInstance().schedulerFile))
          .stream()
          .map(String::trim)
          .filter(s -> !s.isEmpty() && s.startsWith("//"))
          //.map(s -> s.split("//")[1].trim())
          .filter(s -> s.split("//").length > 0 )
          .filter(s -> s.split("//")[1].trim().startsWith(DROP_MARK))
          .map(s -> s.split("//")[1].trim().split(DROP_MARK)[1].trim())
          .collect(Collectors.toList());
    } catch (IOException e) {
      log.error("Can't read schedule file", e);
    }
    return new ArrayList<>();
  }

  @Override
  public synchronized void addNewEvent(int connectionId, PaxosEvent message) {
    if(!eventsToDrop.contains(message.toString())) {
      super.addNewEvent(connectionId, message);
      if(ExplorerConf.getInstance().schedulerFileHasMsgContent)
        eventsToSchedule.put(PaxosEvent.getEventId(message) + " " + message.getPayload(), message);
      else
        eventsToSchedule.put(PaxosEvent.getEventId(message), message);
      checkForSchedule();
    }
  }

  protected synchronized void checkForSchedule() {
    for(String s: eventsToSchedule.keySet()) {
      if(isOkToSchedule(s)) {
        schedule(eventsToSchedule.get(s));
        eventsToSchedule.remove(s);
        if(!isScheduleCompleted()) {
          nextMessage = scheduleToReplay.get(scheduled.size());
          System.out.println("Next " + nextMessage);
          checkForSchedule();
        } //else {
          //System.out.println("Completed ");
        //}
        return;
      }
    }
  }

  private boolean isOkToSchedule(String eventId) {
    return nextMessage.equals(eventId);
  }


  public boolean isScheduleCompleted() {
    return scheduled.size() == scheduleToReplay.size();
  }

  @Override
  public String getStats() {
    return "";
  }
}
