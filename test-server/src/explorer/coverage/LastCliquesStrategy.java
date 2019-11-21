package explorer.coverage;

import com.google.gson.Gson;
import explorer.scheduler.FailureInjectingScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.FileUtils;

import java.util.*;

public class LastCliquesStrategy extends CoverageStrategy {
  private static final Logger log = LoggerFactory.getLogger(FailureInjectingScheduler.class);

  private LastCliques current = new LastCliques();

  public void onRoundComplete(String tag, Set<Integer> clique) {

  }

  public void onRequestPhaseComplete(String tag, Set<Integer> clique) {
    Clique c = new Clique(tag, clique);
    System.out.println("Adding: " + c.toString());
    current.add(c);
  }

  public void onScheduleComplete(String scheduleTag) {
    // write coverage (to be processed by the analyzer program) and the introduced failures (for debugging/analysis)
    FileUtils.writeToFile("covered", current.toJsonStr() + "\n" + scheduleTag + "\n\n", true); // used for coverage exploration
    current.clear();
  }



  public class LastCliques {
    // keeps the set of failed nodes
    private List<Clique> cliques = new ArrayList<>();

    public void add(Clique c) {
      cliques.add(c);
    }

    public void clear() {
      cliques.clear();
    }

    public String getCoverageAsStr(List<Clique> cliques) {
      String s = "";
      for(Clique c: cliques)
        s = s.concat(c.toString() + " ");
      return s;
    }

    public String toJsonStr() {
      Gson gson = new Gson();
      return gson.toJson(this);
    }

    public LastCliques toObject(String json) {
      Gson gson = new Gson();
      //System.out.println(json);
      return gson.fromJson(json, LastCliques.class);
    }
  }
}
