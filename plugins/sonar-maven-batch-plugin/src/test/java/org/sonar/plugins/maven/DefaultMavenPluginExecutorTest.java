package org.sonar.plugins.maven;

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

import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class DefaultMavenPluginExecutorTest {

  @Test
  public void plugin_version_should_be_optional() {
    assertThat(DefaultMavenPluginExecutor.getGoal("group", "artifact", null, "goal"), is("group:artifact::goal"));
  }

  @Test
  public void test_plugin_version() {
    assertThat(DefaultMavenPluginExecutor.getGoal("group", "artifact", "3.54", "goal"), is("group:artifact:3.54:goal"));
  }

  /**
   * The maven plugin sometimes changes the project structure (for example mvn build-helper:add-source). These changes
   * must be applied to the internal structure.
   */
  @Test
  public void should_reset_file_system_after_execution() {
    DefaultMavenPluginExecutor executor = new DefaultMavenPluginExecutor(null, null) {
      @Override
      public void concreteExecute(MavenProject pom, String goal) {
        pom.addCompileSourceRoot("src/java");
      }
    };
    MavenProject pom = new MavenProject();
    pom.setFile(new File("target/AbstractMavenPluginExecutorTest/pom.xml"));
    pom.getBuild().setDirectory("target");
    Project foo = new Project("foo");
    foo.setPom(pom);
    DefaultModuleFileSystem fs = mock(DefaultModuleFileSystem.class);
    executor.execute(foo, fs, new AddSourceMavenPluginHandler());

    verify(fs).resetDirs(any(File.class), any(File.class), anyList(), anyList(), anyList());
  }

  @Test
  public void should_ignore_non_maven_projects() {
    DefaultMavenPluginExecutor executor = new DefaultMavenPluginExecutor(null, null) {
      @Override
      public void concreteExecute(MavenProject pom, String goal) {
        pom.addCompileSourceRoot("src/java");
      }
    };
    Project foo = new Project("foo");
    DefaultModuleFileSystem fs = mock(DefaultModuleFileSystem.class);
    executor.execute(foo, fs, new AddSourceMavenPluginHandler());

    verify(fs, never()).resetDirs(any(File.class), any(File.class), anyList(), anyList(), anyList());
  }

  static class AddSourceMavenPluginHandler implements MavenPluginHandler {
    public String getGroupId() {
      return "fake";
    }

    public String getArtifactId() {
      return "fake";
    }

    public String getVersion() {
      return "2.2";
    }

    public boolean isFixedVersion() {
      return false;
    }

    public String[] getGoals() {
      return new String[] {"fake"};
    }

    public void configure(Project project, MavenPlugin plugin) {
    }
  }

}
