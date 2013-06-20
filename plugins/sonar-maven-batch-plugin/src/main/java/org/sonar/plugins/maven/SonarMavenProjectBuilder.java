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
import org.apache.maven.project.MavenProject;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectBuilderContext;

import java.util.List;

@SupportedEnvironment("maven")
public class SonarMavenProjectBuilder extends ProjectBuilder {

  private MavenSession session;
  private MavenProjectConverter mavenProjectConverter;

  public SonarMavenProjectBuilder(MavenSession session, MavenProjectConverter mavenProjectConverter) {
    this.session = session;
    this.mavenProjectConverter = mavenProjectConverter;
  }

  @Override
  public void build(ProjectBuilderContext context) {
    // Don't use session.getTopLevelProject or session.getProjects to keep compatibility with Maven 2
    List<MavenProject> sortedProjects = session.getSortedProjects();
    MavenProject topLevelProject = null;
    for (MavenProject project : sortedProjects) {
      if (project.isExecutionRoot()) {
        topLevelProject = project;
        break;
      }
    }
    mavenProjectConverter.configure(context.getProjectReactor().getRoot(), sortedProjects, topLevelProject);
  }

}
