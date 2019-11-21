package explorer.coverage;

import java.util.Set;

public class NopStrategy extends CoverageStrategy {

  @Override
  public void onRoundComplete(String tag, Set<Integer> clique) {

  }

  @Override
  public void onRequestPhaseComplete(String tag, Set<Integer> clique) {

  }

  @Override
  public void onScheduleComplete(String scheduleTag) {

  }
}
