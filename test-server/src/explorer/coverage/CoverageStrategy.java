package explorer.coverage;

import java.util.Set;

public abstract class CoverageStrategy {
  abstract public void onRoundComplete(String tag, Set<Integer> clique);
  abstract public void onRequestPhaseComplete(String tag, Set<Integer> clique);
  abstract public void onScheduleComplete(String scheduleTag);
}
