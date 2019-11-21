package explorer.coverage;

import explorer.scheduler.FailureInjectingSettings;
import utils.FileUtils;

import java.util.*;

public class LastCliqueProcessor extends CoverageProcessor {

  public final int NUM_MUTATIONS; // number of mutations
  public final int MIN_FREQ; // if a coverage trace is samples less than this, add mutations

  private Map<LastCliquesStrategy.LastCliques, Integer> cliques = new HashMap<>();
  private Map<LastCliquesStrategy.LastCliques, FailureInjectingSettings> failures = new HashMap<>();

  public LastCliqueProcessor(int minFreq, int numMutations) {
    MIN_FREQ = minFreq;
    NUM_MUTATIONS = numMutations;
  }

  public void processCoveredTraces(String inFileCovered) {
    Iterator<String> lines = FileUtils.readLinesFromFile(inFileCovered).iterator();

    //todo make more robust against different file configurations
    while(lines.hasNext()){
      String cliquesLine = lines.next();
      if(cliquesLine.isEmpty()) continue;

      LastCliquesStrategy.LastCliques clqs = LastCliquesStrategy.LastCliques.toObject(cliquesLine);
      if(cliques.containsKey(clqs))
        cliques.put(clqs, cliques.get(clqs) + 1);
      else
        cliques.put(clqs,1);

      String failuresLine = lines.next();
      FailureInjectingSettings fs = (FailureInjectingSettings) FailureInjectingSettings.toObject(failuresLine);
      failures.put(clqs, fs);
    }
  }

  public void createMutationsFile(String outFileMutations) {
    for(LastCliquesStrategy.LastCliques clique: cliques.keySet()) {

      if(cliques.get(clique) < MIN_FREQ) {
        List<FailureInjectingSettings> mutations = mutate(failures.get(clique), NUM_MUTATIONS);
        for(FailureInjectingSettings mutation: mutations)
          FileUtils.writeToFile(outFileMutations, FailureInjectingSettings.toJsonStr(mutation), true);
      }

    }
  }

  private List<FailureInjectingSettings> mutate(FailureInjectingSettings failure, int numMutations) {
    List<FailureInjectingSettings> mutations = new ArrayList<>();

    for(int i = 0; i < numMutations; i++) {
      FailureInjectingSettings mutation = (FailureInjectingSettings) failure.mutate();
      mutations.add(mutation);
    }

    return mutations;
  }

  public Map<LastCliquesStrategy.LastCliques, Integer> getCliques() {
    return new HashMap<>(cliques);
  }


}
