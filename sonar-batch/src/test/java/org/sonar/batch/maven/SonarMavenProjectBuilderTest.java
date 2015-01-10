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
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.bootstrap.ProjectDefinition;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SonarMavenProjectBuilderTest {

  @Test
  public void testSimpleProject() {
    MavenSession session = mock(MavenSession.class);
    MavenProject rootProject = mock(MavenProject.class);
    when(rootProject.isExecutionRoot()).thenReturn(true);
    when(session.getSortedProjects()).thenReturn(Arrays.asList(rootProject));

    MavenProjectConverter mavenProjectConverter = mock(MavenProjectConverter.class);
    ProjectDefinition projectDefinition = ProjectDefinition.create();
    when(mavenProjectConverter.configure(any(List.class), any(MavenProject.class))).thenReturn(projectDefinition);
    MavenProjectBootstrapper builder = new MavenProjectBootstrapper(session, mavenProjectConverter);

    assertThat(builder.bootstrap().getRoot()).isEqualTo(projectDefinition);

    ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
    verify(mavenProjectConverter).configure(argument.capture(), eq(rootProject));
    assertThat(argument.getValue()).contains(rootProject);
  }

  @Test
  public void testMultimoduleProject() {
    MavenSession session = mock(MavenSession.class);
    MavenProject rootProject = mock(MavenProject.class);
    MavenProject module1 = mock(MavenProject.class);
    MavenProject module2 = mock(MavenProject.class);
    when(rootProject.isExecutionRoot()).thenReturn(true);
    when(module1.isExecutionRoot()).thenReturn(false);
    when(module2.isExecutionRoot()).thenReturn(false);
    when(session.getSortedProjects()).thenReturn(Arrays.asList(module1, module2, rootProject));

    MavenProjectConverter mavenProjectConverter = mock(MavenProjectConverter.class);
    ProjectDefinition projectDefinition = ProjectDefinition.create();
    when(mavenProjectConverter.configure(any(List.class), any(MavenProject.class))).thenReturn(projectDefinition);
    MavenProjectBootstrapper builder = new MavenProjectBootstrapper(session, mavenProjectConverter);

    assertThat(builder.bootstrap().getRoot()).isEqualTo(projectDefinition);

    ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
    verify(mavenProjectConverter).configure(argument.capture(), eq(rootProject));
    assertThat(argument.getValue()).contains(module1, module2, rootProject);
  }

}
