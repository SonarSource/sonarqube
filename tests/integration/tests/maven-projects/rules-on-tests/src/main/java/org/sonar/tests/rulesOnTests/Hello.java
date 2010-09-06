package org.sonar.tests.rulesOnTests;

public class Hello {
  public void foo() throws Exception {
    int i=0;
    i++;
    hello();
  }
  public String hello() {
    return "hello";
  }
  public boolean equals(Object o) {
	// TODO
    return true;	
  }
}
