/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.jacoco;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class JacocoMavenInitializerTest {
  private JaCoCoMavenPluginHandler mavenPluginHandler;
  private JacocoMavenInitializer initializer;
  private JacocoConfiguration jacocoSettings;

  @Before
  public void setUp() {
    mavenPluginHandler = mock(JaCoCoMavenPluginHandler.class);
    jacocoSettings = mock(JacocoConfiguration.class);
    when(jacocoSettings.isEnabled()).thenReturn(true);
    initializer = new JacocoMavenInitializer(mavenPluginHandler, jacocoSettings);
  }

  @Test
  public void shouldDoNothing() {
    Project project = mockProject();
    initializer.execute(project);
    verifyNoMoreInteractions(project);
    verifyNoMoreInteractions(mavenPluginHandler);
  }

  @Test
  public void shouldExecuteMaven() {
    Project project = mockProject();
    InputFile inputFile = mock(InputFile.class);
    when(project.getFileSystem().testFiles(Java.KEY)).thenReturn(Collections.singletonList(inputFile));
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);

    assertThat(initializer.shouldExecuteOnProject(project)).isTrue();
    assertThat(initializer.getMavenPluginHandler(project)).isInstanceOf(JaCoCoMavenPluginHandler.class);
  }

  @Test
  public void shouldNotExecuteMavenWhenReuseReports() {
    Project project = mockProject();
    InputFile inputFile = mock(InputFile.class);
    when(project.getFileSystem().testFiles(Java.KEY)).thenReturn(Collections.singletonList(inputFile));
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);

    assertThat(initializer.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void shouldNotExecuteMavenWhenNoTests() {
    Project project = mockProject();
    when(project.getFileSystem().hasTestFiles(argThat(Is.is(Java.INSTANCE)))).thenReturn(false);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);

    assertThat(initializer.shouldExecuteOnProject(project)).isFalse();
  }

  private Project mockProject() {
    Project project = mock(Project.class);
    ProjectFileSystem projectFileSystem = mock(ProjectFileSystem.class);
    when(project.getFileSystem()).thenReturn(projectFileSystem);
    return project;
  }
}
