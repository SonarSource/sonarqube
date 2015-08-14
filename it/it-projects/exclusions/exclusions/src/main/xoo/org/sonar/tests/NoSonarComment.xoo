package org.sonar.tests;

import java.lang.String;//NOSONAR

public class NoSonarComment {

  public NoSonarComment(int i) {//NOSONAR
    i=3;// NOSONAR
    i=4;  // ERROR magic number, parameter assignment
    String s="foo";
    if (s=="bar") return; // ERROR: compare Strings with equals()
  }
}
