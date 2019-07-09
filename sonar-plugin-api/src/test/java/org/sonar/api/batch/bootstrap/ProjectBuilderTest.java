/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch.bootstrap;

import java.io.File;
import org.junit.Test;
import org.sonar.api.batch.bootstrap.internal.ProjectBuilderContext;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ProjectBuilderTest {

  @Test
  public void shouldChangeProject() {
    // this reactor is created and provided by Sonar
    final ProjectReactor projectReactor = new ProjectReactor(ProjectDefinition.create());

    ProjectBuilder builder = new ProjectBuilderSample(new MapSettings());
    builder.build(new ProjectBuilderContext(projectReactor));

    assertThat(projectReactor.getProjects().size(), is(2));
    ProjectDefinition root = projectReactor.getRoot();
    assertThat(root.getName(), is("Name changed by plugin"));
    assertThat(root.getSubProjects().size(), is(1));
    assertThat(root.getSubProjects().get(0).sources()).contains("src");
  }

  final static class ProjectBuilderSample extends ProjectBuilder {
    private Settings conf;

    public ProjectBuilderSample(Settings conf) {
      // A real implementation should for example use the settings
      this.conf = conf;
    }

    @Override
    protected void build(ProjectReactor reactor) {
      // change name of root project
      ProjectDefinition root = reactor.getRoot();
      root.setName("Name changed by plugin");

      // add sub-project
      File baseDir = new File(root.getBaseDir(), "path/to/subproject");
      ProjectDefinition subProject = ProjectDefinition.create();
      subProject.setBaseDir(baseDir);
      subProject.setWorkDir(new File(baseDir, "target/.sonar"));
      subProject.setKey("groupId:parentProjectId");
      subProject.setProjectVersion(root.getOriginalVersion());
      subProject.setName("Sub Project");
      subProject.setSources("src");
      root.addSubProject(subProject);
    }
  }

}
