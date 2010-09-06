package org.sonar.samples;

import junit.framework.TestCase;

public class ClassUnderTestTest extends TestCase {
  
  public void testHello() {
    ClassUnderTest instance = new ClassUnderTest();
    assertEquals("hello", instance.hello());
  }

  public void testToto() throws Exception {
    ClassUnderTest instance = new ClassUnderTest();
    instance.toto();
  }
}
