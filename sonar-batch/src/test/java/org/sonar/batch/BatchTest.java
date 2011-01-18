package org.sonar.batch;

import org.junit.Test;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class BatchTest {

  class MyMavenPluginExecutor implements MavenPluginExecutor {
    public void execute(Project project, String goal) {
    }

    public MavenPluginHandler execute(Project project, MavenPluginHandler handler) {
      return handler;
    }
  }

  @Test
  public void shouldSearchMavenPluginExecutor() {
    Batch batch;

    batch = new Batch(null, MyMavenPluginExecutor.class);
    assertThat(batch.isMavenPluginExecutorRegistered(), is(true));

    batch = new Batch(null);
    assertThat(batch.isMavenPluginExecutorRegistered(), is(false));
  }
}
