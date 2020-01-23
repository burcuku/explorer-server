package testAPI;

import explorer.ExplorerConf;
import explorer.scheduler.NodeFailureInjector;
import explorer.scheduler.NodeFailureSettings;

public class OnlineTestDriver extends TestDriver {

    public OnlineTestDriver() {
        super(ExplorerConf.getInstance(), new NodeFailureInjector(NodeFailureSettings.ONLINE_CONTROLLED));
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
