package org.sonar.batch;

import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;

public class FakeMavenPluginExecutor implements MavenPluginExecutor {
  public void execute(Project project, String goal) {
    // do nothing
  }

  public MavenPluginHandler execute(Project project, MavenPluginHandler handler) {
    // do nothing
    return handler;
  }
}