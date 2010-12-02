package org.sonar.tests.violationstimemachine;

public class Hello {

  // We need two similar violations here to check that they would be associated correctly

  protected void methodOne() {
    int i = 0; // unused local variable
    i++;
  }

  protected void methodTwo() {
    int i = 0; // unused local variable
    i++;
  }

  public final int methodReturnThree() { // fixed - design for extension
    int j = 0; // new - unused local variable
    return 3;
  }
}
