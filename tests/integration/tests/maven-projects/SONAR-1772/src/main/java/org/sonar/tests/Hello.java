package org.sonar.tests;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;

public final class Hello {
  private final LineNumberReader reader;

  public Hello(Reader r) {
    this.reader = new LineNumberReader(r);
  }

  public String[] readTokens() throws IOException {
    return reader.readLine().split("\\|"); // Dodgy - Dereference of the result of readLine() without nullcheck
  }
}
