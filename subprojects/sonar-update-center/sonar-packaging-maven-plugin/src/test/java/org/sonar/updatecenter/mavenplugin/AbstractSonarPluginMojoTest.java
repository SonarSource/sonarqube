package org.sonar.updatecenter.mavenplugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

public class AbstractSonarPluginMojoTest {

  @Test
  public void shouldExtractPluginKeyFromArtifactId() {
    AbstractSonarPluginMojo mojo = new AbstractSonarPluginMojo() {
      public void execute() throws MojoExecutionException, MojoFailureException {
      }
    };
    mojo.pluginKey = "sonar-test-plugin";
    assertThat(mojo.getPluginKey(), is("test"));
    mojo.pluginKey = "test-sonar-plugin";
    assertThat(mojo.getPluginKey(), is("test"));
    mojo.pluginKey = "test";
    assertThat(mojo.getPluginKey(), is("test"));
  }

}
