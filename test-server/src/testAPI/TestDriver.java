package testAPI;

import explorer.ConnectionHandler;
import explorer.ExplorerConf;
import explorer.net.Handler;
import explorer.net.TestingServer;
import explorer.net.socket.SocketServer;
import explorer.scheduler.NodeFailureInjector;
import explorer.scheduler.NodeFailureSettings;
import explorer.scheduler.Scheduler;
import explorer.workload.CassWorkloadDriver;
import explorer.workload.WorkloadDriver;
import utils.FileUtils;

import java.util.List;

public class TestDriver {

    WorkloadDriver workloadDriver;
    Scheduler scheduler;
    TestingServer testingServer;
    Thread serverThread;

    public TestDriver(int randomSeed) {
        this(ExplorerConf.getInstance(), new NodeFailureInjector(new NodeFailureSettings(randomSeed)));
    }

    public TestDriver(NodeFailureSettings settings) {
        this(ExplorerConf.initialize("explorer.conf", null), new NodeFailureInjector(settings));

        if(settings.equals(NodeFailureSettings.ONLINE_CONTROLLED)) {
            throw new IllegalArgumentException("Please use OnlineTestDriver for online control of failure injection.");
        }
    }

    protected TestDriver(ExplorerConf conf, Scheduler scheduler) {
        this.scheduler = scheduler;

        Handler handler = new ConnectionHandler(scheduler);
        // start server which enforces a schedule over distributed system nodes
        testingServer = new SocketServer(conf.portNumber, conf.numberOfClients, handler);
        serverThread = new Thread(testingServer, "testing-server");

        // start the server thread
        serverThread.start();

        // start distributed system nodes and the workload
        workloadDriver = new CassWorkloadDriver(conf.getWorkloadDirs(), conf.numberOfClients, conf.javaPath);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            workloadDriver.stopEnsemble();
            testingServer.stop();
        }));

        workloadDriver.cleanup();  // delete existing runtime directories for the Cassandra nodes
        workloadDriver.prepare(1); // prepare runtime directories

        setMaxTestDuration(conf.maxExecutionDuration); // can be overwritten by user
    }

    public void startCluster() {
        workloadDriver.startEnsemble();
    }

    public void stopCluster() {
        workloadDriver.stopEnsemble();
    }

    public void submitQuery(int nodeId, String query) {
        Thread t = new Thread(() -> workloadDriver.submitQuery(nodeId, query));
        t.start();
    }

    public void submitQueries(List<Integer> nodeIds, List<String > queries) {
        Thread t = new Thread(() -> workloadDriver.submitQueries(nodeIds, queries));
        t.start();
    }

    public void setOnExecutionCompleted(Runnable r) {
        scheduler.setOnExecutionCompleted(r);
    }

    public void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void waitUntilCompletion() {
        while(!scheduler.isExecutionCompleted())
        {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setMaxTestDuration(int msec) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(msec);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Timed out - Shutting down");
                FileUtils.writeToFile(ExplorerConf.getInstance().resultFile, "Timed out - shutting down", true);
                System.exit(-1);
            }
        });
        t.start();
    }

    public String getSchedule() {
        return scheduler.getScheduleAsStr();
    }

    public void tearDown() {
        try {
            Thread.sleep(1000);
            testingServer.stop();
            serverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
