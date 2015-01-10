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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MavenProjectBootstrapperTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void bootstrap() throws Exception {
    ProjectDefinition def = mock(ProjectDefinition.class);
    MavenSession session = mock(MavenSession.class);
    MavenProject rootProject = new MavenProject();
    rootProject.setExecutionRoot(true);
    List<MavenProject> projects = Arrays.asList(rootProject);
    when(session.getSortedProjects()).thenReturn(projects);

    MavenProjectConverter pomConverter = mock(MavenProjectConverter.class);
    when(pomConverter.configure(projects, rootProject)).thenReturn(def);
    MavenProjectBootstrapper bootstrapper = new MavenProjectBootstrapper(session, pomConverter);

    ProjectReactor reactor = bootstrapper.bootstrap();

    assertThat(reactor).isNotNull();
    verify(pomConverter).configure(projects, rootProject);
  }

  @Test
  public void should_fail_if_no_top_level_project() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Maven session does not declare a top level project");

    MavenSession session = mock(MavenSession.class);
    MavenProjectConverter pomConverter = new MavenProjectConverter();
    MavenProjectBootstrapper bootstrapper = new MavenProjectBootstrapper(session, pomConverter);

    bootstrapper.bootstrap();
  }
}
