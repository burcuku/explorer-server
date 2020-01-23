package explorer.scheduler;
import explorer.ExplorerConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LinkFailureSettings extends SchedulerSettings {
    private static final Logger log = LoggerFactory.getLogger(LinkFailureSettings.class);
    ExplorerConf conf = ExplorerConf.getInstance();

    // derived from the parameters
    public final int NUM_MAJORITY= (conf.NUM_PROCESSES / 2) + 1;

    private Random random;
    public final int depth = conf.bugDepth;

    public int seed = conf.randomSeed;
    private List<LinkFailureSettings.LinkFailure> failures;

    // to be used for deserialization (failures will be set)
    public LinkFailureSettings() {
        this(ExplorerConf.getInstance().randomSeed);
    }

    // to be used for creation
    public LinkFailureSettings(int seed) {
        this.seed = seed;
        random = new Random(seed);
        failures = getRandomFailures();
        //failures = getFailuresToReproduceBug();
        String failuresAsStr = toString();
        log.info("Failure Injecting Settings: \n" + failuresAsStr);
    }

    public LinkFailureSettings(List<LinkFailure> failures) {
        this.failures = failures;
    }

    @Override
    public SchedulerSettings mutate() {
        throw new RuntimeException("Not implemented for LinkFailureInjector");
    }

    private List<LinkFailureSettings.LinkFailure> getRandomFailures() {
        List<LinkFailureSettings.LinkFailure> f  = new ArrayList<>();

        int[] failurePerPhase = new int[conf.NUM_PHASES];
        List<Integer> phases = new ArrayList<>();

        for(int i = 0; i < conf.NUM_PHASES; i++) {
            phases.add(i);
        }

        for(int i = 0; i < depth; i++) {
            int phaseToFailAt = random.nextInt(phases.size());
            failurePerPhase[phaseToFailAt] ++;
            if(failurePerPhase[phaseToFailAt] == conf.NUM_PROCESSES)
                phases.remove(phaseToFailAt);

            int roundToFailAt = random.nextInt(conf.NUM_ROUNDS_IN_PROTOCOL);
            int fromProcess = random.nextInt(conf.NUM_PROCESSES);
            int toProcess = random.nextInt(conf.NUM_PROCESSES);

            f.add(new LinkFailure(phaseToFailAt, roundToFailAt, fromProcess, toProcess));
        }

        return f;
    }


    public List<LinkFailureSettings.LinkFailure> getFailures() {
        return failures;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Num processes: ").append(conf.NUM_PROCESSES).append("\n");
        sb.append("Num rounds in the protocol: ").append(conf.NUM_ROUNDS_IN_PROTOCOL).append("\n");
        sb.append("Num requests/phases: ").append(conf.NUM_PHASES).append("\n");
        sb.append("Random seed: ").append(conf.randomSeed).append("\n");
        sb.append("Bug depth: ").append(conf.bugDepth).append("\n");
        return sb.toString();
    }

    private List<LinkFailureSettings.LinkFailure> getFailuresToReproduceBug() {
        // depth is 7
        List<LinkFailureSettings.LinkFailure> f  = new ArrayList<>();
        f.add(new LinkFailure(0, 4, 0, 2));
        f.add(new LinkFailure(1, 2, 1, 2));
        f.add(new LinkFailure(1, 4, 1, 2));
        f.add(new LinkFailure(1, 4, 1, 0));
        f.add(new LinkFailure(2, 0, 2, 0));
        f.add(new LinkFailure(2, 0, 2, 1));
        f.add(new LinkFailure(3, 0, 2, 0));
        return f;
    }

    public static class LinkFailure {
        int k; // in which request does it happen?
        int r; // at which round does it happen?
        int fromProcess; //
        int toProcess;

        public LinkFailure(int k, int r, int from, int to) {
            this.k = k;
            this.r = r;
            this.fromProcess = from;
            this.toProcess = to;
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof LinkFailure)) return false;

            return k == ((LinkFailure)obj).k
                    && r == ((LinkFailure)obj).r
                    && fromProcess == ((LinkFailure)obj).fromProcess
                    && toProcess == ((LinkFailure)obj).toProcess;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + k;
            result = 31 * result + r;
            result = 31 * result + fromProcess;
            result = 31 * result + toProcess;
            return result;
        }
    }

}
