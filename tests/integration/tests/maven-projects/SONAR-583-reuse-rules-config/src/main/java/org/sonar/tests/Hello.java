package org.sonar.tests;

public class Hello {
  
  private String hello;
  private int magicNumber = 50;
  
  public Hello(String s) {
    this.hello = s;
  }
  
  private void say() {
  	System.out.println(hello);
  }
  
  public int designForExtensionViolation() {
    return 3;	
  }
  
  private boolean simplifyBooleanReturn() {
    if (true) {
      return true;
    } else {
      return false;
    }
  }
}
