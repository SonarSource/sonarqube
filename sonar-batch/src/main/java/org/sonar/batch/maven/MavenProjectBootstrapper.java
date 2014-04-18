/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.batch.bootstrap.ProjectBootstrapper;
import org.sonar.api.batch.bootstrap.ProjectReactor;

import java.util.List;

/**
 * @deprecated since 4.3 kept only to support old version of SonarQube Mojo
 */
@Deprecated
@SupportedEnvironment("maven")
public class MavenProjectBootstrapper implements ProjectBootstrapper {

  private MavenSession session;
  private MavenProjectConverter mavenProjectConverter;

  public MavenProjectBootstrapper(MavenSession session, MavenProjectConverter mavenProjectConverter) {
    this.session = session;
    this.mavenProjectConverter = mavenProjectConverter;
  }

  @Override
  public ProjectReactor bootstrap() {
    // Don't use session.getTopLevelProject or session.getProjects to keep compatibility with Maven 2
    List<MavenProject> sortedProjects = session.getSortedProjects();
    MavenProject topLevelProject = null;
    for (MavenProject project : sortedProjects) {
      if (project.isExecutionRoot()) {
        topLevelProject = project;
        break;
      }
    }
    if (topLevelProject == null) {
      throw new IllegalStateException("Maven session does not declare a top level project");
    }
    return new ProjectReactor(mavenProjectConverter.configure(sortedProjects, topLevelProject));
  }

}
