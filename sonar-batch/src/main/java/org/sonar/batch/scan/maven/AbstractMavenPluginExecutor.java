/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.scan.maven;

import org.apache.maven.project.MavenProject;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;

/**
 * Abstract implementation of {@link org.sonar.batch.scan.maven.MavenPluginExecutor} to reduce duplications in concrete implementations for different Maven versions.
 */
public abstract class AbstractMavenPluginExecutor implements MavenPluginExecutor {

  public final MavenPluginHandler execute(Project project, DefaultModuleFileSystem fs, MavenPluginHandler handler) {
    for (String goal : handler.getGoals()) {
      MavenPlugin plugin = MavenPlugin.getPlugin(project.getPom(), handler.getGroupId(), handler.getArtifactId());
      execute(project,
        fs,
        getGoal(handler.getGroupId(), handler.getArtifactId(), (plugin != null && plugin.getPlugin() != null ? plugin.getPlugin().getVersion() : null), goal));
    }
    return handler;
  }

  public final void execute(Project project, DefaultModuleFileSystem fs, String goal) {
    if (project.getPom() != null) {
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

      MavenProjectConverter.synchronizeFileSystem(project.getPom(), fs);
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
