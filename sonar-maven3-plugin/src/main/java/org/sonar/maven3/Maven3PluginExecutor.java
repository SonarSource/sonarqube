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
package org.sonar.maven3;

import java.util.Arrays;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.project.MavenProject;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.scan.maven.AbstractMavenPluginExecutor;

public class Maven3PluginExecutor extends AbstractMavenPluginExecutor {

  private LifecycleExecutor lifecycleExecutor;
  private MavenSession mavenSession;

  public Maven3PluginExecutor(LifecycleExecutor le, MavenSession mavenSession) {
    this.lifecycleExecutor = le;
    this.mavenSession = mavenSession;
  }

  @Override
  public void concreteExecute(MavenProject pom, String goal) {
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

}
