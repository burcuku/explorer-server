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
    //log.debug("Adding clique: " + c.toString());
    current.add(c);
  }

  public void onScheduleComplete(String scheduleTag) {
    // write coverage (to be processed by the analyzer program) and the introduced failures (for debugging/analysis)
    FileUtils.writeToFile("covered", LastCliques.toJsonStr(current) + "\n" + scheduleTag + "\n\n", true); // used for coverage exploration
    current.clear();
  }

  public static class LastCliques {
    // keeps the set of failed nodes
    private List<Clique> cliques = new ArrayList<>();

    public void add(Clique c) {
      cliques.add(c);
    }

    public void clear() {
      cliques.clear();
    }

    public String getCoverageAsStr() {
      String s = "";
      for(Clique c: cliques)
        s = s.concat(c.toString() + " ");
      return s;
    }

    public static String getCoverageAsStr(List<Clique> cliques) {
      String s = "";
      for(Clique c: cliques)
        s = s.concat(c.toString() + " ");
      return s;
    }

    public static String toJsonStr(Object obj) {
      Gson gson = new Gson();
      return gson.toJson(obj);
    }

    public static LastCliques toObject(String json) {
      Gson gson = new Gson();
      //System.out.println(json);
      return gson.fromJson(json, LastCliques.class);
    }

    // compares only the list of failures for easy analysis of coverage!
    @Override
    public boolean equals(Object obj) {
      if(!(obj instanceof LastCliques))
        return false;

      List<Clique> cliquesThis = this.cliques;
      List<Clique> cliquesThat = ((LastCliques)obj).cliques;

      if(cliquesThis.size() != cliquesThat.size())
        return false;

      for(int i = 0; i < cliquesThis.size(); i++) {
        if(!cliquesThis.get(i).equals(cliquesThat.get(i)))
          return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int hashCode = 1;
      Iterator<Clique> i = cliques.iterator();
      while (i.hasNext()) {
        Clique obj = i.next();
        hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
      }
      return hashCode;
    }
  }
}
