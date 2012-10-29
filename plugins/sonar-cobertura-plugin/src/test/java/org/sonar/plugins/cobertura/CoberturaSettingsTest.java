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
package org.sonar.plugins.cobertura;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.plugins.java.api.JavaSettings;

import java.util.Arrays;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoberturaSettingsTest {

  private Settings settings;
  private JavaSettings javaSettings;
  private ProjectFileSystem fileSystem;
  private Project javaProject;
  private CoberturaSettings coberturaSettings;

  @Before
  public void before() {
    settings = new Settings();
    javaSettings = mock(JavaSettings.class);
    when(javaSettings.getEnabledCoveragePlugin()).thenReturn("cobertura");
    fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.mainFiles(Java.KEY)).thenReturn(Arrays.asList(InputFileUtils.create(null, "")));
    javaProject = mock(Project.class);
    when(javaProject.getLanguageKey()).thenReturn(Java.KEY);
    when(javaProject.getFileSystem()).thenReturn(fileSystem);
    when(javaProject.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    coberturaSettings = new CoberturaSettings(settings, javaSettings);
  }

  @Test
  public void should_be_enabled_if_project_with_java_sources() {
    assertThat(coberturaSettings.isEnabled(javaProject)).isTrue();
  }

  @Test
  public void should_be_disabled_if_not_java() {
    Project phpProject = mock(Project.class);
    when(phpProject.getLanguageKey()).thenReturn("php");

    assertThat(coberturaSettings.isEnabled(phpProject)).isFalse();
  }

  @Test
  public void should_be_disabled_if_java_project_without_sources() {
    when(fileSystem.mainFiles(Java.KEY)).thenReturn(Collections.<InputFile>emptyList());
    assertThat(coberturaSettings.isEnabled(javaProject)).isFalse();
  }

  @Test
  public void should_be_disabled_if_static_analysis_only() {
    when(javaProject.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    assertThat(coberturaSettings.isEnabled(javaProject)).isFalse();
  }

  @Test
  public void should_be_enabled_if_reuse_report_mode() {
    when(javaProject.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(coberturaSettings.isEnabled(javaProject)).isTrue();
  }

  @Test
  public void should_configure_max_memory() {
    settings.setProperty("sonar.cobertura.maxmem", "128m");
    assertThat(coberturaSettings.getMaxMemory()).isEqualTo("128m");
  }

  /**
   * http://jira.codehaus.org/browse/SONAR-2897: there used to be a typo in the parameter name (was "sonar.cobertura.maxmen")
   */
  @Test
  public void should_support_deprecated_max_memory() {
    settings.setProperty("sonar.cobertura.maxmen", "128m");
    assertThat(coberturaSettings.getMaxMemory()).isEqualTo("128m");
  }
}
