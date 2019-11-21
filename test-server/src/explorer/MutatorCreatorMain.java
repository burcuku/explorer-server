package explorer;

import explorer.coverage.CoverageProcessor;
import explorer.coverage.LastCliqueProcessor;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


public class MutatorCreatorMain {

  public static void main(String[] args) {
    BasicConfigurator.configure();
    Logger.getRootLogger().setLevel(Level.INFO);
    ExplorerConf.initialize("explorer.conf", args);

    CoverageProcessor cp = new LastCliqueProcessor(3, 5);
    cp.processCoveredTraces("covered");
    cp.createMutationsFile("mutations");
  }

}