package explorer.net;

public interface Handler {
    void onConnect(int id, MessageSender sender);
    void onDisconnect(int id);
    void onReceive(int id, String message);
}
