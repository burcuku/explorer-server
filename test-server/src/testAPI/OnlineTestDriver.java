package testAPI;

import explorer.ExplorerConf;
import explorer.scheduler.FailureInjectingScheduler;
import explorer.scheduler.FailureInjectingSettings;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class OnlineTestDriver extends TestDriver {

    public OnlineTestDriver() {
        super(ExplorerConf.getInstance(), new FailureInjectingScheduler(FailureInjectingSettings.ONLINE_CONTROLLED));
    }

    public void runUntilRound(int i) {
        scheduler.runUntilRound(i);
    }

    public void runForRounds(int numRounds) {
        scheduler.runForRounds(numRounds);
    }

    public void failNode(int nodeId) {
        scheduler.failNode(nodeId);
    }

    public void resumeNode(int nodeId) {
        scheduler.resumeNode(nodeId);
    }

    public void runToCompletion() {
        scheduler.runToCompletion();
    }
}
