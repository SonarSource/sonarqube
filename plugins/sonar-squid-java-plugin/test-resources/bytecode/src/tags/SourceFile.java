package tags;

import java.util.Collection;
import java.util.ArrayList;

public class SourceFile extends File {

  public static String       path;
  private int                localInt;
  private long               localLong;
  private String[]           paths;
  private Collection<String> stringCollection = new ArrayList<String>();

  public void readSourceFile() {
    try {
      read();
    } catch (TagException e) {
      System.out.println("There is an issue");
    }
  }

}
