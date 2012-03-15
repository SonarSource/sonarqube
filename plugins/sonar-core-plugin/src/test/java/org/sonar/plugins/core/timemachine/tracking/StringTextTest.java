package org.sonar.plugins.core.timemachine.tracking;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StringTextTest {

  @Test
  public void testEmpty() {
    StringText r = new StringText("");
    assertThat(r.length(), is(0));
  }

  @Test
  public void testTwoLines() {
    StringText r = new StringText("a\nb");
    assertThat(r.length(), is(2));
  }

}
