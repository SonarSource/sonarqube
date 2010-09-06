package org.sonar.tests.rulesOnTests;

import junit.framework.TestCase;

public class HelloTest extends TestCase {
  
  public void testHello() {
    Hello instance = new Hello();
    assertEquals("hello", instance.hello());
  }

  public void testToto() throws Exception {
    Hello instance = new Hello();
    instance.foo();
  }
}
