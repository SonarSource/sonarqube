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
package org.sonar.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.project.MavenProject;
import org.sonar.batch.scan.maven.AbstractMavenPluginExecutor;

import java.util.Arrays;

public class Maven2PluginExecutor extends AbstractMavenPluginExecutor {

  private LifecycleExecutor lifecycleExecutor;
  private MavenSession mavenSession;

  public Maven2PluginExecutor(LifecycleExecutor le, MavenSession mavenSession) {
    this.lifecycleExecutor = le;
    this.mavenSession = mavenSession;
  }

  @Override
  public void concreteExecute(MavenProject pom, String goal) throws Exception {
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
    lifecycleExecutor.execute(clonedSession, reactor, clonedSession.getEventDispatcher());
  }

}
