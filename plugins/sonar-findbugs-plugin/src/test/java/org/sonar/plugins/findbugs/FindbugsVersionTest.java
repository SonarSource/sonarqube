package org.sonar.plugins.findbugs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import org.junit.Test;

public class FindbugsVersionTest {

  @Test
  public void getFindbugsVersion() {
    assertThat(FindbugsVersion.getVersion().length(), greaterThan(1));
  }

}
