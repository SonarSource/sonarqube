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
package org.sonar.plugins.batch.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;

import java.util.List;

/**
 * Class that inject MavenProject in each module container
 */
@SupportedEnvironment("maven")
public class MavenProjectBuilder extends ProjectBuilder {

  private final MavenSession mavenSession;

  public MavenProjectBuilder(MavenSession mavenSession) {
    this.mavenSession = mavenSession;
  }

  @Override
  public void build(Context context) {
    ProjectReactor reactor = context.projectReactor();
    for (ProjectDefinition moduleDef : reactor.getProjects()) {
      setMavenProjectIfApplicable(moduleDef);
    }
  }

  private void setMavenProjectIfApplicable(ProjectDefinition definition) {
    if (mavenSession != null) {
      String moduleKey = definition.getKey();
      for (MavenProject mavenModule : (List<MavenProject>) mavenSession.getProjects()) {
        // FIXME assumption that moduleKey was not modified by user and follow convention <groupId>:<artifactId>
        String mavenModuleKey = mavenModule.getGroupId() + ":" + mavenModule.getArtifactId();
        if (mavenModuleKey.equals(moduleKey) && !definition.getContainerExtensions().contains(mavenModule)) {
          definition.addContainerExtension(mavenModule);
        }
      }
    }
  }

}
