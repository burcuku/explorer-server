package explorer.net.socket;

import explorer.net.Handler;
import explorer.net.TestingServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocketServer implements TestingServer {

  private static Logger log = LoggerFactory.getLogger(SocketServer.class);

  private ServerSocket serverSocket;
  private int numClients;
  private Handler handler;

  private Map<Integer, SocketConnection> connections = new HashMap<>();
  private List<Thread> threads = new ArrayList<>();

  public SocketServer(int portNumber, int clients, Handler handler) {
    this.handler = handler;
    this.numClients = clients;

    try {
      this.serverSocket = new ServerSocket(portNumber);
    } catch (IOException e) {
      log.error("Could not listen on port: " + portNumber, e);
    }
  }

  @Override
  public void run() {

    while (connections.size() < numClients) {
      try {
        log.info("Waiting for connections from Cassandra nodes...");

        Socket clientSocket = serverSocket.accept();
        int id = connections.size() + 1;

        SocketConnection c = new SocketConnection(clientSocket, id, handler);
        connections.put(id, c);

        Thread thread = new Thread(c, "client-runner-" + id);
        threads.add(thread);
        thread.start();

      } catch (IOException e) {
        log.error("Accept failed.", e);
        System.exit(1);
      }
    }

    for (Thread t : threads) {
      try {
        t.join();
      } catch (InterruptedException e) {
        log.warn("Requested thread exit");
      }
    }
  }

  @Override
  public void stop() {
    for (Thread t : threads) {
      t.interrupt();
    }
  }
}
