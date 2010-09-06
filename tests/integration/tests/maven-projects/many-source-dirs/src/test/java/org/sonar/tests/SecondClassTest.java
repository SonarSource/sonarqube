package org.sonar.tests;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SecondClassTest {
	
	@Test
	public void hello() {
		assertEquals("hello", new SecondClass(3).foo());
	}

  @Test
	public void error() {
		assertEquals("no", new SecondClass(3).foo());
	}
}