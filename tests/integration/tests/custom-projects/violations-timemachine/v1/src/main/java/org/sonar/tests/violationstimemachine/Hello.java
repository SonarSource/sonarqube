package org.sonar.tests.violationstimemachine;

public class Hello {

  // We need two similar violations here to check that they would be associated correctly

  protected void methodOne() { // design for extension
    int i = 0; // unused local variable
    i++;
  }

  protected void methodTwo() { // design for extension
    int i = 0; // unused local variable
    i++;
  }

  public int methodReturnThree() { // design for extension
    return 3;
  }
}
