/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.cobertura;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.configuration.Configuration;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.resources.Project;
import org.sonar.api.test.MavenTestUtils;

public class CoberturaMavenInitializerTest {

  private Project project;
  private CoberturaMavenInitializer initializer;

  @Before
  public void setUp() {
    project = mock(Project.class);
    initializer = new CoberturaMavenInitializer(new CoberturaMavenPluginHandler());
  }

  @Test
  public void doNotExecuteMavenPluginIfReuseReports() {
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(initializer.getMavenPluginHandler(project), nullValue());
  }

  @Test
  public void doNotExecuteMavenPluginIfStaticAnalysis() {
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    assertThat(initializer.getMavenPluginHandler(project), nullValue());
  }

  @Test
  public void executeMavenPluginIfDynamicAnalysis() {
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(initializer.getMavenPluginHandler(project), not(nullValue()));
    assertThat(initializer.getMavenPluginHandler(project).getArtifactId(), is("cobertura-maven-plugin"));
  }

  @Test
  public void doNotSetReportPathIfAlreadyConfigured() {
    Configuration configuration = mock(Configuration.class);
    when(configuration.containsKey(CoreProperties.COBERTURA_REPORT_PATH_PROPERTY)).thenReturn(true);
    when(project.getConfiguration()).thenReturn(configuration);
    initializer.execute(project);
    verify(configuration, never()).setProperty(eq(CoreProperties.COBERTURA_REPORT_PATH_PROPERTY), anyString());
  }

  @Test
  public void shouldSetReportPathFromPom() {
    Configuration configuration = mock(Configuration.class);
    when(project.getConfiguration()).thenReturn(configuration);
    MavenProject pom = MavenTestUtils.loadPom("/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldGetReportPathFromPom/pom.xml");
    when(project.getPom()).thenReturn(pom);
    initializer.execute(project);
    verify(configuration).setProperty(eq(CoreProperties.COBERTURA_REPORT_PATH_PROPERTY), eq("overridden/dir/coverage.xml"));
  }
}
