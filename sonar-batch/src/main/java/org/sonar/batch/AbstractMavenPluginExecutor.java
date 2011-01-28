package org.sonar.batch;

import org.apache.maven.project.MavenProject;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;

/**
 * Abstract implementation of {@link MavenPluginExecutor} to reduce duplications in concrete implementations for different Maven versions.
 */
public abstract class AbstractMavenPluginExecutor implements MavenPluginExecutor {

  public final MavenPluginHandler execute(Project project, MavenPluginHandler handler) {
    for (String goal : handler.getGoals()) {
      MavenPlugin plugin = MavenPlugin.getPlugin(project.getPom(), handler.getGroupId(), handler.getArtifactId());
      execute(project, getGoal(handler.getGroupId(), handler.getArtifactId(), plugin.getPlugin().getVersion(), goal));
    }
    return handler;
  }

  public final void execute(Project project, String goal) {
    TimeProfiler profiler = new TimeProfiler().start("Execute " + goal);
    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      concreteExecute(project.getPom(), goal);
    } catch (Exception e) {
      throw new SonarException("Unable to execute maven plugin", e);
    } finally {
      // Reset original ClassLoader that may have been changed during Maven Execution (see SONAR-1800)
      Thread.currentThread().setContextClassLoader(currentClassLoader);
      profiler.stop();
    }
  }

  public abstract void concreteExecute(MavenProject pom, String goal) throws Exception;

  static String getGoal(String groupId, String artifactId, String version, String goal) {
    String defaultVersion = (version == null ? "" : version);
    return new StringBuilder()
        .append(groupId).append(":")
        .append(artifactId).append(":")
        .append(defaultVersion)
        .append(":")
        .append(goal)
        .toString();
  }

}
