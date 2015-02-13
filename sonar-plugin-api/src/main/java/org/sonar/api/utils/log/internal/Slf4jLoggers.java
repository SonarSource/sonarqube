package org.sonar.api.utils.log.internal;


import org.slf4j.LoggerFactory;
import org.sonar.api.utils.log.Logger;

public class Slf4jLoggers {

  public static class Slf4jLogger implements Logger {
    private final org.slf4j.Logger slf4j;

    public Slf4jLogger(org.slf4j.Logger slf4j) {
      this.slf4j = slf4j;
    }

    @Override
    public boolean isDebugEnabled() {
      return slf4j.isDebugEnabled();
    }

    @Override
    public void debug(String message) {
      slf4j.debug(message);
    }

    @Override
    public void info(String message) {
      slf4j.info(message);
    }

    @Override
    public void warn(String message) {
      slf4j.warn(message);
    }

    @Override
    public void error(String message) {
      slf4j.error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
      slf4j.error(message, throwable);
    }
  }

  public Slf4jLogger getLogger(String name) {
    return new Slf4jLogger(LoggerFactory.getLogger(name));
  }

  public Slf4jLogger getLogger(Class name) {
    return new Slf4jLogger(LoggerFactory.getLogger(name));
  }
}
