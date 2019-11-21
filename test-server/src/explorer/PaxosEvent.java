package explorer;

import com.google.gson.Gson;

public class PaxosEvent {

  private long sender;
  private long recv;
  private String verb;
  private String payload;
  private String usrval;

  public static int NUM_PAXOS_ROUNDS = 6;
  public static final String ACK_PAYLOAD = "ACK";

  // for customized round identification for Cassandra's Paxos
  public enum ProtocolRound {PAXOS_PREPARE, PAXOS_PREPARE_RESPONSE, PAXOS_PROPOSE, PAXOS_PROPOSE_RESPONSE, PAXOS_COMMIT, PAXOS_COMMIT_RESPONSE};

  public PaxosEvent(long sender, long recv, String verb, String payload, String usrval) {
    this.sender = sender;
    this.recv = recv;
    this.verb = verb;
    this.payload = payload;
    this.usrval = usrval;
  }

  public long getSender() {
    return sender;
  }

  public long getRecv() {
    return recv;
  }

  public String getVerb() {
    return verb;
  }

  public String getPayload() {
    return payload;
  }

  public String getUsrval() {
    return usrval;
  }

  public String getBallot() {
    if(payload.contains("ballot=")) {
      return payload.substring(payload.indexOf("ballot=")+7, payload.indexOf("ballot=")+31);
    }
    return "";
  }

  public int getProtocolStep() {
      if(verb.equals("PAXOS_PREPARE")) {
        return 0;
      } else if(verb.equals("PAXOS_PREPARE_RESPONSE")) {
        return 1;
      } else if(verb.equals("PAXOS_PROPOSE")) {
        return 2;
      } else if(verb.equals("PAXOS_PROPOSE_RESPONSE")) {
        return 3;
      } else if(verb.equals("PAXOS_COMMIT")) {
        return 4;
      } else if(verb.equals("PAXOS_COMMIT_RESPONSE")) {
        return 5;
      }
      return -1;
  }

  public int getClientRequest() {
    if(verb.equals("PAXOS_PREPARE") || verb.equals("PAXOS_PROPOSE") || verb.equals("PAXOS_COMMIT")) {
      return (int)sender;
    } if(verb.equals("PAXOS_PREPARE_RESPONSE") || verb.equals("PAXOS_PROPOSE_RESPONSE") || verb.equals("PAXOS_COMMIT_RESPONSE")) {
      return (int)recv;
    }
    return -1;
  }

  public boolean isRequest() {
    return verb.equals("PAXOS_PREPARE") || verb.equals("PAXOS_PROPOSE") || verb.equals("PAXOS_COMMIT");
  }

  public boolean isResponse() {
    return verb.equals("PAXOS_PREPARE_RESPONSE") || verb.equals("PAXOS_PROPOSE_RESPONSE") || verb.equals("PAXOS_COMMIT_RESPONSE");
  }

  public boolean isResponseOf(PaxosEvent m) {
    if(verb.equals("PAXOS_PREPARE_RESPONSE") && m.verb.equals("PAXOS_PREPARE") && this.sender == m.recv && this.recv == m.sender) {
      return true;
    } else if(verb.equals("PAXOS_PROPOSE_RESPONSE") && m.verb.equals("PAXOS_PROPOSE") && this.sender == m.recv && this.recv == m.sender) {
      return true;
    } else if(verb.equals("PAXOS_COMMIT_RESPONSE") && m.verb.equals("PAXOS_COMMIT") && this.sender == m.recv && this.recv == m.sender) {
      return true;
    }
    return false;
  }

  public static String toJsonStr(PaxosEvent obj) {
    Gson gson = new Gson();
    //System.out.println(gson.toJson(obj));
    return gson.toJson(obj);
  }

  public static PaxosEvent toObject(String json) {
    Gson gson = new Gson();
    //System.out.println(json);
    return gson.fromJson(json, PaxosEvent.class);
  }

  public boolean isAckEvent() {
    return payload.equals(ACK_PAYLOAD);
  }

  @Override
  public String toString() {
      return "Req: " + getClientRequest() +
          " From: " + sender + " To: " + recv + " - " + verb;
  }

  //todo put into PaxosEvent
  public static String getEventId(PaxosEvent message) {
    StringBuilder sb = new StringBuilder("Req-");
    sb.append(message.getClientRequest());
    sb.append("--");
    sb.append(message.getVerb());
    sb.append("--From-");
    sb.append(message.getSender());
    sb.append("--To-");
    sb.append(message.getRecv());
    return sb.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == this) return true;

    if(!(obj instanceof PaxosEvent)) return false;

    PaxosEvent e = (PaxosEvent) obj;
    return sender == e.sender  &&
        recv == e.recv &&
        verb.equals(e.verb) &&
        payload.equals(e.payload) &&
        usrval.equals(e.usrval);
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + (int)sender;
    result = 31 * result + verb.hashCode();
    result = 31 * result + (int)recv;
    result = 31 * result + payload.hashCode();
    result = 31 * result + usrval.hashCode();
    return result;
  }
}
