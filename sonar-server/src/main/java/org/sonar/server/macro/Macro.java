package org.sonar.server.macro;

public interface Macro {

  String getRegex();

  String getReplacement();
}
