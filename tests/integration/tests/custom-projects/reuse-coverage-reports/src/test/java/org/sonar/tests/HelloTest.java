package org.sonar.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;


public class HelloTest {

  @Test
  public void shouldSayHello() {
    assertEquals("hi", new Hello("hi").say());
  }
  
  @Test
  public void shouldNotFail() {
    assertEquals(true, true);
  }
}
