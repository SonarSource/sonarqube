package org.sonar.updatecenter.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class PluginKeyUtilsTest {

  @Test
  public void shouldExtractCorrectPluginKey() {
    assertThat(PluginKeyUtils.getPluginKey("sonar-test-plugin"), is("test"));
    assertThat(PluginKeyUtils.getPluginKey("test-sonar-plugin"), is("test"));
    assertThat(PluginKeyUtils.getPluginKey("test"), is("test"));

    assertThat(PluginKeyUtils.getPluginKey("sonar-test-foo-plugin"), is("testfoo"));
    assertThat(PluginKeyUtils.getPluginKey("test-foo-sonar-plugin"), is("testfoo"));
    assertThat(PluginKeyUtils.getPluginKey("test-foo"), is("testfoo"));
  }
}
