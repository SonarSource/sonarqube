package org.sonar.batch.bootstrapper;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class BootstrapperVersionTest {

  @Test
  public void shouldLoadVersion() {
    String version = BootstrapperVersion.getVersion();
    assertThat(version, containsString("."));
    assertThat(version, not(containsString("$")));
  }

}
