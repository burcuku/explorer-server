package explorer;

import explorer.coverage.CoverageStrategy;
import explorer.coverage.LastCliquesStrategy;
import explorer.net.Handler;
import explorer.net.TestingServer;
import explorer.net.socket.SocketServer;
import explorer.scheduler.FailureInjectingSettings;
import explorer.scheduler.Scheduler;
import explorer.scheduler.SchedulerSettings;
import explorer.workload.CassWorkloadDriver;
import explorer.workload.WorkloadDriver;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import utils.FileUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class ServerMain {

    // seed  OR // nodes to drop??? at each process, round, request
// takes json string as argument
    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.DEBUG);

        List<String> options = Arrays.asList(args);
        int seed = -1;
        if(options.contains("seed")) {
            try {
                seed = Integer.parseInt(options.get(options.indexOf("seed") + 1));
            } catch (Exception e) {
                throw new RuntimeException("Invalid command line arguments.\n" + e.getMessage());
            }
        }

        String failureSettingsJsonStr = "";

        if(options.contains("failures")) {
          try {
            failureSettingsJsonStr =options.get(options.indexOf("failures") + 1);
          } catch (Exception e) {
            throw new RuntimeException("Invalid command line arguments.\n" + e.getMessage());
          }
        }

        ExplorerConf conf = ExplorerConf.initialize("explorer.conf", args);
        if(seed > 0) conf.setSeed(seed);

        runAll(conf, failureSettingsJsonStr);
    }

    public static void runAll(ExplorerConf conf, String failureSettingsJsonStr) throws Exception {
        Class<? extends Scheduler> schedulerClass = null;
        Scheduler scheduler = null;
        SchedulerSettings settings = null;
        try {
            schedulerClass = (Class<? extends Scheduler>) Class.forName(conf.schedulerClass);
            //todo read settings class from config file
            if(failureSettingsJsonStr.equals(""))
              settings = new FailureInjectingSettings(conf.getSeed());
            else
              settings = FailureInjectingSettings.toObject(failureSettingsJsonStr);
            scheduler = schedulerClass.getConstructor(FailureInjectingSettings.class).newInstance(settings);
            //System.out.println(settings.toJsonStr());
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
            | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        CoverageStrategy coverageStrategy = new LastCliquesStrategy();
        scheduler.setCoverageStrategy(coverageStrategy);

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
        FileUtils.writeToFile("result.txt", "\nTest seed: " + conf.getSeed(), true);
        // send workload
        workloadDriver.sendWorkload();


      ExecutorService ex = Executors.newSingleThreadExecutor();
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            Thread.sleep(15000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          System.out.println("Shutting down");
          FileUtils.writeToFile("result.txt", "timed out - shutting down", true);
          System.exit(-1);
        }
      });
      t.start();

        while(!scheduler.isExecutionCompleted())
        {
            Thread.sleep(250);
        }
        scheduler.onExecutionCompleted();
        workloadDriver.stopEnsemble();
        Thread.sleep(1000);
        t.stop();
        testingServer.stop();
        serverThread.join();
    }

}