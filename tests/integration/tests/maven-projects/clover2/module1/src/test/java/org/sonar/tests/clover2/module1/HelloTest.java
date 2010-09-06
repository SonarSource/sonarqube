package org.sonar.tests.clover2.module1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;


public class HelloTest {

  	@Test
	  public void shouldSayHello() {
	    assertEquals("hi", new Hello("hi").say());
	  }

	  @Test
	  public void thisIsError() {
	    assertEquals("foo", "bar");
	  }

	  @Test
	  public void thisIsFailure() {
	    throw new RuntimeException();
	  }

	  @Test
	  public void anotherTest() {
	    assertEquals("hi", new Hello("hi").say());
	  }

	  @Test
	  public void again() {
	    assertEquals("hi", new Hello("hi").say());
	  }

	  @Test
	  public void thelast() {
	    assertEquals("hi", new Hello("hi").say());
	  }
}
