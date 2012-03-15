package org.sonar.plugins.core.timemachine.tracking;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StringTextComparatorTest {

  @Test
  public void testEquals() {
    StringTextComparator cmp = StringTextComparator.IGNORE_WHITESPACE;

    StringText a = new StringText("abc\nabc\na bc");
    StringText b = new StringText("abc\nabc d\nab c");

    assertThat("abc == abc", cmp.equals(a, 0, b, 0), is(true));
    assertThat("abc != abc d", cmp.equals(a, 1, b, 1), is(false));
    assertThat("a bc == ab c", cmp.equals(a, 2, b, 2), is(true));
    assertThat(cmp.hash(a, 0), equalTo(cmp.hash(b, 0)));
    assertThat(cmp.hash(a, 2), equalTo(cmp.hash(b, 2)));
  }

}
