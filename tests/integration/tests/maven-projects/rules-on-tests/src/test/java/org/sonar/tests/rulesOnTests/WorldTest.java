package org.sonar.tests.rulesOnTests;

import static junit.framework.Assert.assertTrue;
import org.junit.Test;

public class WorldTest {

  @Test
  public void testFoo() {
    int i=0;
    i++;
  }

  @Test
  public void shouldFoo() {
    Object o = new World(30);
    String s = o.toString();
    assertTrue(s==o);
  }

  public boolean equals(Object o) {
	return false;
}
}
