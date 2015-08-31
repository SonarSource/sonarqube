package selenium;

import com.google.common.base.Joiner;
import org.openqa.selenium.By;

public abstract class Text {
  private Text() {
    // Static utility class
  }

  public static String doesOrNot(boolean not, String verb) {
    if (!verb.contains(" ")) {
      if (not) {
        return "doesn't " + verb;
      } else if (verb.endsWith("h")) {
        return verb + "es";
      } else {
        return verb + "s";
      }
    }

    String[] verbs = verb.split(" ");
    verbs[0] = doesOrNot(not, verbs[0]);

    return Joiner.on(" ").join(verbs);
  }

  public static String isOrNot(boolean not, String state) {
    return (not ? "is not " : "is ") + state;
  }

  public static String plural(int n, String word) {
    return (n + " " + word) + (n <= 1 ? "" : "s");
  }

  public static String toString(By selector) {
    return selector.toString().replace("By.selector: ", "").replace("By.cssSelector: ", "");
  }
}
