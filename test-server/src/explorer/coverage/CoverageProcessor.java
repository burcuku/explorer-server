package explorer.coverage;

public abstract class CoverageProcessor {

  public abstract void processCoveredTraces(String inFileCovered);
  public abstract void createMutationsFile(String outFileMutations);

}
