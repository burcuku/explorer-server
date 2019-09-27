package explorer.mock;

import java.util.Scanner;

public class MockNode {

    int nodeId;

    public MockNode(int nodeId) {
        this.nodeId = nodeId;

        TestingClient tc = new TestingClient("127.0.0.1", 4444);

        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                tc.connect();
            }
        });
        t.start();

        Scanner s = new Scanner(System.in);
        String str = "";

        while(!str.equals("END")) {
            str = s.nextLine();
            tc.writeToSocket(nodeId);
        }

        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(int msg) {
        System.out.println("NODE " + nodeId + " has sent the message: " + msg);
    }

    public void receiveMessage(int msg) {
        System.out.println("NODE " + nodeId + " has received the message: " + msg);
    }

}
