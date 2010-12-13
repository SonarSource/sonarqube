package org.sonar.tests.violationstimemachine;

public class FileAddedInV2 {

  protected void methodOne() { // design for extension
    int i = 0; // unused local variable
    i++;
  }

}
