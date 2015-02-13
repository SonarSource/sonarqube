package org.sonar.api.utils.log.internal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import org.sonar.api.utils.log.Logger;

import java.util.List;

/**
 * Implementation of {@link org.sonar.api.utils.log.Logger} which keeps logs
 * in memory, so that they can be loaded after writing. It is helpful
 * for testing.
 */
public class InMemoryLogger implements Logger {

  private static enum Level {
    DEBUG, INFO, WARN, ERROR
  }

  private boolean debugEnabled = false;
  private final ArrayListMultimap<Level, String> logs = ArrayListMultimap.create();

  @Override
  public boolean isDebugEnabled() {
    return debugEnabled;
  }

  public InMemoryLogger setDebugEnabled(boolean b) {
    this.debugEnabled = b;
    return this;
  }

  @Override
  public void debug(String message) {
    if (isDebugEnabled()) {
      log(Level.DEBUG, message);
    }
  }

  @Override
  public void info(String message) {
    log(Level.INFO, message);
  }

  @Override
  public void warn(String message) {
    log(Level.WARN, message);
  }

  @Override
  public void error(String message) {
    log(Level.ERROR, message);
  }

  @Override
  public void error(String message, Throwable throwable) {
    log(Level.ERROR, String.format("%s | %s", message, throwable.getMessage()));
  }

  public List<String> logs() {
    return Lists.newArrayList(logs.values());
  }

  public List<String> debugLogs() {
    return logs.get(Level.DEBUG);
  }

  public List<String> infoLogs() {
    return logs.get(Level.INFO);
  }

  public List<String> warnLogs() {
    return logs.get(Level.WARN);
  }

  public List<String> errorLogs() {
    return logs.get(Level.ERROR);
  }

  public InMemoryLogger clear() {
    logs.clear();
    return this;
  }

  private void log(Level level, String message) {
    logs.put(level, message);
  }
}
