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
package org.sonar.plugins.findbugs;

import com.google.common.collect.Lists;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.test.MavenTestUtils;

import java.util.ArrayList;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FindbugsMavenInitializerTest {

  private Project project;
  private FindbugsMavenInitializer initializer;

  @Before
  public void setUp() {
    project = mock(Project.class);
    initializer = new FindbugsMavenInitializer();
  }

  @Test
  public void shouldNotAnalyseIfNoJavaProject() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn("php");
    assertThat(initializer.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void shouldNotAnalyseIfJavaProjectButNoSource() {
    Project project = mock(Project.class);
    ProjectFileSystem fs = mock(ProjectFileSystem.class);
    when(fs.mainFiles("java")).thenReturn(new ArrayList<InputFile>());
    when(project.getFileSystem()).thenReturn(fs);
    when(project.getLanguageKey()).thenReturn("java");
    assertThat(initializer.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void shouldAnalyse() {
    Project project = mock(Project.class);
    ProjectFileSystem fs = mock(ProjectFileSystem.class);
    when(fs.mainFiles("java")).thenReturn(Lists.newArrayList(InputFileUtils.create(null, "")));
    when(project.getFileSystem()).thenReturn(fs);
    when(project.getLanguageKey()).thenReturn("java");
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(initializer.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void doNotSetExcludesFiltersIfAlreadyConfigured() {
    Configuration configuration = mock(Configuration.class);
    when(configuration.containsKey(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY)).thenReturn(true);
    when(project.getConfiguration()).thenReturn(configuration);
    initializer.execute(project);
    verify(configuration, never()).setProperty(eq(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY), anyString());
  }

  @Test
  public void shouldGetExcludesFiltersFromPom() {
    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "pom.xml");
    initializer.execute(project);
    assertThat(project.getConfiguration().getString(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY)).isEqualTo("foo.xml");
  }

}
