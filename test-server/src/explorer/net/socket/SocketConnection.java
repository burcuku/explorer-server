package explorer.net.socket;

import explorer.net.Handler;
import explorer.net.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class SocketConnection implements Runnable {

    private static Logger log = LoggerFactory.getLogger(SocketConnection.class);

    private Socket socket;
    private int connectionId;
    private Handler handler;

    SocketConnection(Socket socket, int connectionId, Handler handler) {
        this.socket = socket;
        this.connectionId = connectionId;
        this.handler = handler;
    }

    @Override
    public void run() {

        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine;
            handler.onConnect(connectionId, new SocketSender(out));

            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equals("END")) {
                    break;
                }

                //get event from node
                handler.onReceive(connectionId, inputLine);
            }
            out.close();
            in.close();

            handler.onDisconnect(connectionId);

            socket.close();
        } catch (IOException e) {
            log.error("Error reading from client", e);
        }
    }

    private static class SocketSender implements MessageSender {

        PrintWriter printWriter;

        SocketSender(PrintWriter printWriter) {
            this.printWriter = printWriter;
        }

        @Override
        public void send(String message) {
            printWriter.println(message);
            printWriter.flush();
        }
    }
}
