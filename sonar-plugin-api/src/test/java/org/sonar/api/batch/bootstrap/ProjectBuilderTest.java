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
package org.sonar.api.batch.bootstrap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class ProjectBuilderTest {

  @Test
  public void shouldChangeProject() {
    // this reactor is created and injected by Sonar
    ProjectReactor projectReactor = new ProjectReactor(ProjectDefinition.create());

    ProjectBuilder builder = new ProjectBuilderSample(projectReactor, new PropertiesConfiguration());
    builder.start();

    assertThat(projectReactor.getProjects().size(), is(2));
    ProjectDefinition root = projectReactor.getRoot();
    assertThat(root.getName(), is("Name changed by plugin"));
    assertThat(root.getSubProjects().size(), is(1));
    assertThat(root.getSubProjects().get(0).getSourceDirs(), hasItem("src"));
  }

  final static class ProjectBuilderSample extends ProjectBuilder {
    private Configuration conf;

    public ProjectBuilderSample(ProjectReactor reactor, Configuration conf) {
      super(reactor);

      // A real implementation should for example use the configuration
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
      subProject.setKey("groupId:subProjectId");
      subProject.setVersion(root.getVersion());
      subProject.setName("Sub Project");
      subProject.setSourceDirs("src");
      root.addSubProject(subProject);
    }
  }

}
