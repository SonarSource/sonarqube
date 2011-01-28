package org.sonar.batch;

import org.junit.Test;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AbstractMavenPluginExecutorTest {

  @Test
  public void pluginVersionIsOptional() {
    assertThat(AbstractMavenPluginExecutor.getGoal("group", "artifact", null, "goal"), is("group:artifact::goal"));
  }

  static class FakeCheckstyleMavenPluginHandler implements MavenPluginHandler {
    public String getGroupId() {
      return "org.apache.maven.plugins";
    }

    public String getArtifactId() {
      return "maven-checkstyle-plugin";
    }

    public String getVersion() {
      return "2.2";
    }

    public boolean isFixedVersion() {
      return false;
    }

    public String[] getGoals() {
      return new String[] { "checkstyle" };
    }

    public void configure(Project project, MavenPlugin plugin) {
    }
  }

}
