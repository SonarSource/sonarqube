package org.sonar.process;

public interface ProcessMXBean {

  boolean isReady();

  void ping();

  void stop();
}
