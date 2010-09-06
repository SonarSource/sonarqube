package org.sonar.tests;

import org.junit.Test;
import static org.junit.Assert.fail;

public class ClassToExcludeTest {

  @Test
  public void increaseCodeCoverage() {
    new ClassToExclude().duplicatedMethod(222);
  }

  @Test
  public void shouldFail() {
    if (true) {
      fail();
    }
  }
}
