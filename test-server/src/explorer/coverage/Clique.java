package explorer.coverage;

// instead of set, keep a string representing a clique for the nodes
// e.g. Node2 Node1 Node0 -> 111, Node2 Node0 -> 101, ...

import java.util.Iterator;
import java.util.Set;

public class Clique {
  private String round;
  private int clique = 0;

  public Clique(String round, Set<Integer> c) {
    this.round = round;
    for(int i: c) clique = clique + (int) Math.pow(10, i);
  }

  //todo assert s has only 0s and 1s
  public Clique(String round, String s) {
    this.round = round;
    this.clique = Integer.parseInt(s);
  }

  public int getClique() {
    return clique;
  }

  public String toString() {
    /*String s = "[ ";
    for(int i: clique) s = s.concat(i + " ");
    s = s.concat("]");
    return s;*/
    return String.valueOf(clique);
  }

  public boolean hasSameNodes(Object obj) {
    if(!(obj instanceof Clique)) return false;
    Clique c = (Clique) obj;

    return this.clique == c.getClique();
  }

  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof Clique)) return false;
    Clique c = (Clique) obj;

    return this.clique == c.getClique() && this.round.equals(c.round);
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + clique;
    result = 31 * result + round.hashCode();
    return result;
  }
}