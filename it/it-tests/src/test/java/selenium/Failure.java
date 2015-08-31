package selenium;

import java.util.ArrayList;
import java.util.List;

class Failure {
  private static final String PREFIX = Failure.class.getPackage().getName() + ".";

  private Failure() {
    // Static class
  }

  public static AssertionError create(String message) {
    AssertionError error = new AssertionError(message);
    removeSimpleleniumFromStackTrace(error);
    return error;
  }

  private static void removeSimpleleniumFromStackTrace(Throwable throwable) {
    List<StackTraceElement> filtered = new ArrayList<>();

    for (StackTraceElement element : throwable.getStackTrace()) {
      if (!element.getClassName().contains(PREFIX)) {
        filtered.add(element);
      }
    }

    throwable.setStackTrace(filtered.toArray(new StackTraceElement[filtered.size()]));
  }
}
