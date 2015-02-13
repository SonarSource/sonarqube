package org.sonar.api.utils.log;

public interface Logger {

  boolean isDebugEnabled();

  void debug(String message);

  void info(String message);

  void warn(String message);

  void error(String message);

  void error(String message, Throwable throwable);
}
