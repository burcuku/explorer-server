package explorer.verifier;

import java.util.Map;
import java.util.HashMap;
import com.datastax.driver.core.*;
import org.apache.log4j.BasicConfigurator;

public class CassVerifier  {
  //private static final Logger LOG = LoggerFactory.getLogger(CassVerifier.class);

  private Map<String, String> map = new HashMap<String, String>();

  private String value_1;
  private String value_2;

  public boolean verify() {
    BasicConfigurator.configure();

    boolean result = true;
    if (!checkDataConsistency()) {
      result = false;
    }
    return result;
  }

  private boolean checkDataConsistency() {
	  getValues();

    try {
      value_1 = map.get("value_1");
      value_2 = map.get("value_2");

      System.out.println("Value1: " + value_1 + "    Value2: " + value_2);
      if (value_1.equals("A") && (value_2.equals("B"))) {
        System.out.println("Reproduced the bug.");
        return false;
      }
    } catch (Exception e) {
      System.out.println("Failed to check data consistency.");
      System.out.println(e.getMessage());
      return true;
    }
    System.out.println("No bug.");
    return true;
  }

  private void getValues(){
		Cluster cluster = Cluster.builder()
        .addContactPoint("127.0.0.1")
        .build();
		Session session = cluster.connect("test");
		try {
			//System.out.println("Querying row from table");
			ResultSet rs = session.execute("SELECT * FROM tests");
			//System.out.println("Row acquired");
			Row row = rs.one();
      map.put("owner", row.getString("owner"));
      map.put("value_1", row.getString("value_1"));
      map.put("value_2", row.getString("value_2"));
      map.put("value_3", row.getString("value_3"));
		} catch (Exception e) {
			System.out.println("ERROR in reading row.");
			System.out.println(e.getMessage());
		}
		cluster.close();
  }

}
