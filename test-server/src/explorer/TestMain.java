package explorer;

import testAPI.OnlineTestDriver;
import testAPI.TestDriver;
import explorer.scheduler.FailureInjectingSettings;
import explorer.verifier.CassVerifier;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TestMain {

    String query1 = "UPDATE tests SET value_1 = 'A' WHERE name = 'testing' IF owner = 'user_1'";
    String query2 = "UPDATE tests SET value_1 = 'B', value_2 = 'B' WHERE name = 'testing' IF  value_1 = 'A'";
    String query3 = "UPDATE tests SET value_3 = 'C' WHERE name = 'testing' IF owner = 'user_1'";

    public void onlineFailuresForBuggyScenario() throws Exception {
        OnlineTestDriver test = new OnlineTestDriver();
        test.startCluster();
        Thread.sleep(4000);

        // send workload
        test.submitQuery(0, query1);

        test.runUntilRound(4);
        test.failNode(2);

        // send workload
        test.submitQuery(1, query2);

        test.runUntilRound(6);
        test.resumeNode(2);

        test.runUntilRound(8);
        test.failNode(2);

        test.runUntilRound(10);
        test.failNode(0);

        // send workload
        test.submitQuery(2, query3);

        test.runUntilRound(12);
        test.resumeNode(2);
        test.failNode(0);
        test.failNode(1);

        test.runUntilRound(14);
        test.resumeNode(1);

        test.runToCompletion();

        test.waitUntilCompletion();  // returns control here when the execution is completed

        // write to results file
        //FileUtils.writeToFile("result.txt", test.getSchedule());
        new CassVerifier().verify();

        test.stopCluster();
        test.tearDown();
    }

    public void offlineFailuresForBuggyScenario() throws Exception {
        List<FailureInjectingSettings.NodeFailure> failures  = new ArrayList<>();
        failures.add(new FailureInjectingSettings.NodeFailure(0, 4, 2));
        failures.add(new FailureInjectingSettings.NodeFailure(1, 2, 2));
        failures.add(new FailureInjectingSettings.NodeFailure(1, 4, 0));
        failures.add(new FailureInjectingSettings.NodeFailure(2, 0, 0));
        failures.add(new FailureInjectingSettings.NodeFailure(2, 0, 1));
        failures.add(new FailureInjectingSettings.NodeFailure(3, 0, 0));

        TestDriver test = new TestDriver(new FailureInjectingSettings(failures)); // or seed
        test.startCluster();
        Thread.sleep(4000);

        // send workload
        test.submitQueries(Arrays.asList(0, 1, 2), Arrays.asList(query1, query2, query3));

        test.waitUntilCompletion();  // returns control here when the execution is completed

        // write to results file
        //FileUtils.writeToFile("result.txt", test.getSchedule());
        new CassVerifier().verify();

        test.stopCluster();
        test.tearDown();
    }

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        ExplorerConf.initialize("explorer.conf", null);

        TestMain tm = new TestMain();
        //tm.onlineFailuresForBuggyScenario();
        tm.offlineFailuresForBuggyScenario();
    }

}
