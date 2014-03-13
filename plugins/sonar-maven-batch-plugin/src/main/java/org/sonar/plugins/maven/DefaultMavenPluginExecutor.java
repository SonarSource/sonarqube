/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.project.MavenProject;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.batch.scan.maven.MavenPluginExecutor;

import javax.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;

@SupportedEnvironment("maven")
public class DefaultMavenPluginExecutor implements MavenPluginExecutor {

  private final MavenProjectConverter pomConverter;
  private LifecycleExecutor lifecycleExecutor;
  private MavenSession mavenSession;

  public DefaultMavenPluginExecutor(LifecycleExecutor le, MavenSession mavenSession, MavenProjectConverter pomConverter) {
    this.lifecycleExecutor = le;
    this.mavenSession = mavenSession;
    this.pomConverter = pomConverter;
  }

  @Override
  public final MavenPluginHandler execute(Project project, DefaultModuleFileSystem fs, MavenPluginHandler handler) {
    for (String goal : handler.getGoals()) {
      if (goal == null) {
        throw new IllegalStateException("Maven goal can't be null");
      }
      MavenPlugin plugin = MavenPlugin.getPlugin(project.getPom(), handler.getGroupId(), handler.getArtifactId());
      execute(project,
        fs,
        getGoal(handler.getGroupId(), handler.getArtifactId(), plugin != null && plugin.getPlugin() != null ? plugin.getPlugin().getVersion() : null, goal));
    }
    return handler;
  }

  @Override
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
      if (!fs.isInitialized()) {
        pomConverter.synchronizeFileSystem(project.getPom(), fs);
      }
    }
  }

  static String getGoal(String groupId, String artifactId, @Nullable String version, String goal) {
    String defaultVersion = version == null ? "" : version;
    return new StringBuilder()
      .append(groupId).append(":")
      .append(artifactId).append(":")
      .append(defaultVersion)
      .append(":")
      .append(goal)
      .toString();
  }

  public void concreteExecute(MavenProject pom, String goal) {
    Method executeMethod = null;
    for (Method m : lifecycleExecutor.getClass().getMethods()) {
      if ("execute".equals(m.getName())) {
        executeMethod = m;
        break;
      }
    }
    if (executeMethod == null) {
      throw new SonarException("Unable to find execute method on Maven LifecycleExecutor. Please check your Maven version.");
    }
    if (executeMethod.getParameterTypes().length == 1) {
      concreteExecuteMaven3(pom, goal);
    } else if (executeMethod.getParameterTypes().length == 3) {
      concreteExecuteMaven2(executeMethod, pom, goal);
    } else {
      throw new SonarException("Unexpected parameter count on Maven LifecycleExecutor#execute method. Please check your Maven version.");
    }
  }

  public void concreteExecuteMaven3(MavenProject pom, String goal) {
    MavenSession projectSession = mavenSession.clone();
    projectSession.setCurrentProject(pom);
    projectSession.setProjects(Arrays.asList(pom));
    projectSession.getRequest().setRecursive(false);
    projectSession.getRequest().setPom(pom.getFile());
    projectSession.getRequest().setGoals(Arrays.asList(goal));
    projectSession.getRequest().setInteractiveMode(false);
    lifecycleExecutor.execute(projectSession);
    if (projectSession.getResult().hasExceptions()) {
      throw new SonarException("Exception during execution of " + goal);
    }
  }

  public void concreteExecuteMaven2(Method executeMethod, MavenProject pom, String goal) {
    try {
      ReactorManager reactor = new ReactorManager(Arrays.asList(pom));
      MavenSession clonedSession = new MavenSession(mavenSession.getContainer(),
        mavenSession.getSettings(),
        mavenSession.getLocalRepository(),
        mavenSession.getEventDispatcher(),
        reactor,
        Arrays.asList(goal),
        mavenSession.getExecutionRootDirectory(),
        mavenSession.getExecutionProperties(),
        mavenSession.getStartTime());
      executeMethod.invoke(lifecycleExecutor, clonedSession, reactor, clonedSession.getEventDispatcher());
    } catch (Exception e) {
      throw new SonarException("Unable to execute Maven 2 plugin", e);
    }
  }

}
