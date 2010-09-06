package org.sonar.tests.skipSurefireTests;

import static org.junit.Assert.assertEquals;
import org.junit.Test;


public class HelloTest {

  @Test
  public void shouldSayHello() {
    assertEquals("hi", new Hello("hi").say());
  }
}
