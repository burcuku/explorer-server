package explorer;

import explorer.net.Handler;
import explorer.net.TestingServer;
import explorer.net.netty.NettyServer;
import explorer.net.socket.SocketServer;
import explorer.scheduler.Scheduler;
import explorer.workload.CassWorkloadDriver;
import explorer.workload.WorkloadDriver;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class ServerMain {

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.DEBUG);

        ExplorerConf conf = ExplorerConf.initialize("explorer.conf", args);
        Class<? extends Scheduler> schedulerClass = (Class<? extends Scheduler>) Class.forName(conf.schedulerClass);
        Scheduler scheduler = schedulerClass.getConstructor().newInstance();
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

        workloadDriver.cleanup();
        workloadDriver.prepare(1);
        workloadDriver.startEnsemble();

        Thread.sleep(4000);

        workloadDriver.sendWorkload();

        serverThread.join();
    }
}