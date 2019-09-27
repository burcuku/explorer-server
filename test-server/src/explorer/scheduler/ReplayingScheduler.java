package explorer.scheduler;

import explorer.ExplorerConf;
import explorer.PaxosEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReplayingScheduler extends Scheduler {

  private static final Logger log = LoggerFactory.getLogger(ReplayingScheduler.class);

  private final List<String> scheduleToReplay;
  private Map<String, PaxosEvent> eventsToSchedule;
  private String nextMessage;

  public ReplayingScheduler() {
    eventsToSchedule = new HashMap<String, PaxosEvent>();
    scheduleToReplay = readSchedule();
    nextMessage = scheduleToReplay.get(scheduled.size());
  }

  private List<String> readSchedule() {
    try {
      return Files.readAllLines(Paths.get(ExplorerConf.getInstance().schedulerFile))
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

  @Override
  public synchronized void addNewEvent(int connectionId, PaxosEvent message) {
    super.addNewEvent(connectionId, message);
    eventsToSchedule.put(getEventId(message), message);
    checkForSchedule();
  }

  protected synchronized void checkForSchedule() {
    for(String s: eventsToSchedule.keySet()) {
      if(isOkToSchedule(s)) {
        schedule(eventsToSchedule.get(s));
        eventsToSchedule.remove(s);
        if(!isScheduleCompleted()) {
          nextMessage = scheduleToReplay.get(scheduled.size());
          //System.out.println("Next " + nextMessage);
          checkForSchedule();
        } else {
          System.out.println("Completed ");
        }
        return;
      }
    }
  }

  private boolean isOkToSchedule(String eventId) {
    return nextMessage.equals(eventId);
  }

  public static String getEventId(PaxosEvent message) {
      StringBuilder sb = new StringBuilder("Req-");
      sb.append(message.getClientRequest());
      sb.append("--");
      sb.append(message.getVerb());
      sb.append("--From-");
      sb.append(message.getSender());
      sb.append("--To-");
      sb.append(message.getRecv());
      return sb.toString();
  }

  public boolean isScheduleCompleted() {
    System.out.println("\tExecuted: " + scheduled.size() + " Waiting: ");
    for(String e: eventsToSchedule.keySet())
      System.out.println(" \t" + e);
    return scheduled.size() == scheduleToReplay.size();
  }
}
