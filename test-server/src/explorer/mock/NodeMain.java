package explorer.mock;

public class NodeMain {
    public static void main(String[] args) {
        if (args.length == 1) {
            new MockNode(Integer.parseInt(args[0]));
        }
        System.out.println("No nodeId given.");
    }
}
