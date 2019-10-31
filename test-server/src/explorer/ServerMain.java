package explorer;

import explorer.net.Handler;
import explorer.net.TestingServer;
import explorer.net.socket.SocketServer;
import explorer.scheduler.Scheduler;
import explorer.workload.CassWorkloadDriver;
import explorer.workload.WorkloadDriver;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class ServerMain {

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);

        List<String> options = Arrays.asList(args);
        int seed = -1;
        if(options.contains("seed")) {
            try {
                seed = Integer.parseInt(options.get(options.indexOf("seed") + 1));
            } catch (Exception e) {
                throw new RuntimeException("Invalid command line arguments.\n" + e.getMessage());
            }
        }

        ExplorerConf conf = ExplorerConf.initialize("explorer.conf", args);
        if(seed > 0) conf.setSeed(seed);

        Class<? extends Scheduler> schedulerClass = (Class<? extends Scheduler>) Class.forName(conf.schedulerClass);
        Scheduler scheduler = schedulerClass.getConstructor(ExplorerConf.class).newInstance(conf);
        Handler handler = new ConnectionHandler(scheduler);

        // start server which enforces a schedule over distributed system nodes
        TestingServer testingServer = new SocketServer(conf.portNumber, conf.numberOfClients, handler);

        Thread serverThread = new Thread(testingServer, "testing-server");
        serverThread.start();

        // start distributed system nodes and the workload
        WorkloadDriver workloadDriver = new CassWorkloadDriver(conf.getWorkloadDirs(), conf.numberOfClients, conf.javaPath);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            workloadDriver.stopEnsemble();
            testingServer.stop();
        }));

        // send workload
        workloadDriver.cleanup();
        workloadDriver.prepare(1);
        workloadDriver.startEnsemble();
        Thread.sleep(4000);

        // write to results file
        writeToFile(seed);
        // send workload
        workloadDriver.sendWorkload();
        while(!scheduler.isExecutionCompleted())
        {
            Thread.sleep(250);
        }
        scheduler.onExecutionCompleted();
        workloadDriver.stopEnsemble();
        Thread.sleep(1000);
        testingServer.stop();
        serverThread.join();
    }

    public static void writeToFile(int seed) {
        FileWriter fw;
        PrintWriter pw;

        try {
            fw = new FileWriter("result.txt", true);
            pw = new PrintWriter(fw);
            pw.println("\nTest seed: " + seed);
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}