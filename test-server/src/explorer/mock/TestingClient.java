package explorer.mock;

import explorer.PaxosEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class TestingClient {

  private final String hostName;
  private final int portNumber;

  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;

  public TestingClient(String hostName, int portNumber) {
    this.hostName = hostName;
    this.portNumber = portNumber;
  }

  public void writeToSocket(int messageId) {
    out.println(PaxosEvent.toJsonStr(new PaxosEvent(1, 2, "PAXOS_PREPARE", "payloadStr", "1")));
  }

  public void connect() {
    try {
      socket = new Socket(hostName, portNumber);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      runInLoop();
    } catch (UnknownHostException e) {
      System.err.println("Don't know about host " + hostName);
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Couldn't get I/O for the connection to " + hostName);
      System.exit(1);
    }
  }

  public void disconnect() {

  }

  public void runInLoop() {
    String fromServer;

    try {
      while ((fromServer = in.readLine()) != null) {
        if (fromServer.equals("END"))
          break;
        //System.out.println("Received from server: " + fromServer);
      }
    } catch (Exception e) {
      System.err.println(e);
      System.exit(1);
    }
  }
}