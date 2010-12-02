package org.sonar.tests.violationstimemachine;

public class FileRemovedInV2 {

  protected void methodOne() {
    int i = 0; // unused local variable
    i++;
  }

}
