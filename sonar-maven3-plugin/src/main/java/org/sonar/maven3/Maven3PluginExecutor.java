/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.maven3;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.batch.MavenPluginExecutor;

import java.util.Arrays;

public class Maven3PluginExecutor implements MavenPluginExecutor {

  private LifecycleExecutor lifecycleExecutor;
  private MavenSession mavenSession;

  public Maven3PluginExecutor(LifecycleExecutor le, MavenSession mavenSession) {
    this.lifecycleExecutor = le;
    this.mavenSession = mavenSession;
  }

  public MavenPluginHandler execute(Project project, MavenPluginHandler handler) {
    for (String goal : handler.getGoals()) {
      MavenPlugin plugin = MavenPlugin.getPlugin(project.getPom(), handler.getGroupId(), handler.getArtifactId());
      execute(project, getGoal(handler.getGroupId(), handler.getArtifactId(), plugin.getPlugin().getVersion(), goal));
    }
    return handler;
  }

  public void execute(Project project, String goalOrPhase) {
    TimeProfiler profiler = new TimeProfiler().start("Execute " + goalOrPhase);
    try {
      MavenSession projectSession = mavenSession.clone();
      projectSession.setCurrentProject(project.getPom());
      projectSession.setProjects(Arrays.asList(project.getPom()));
      projectSession.getRequest().setRecursive(false);
      projectSession.getRequest().setPom(project.getPom().getFile());
      projectSession.getRequest().setGoals(Arrays.asList(goalOrPhase));
      projectSession.getRequest().setInteractiveMode(false);
      lifecycleExecutor.execute(projectSession);

    } catch (Exception e) {
      throw new SonarException("Unable to execute maven plugin", e);

    } finally {
      profiler.stop();
    }
  }

  protected static String getGoal(String groupId, String artifactId, String version, String goal) {
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
