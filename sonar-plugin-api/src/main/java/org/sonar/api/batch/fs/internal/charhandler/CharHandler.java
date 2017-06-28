package org.sonar.api.batch.fs.internal.charhandler;

public abstract class CharHandler {

  public void handleAll(char c) {
  }

  public void handleIgnoreEoL(char c) {
  }

  public void newLine() {
  }

  public void eof() {
  }
}