import explorer.ExplorerConf;
import testAPI.OnlineTestDriver;
import testAPI.TestDriver;
import explorer.scheduler.NodeFailureSettings;
import explorer.verifier.CassVerifier;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This file provides examples for writing fault-tolerance tests for Cassandra:
 * In the method "onlineFailuresForBuggyScenario", the user specifies where to introduce faults online during execution
 * In the method "offlineFailuresForBuggyScenario", the user can specify the faults to be introduced before the execution
 *    (or can provide a seed for randomly introducing faults in the execution)
 */
public class TestMain {

    String query1 = "UPDATE tests SET value_1 = 'A' WHERE name = 'testing' IF owner = 'user_1'";
    String query2 = "UPDATE tests SET value_1 = 'B', value_2 = 'B' WHERE name = 'testing' IF  value_1 = 'A'";
    String query3 = "UPDATE tests SET value_3 = 'C' WHERE name = 'testing' IF owner = 'user_1'";

    public void onlineFailuresForBuggyScenario() {
        OnlineTestDriver test = new OnlineTestDriver();
        test.startCluster();
        test.sleep(4000);

        // send workload
        test.submitQuery(0, query1);

        test.runForRounds(4); // around 2169 msec
        test.failNode(2);

        // send workload
        test.submitQuery(1, query2);

        test.runForRounds(2); // around 127 msec
        test.resumeNode(2);

        test.runForRounds(2); // around 201 msec
        test.failNode(2);

        test.runForRounds(2); // around 137 msec
        test.failNode(0);

        // send workload
        test.submitQuery(2, query3);

        test.runForRounds(2); // around 99 msec
        test.resumeNode(2);
        test.failNode(0);
        test.failNode(1);

        test.runForRounds(2); // around 217 msec
        test.resumeNode(1);

        test.runToCompletion();

        test.waitUntilCompletion();  // returns control here when the execution is completed // around 10140 msec

        new CassVerifier().verify();
        test.logStats();

        test.stopCluster();
        test.tearDown();
    }

    public void onlineFailuresWithRoundNumbers()  {
        OnlineTestDriver test = new OnlineTestDriver();
        test.startCluster();
        test.sleep(4000);

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

        new CassVerifier().verify();
        test.logStats();

        test.stopCluster();
        test.tearDown();
    }

    public void offlineFailuresForBuggyScenario() throws Exception {
        List<NodeFailureSettings.NodeFailure> failures  = new ArrayList<>();
        failures.add(new NodeFailureSettings.NodeFailure(0, 4, 2));
        failures.add(new NodeFailureSettings.NodeFailure(1, 2, 2));
        failures.add(new NodeFailureSettings.NodeFailure(1, 4, 0));
        failures.add(new NodeFailureSettings.NodeFailure(2, 0, 0));
        failures.add(new NodeFailureSettings.NodeFailure(2, 0, 1));
        failures.add(new NodeFailureSettings.NodeFailure(3, 0, 0));

        TestDriver test = new TestDriver(new NodeFailureSettings(failures)); // or seed
        test.startCluster();
        Thread.sleep(4000);

        // send workload
        test.submitQueries(Arrays.asList(0, 1, 2), Arrays.asList(query1, query2, query3));

        test.waitUntilCompletion();  // returns control here when the execution is completed
        new CassVerifier().verify();

        test.logStats();
        test.stopCluster();
        test.tearDown();
    }

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        ExplorerConf.initialize("explorer.conf", args);
        
        TestMain tm = new TestMain();
        //tm.onlineFailuresForBuggyScenario();
        tm.offlineFailuresForBuggyScenario();
        //tm.onlineFailuresWithRoundNumbers();
    }

}
