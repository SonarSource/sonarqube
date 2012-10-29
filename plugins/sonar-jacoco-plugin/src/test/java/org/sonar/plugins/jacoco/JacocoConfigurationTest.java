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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.plugins.java.api.JavaSettings;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JacocoConfigurationTest {

  private Settings settings;
  private JacocoConfiguration jacocoSettings;
  private JavaSettings javaSettings;

  @Before
  public void setUp() {
    JaCoCoAgentDownloader downloader = mock(JaCoCoAgentDownloader.class);
    when(downloader.getAgentJarFile()).thenReturn(new File("jacocoagent.jar"));
    javaSettings = mock(JavaSettings.class);
    settings = new Settings(new PropertyDefinitions(JacocoConfiguration.class));
    jacocoSettings = new JacocoConfiguration(settings, downloader, javaSettings);
  }

  @Test
  public void should_be_enabled() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn(Java.KEY);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    when(javaSettings.getEnabledCoveragePlugin()).thenReturn(JaCoCoUtils.PLUGIN_KEY);

    assertThat(jacocoSettings.isEnabled(project)).isTrue();
  }

  @Test
  public void should_be_enabled_if_reuse_report() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn(Java.KEY);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    when(javaSettings.getEnabledCoveragePlugin()).thenReturn(JaCoCoUtils.PLUGIN_KEY);

    assertThat(jacocoSettings.isEnabled(project)).isTrue();
  }

  @Test
  public void should_be_enabled_if_static_analysis_only() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn(Java.KEY);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    when(javaSettings.getEnabledCoveragePlugin()).thenReturn(JaCoCoUtils.PLUGIN_KEY);

    assertThat(jacocoSettings.isEnabled(project)).isFalse();
  }

  @Test
  public void plugin_should_be_disabled() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn(Java.KEY);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    when(javaSettings.getEnabledCoveragePlugin()).thenReturn("cobertura");

    assertThat(jacocoSettings.isEnabled(project)).isFalse();
  }

  @Test
  public void should_be_disabled_if_not_java() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn("flex");
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    when(javaSettings.getEnabledCoveragePlugin()).thenReturn(JaCoCoUtils.PLUGIN_KEY);

    assertThat(jacocoSettings.isEnabled(project)).isFalse();
  }

  @Test
  public void defaults() {
    assertThat(jacocoSettings.getReportPath()).isEqualTo("target/jacoco.exec");
    assertThat(jacocoSettings.getJvmArgument()).isEqualTo("-javaagent:jacocoagent.jar=destfile=target/jacoco.exec,excludes=*_javassist_*");

    assertThat(jacocoSettings.getItReportPath()).isNull();

    assertThat(jacocoSettings.getAntTargets()).isEqualTo(new String[]{});
  }

  @Test
  public void shouldReturnAntTargets() {
    settings.setProperty(JacocoConfiguration.ANT_TARGETS_PROPERTY, "test");
    assertThat(jacocoSettings.getAntTargets()).isEqualTo(new String[]{"test"});

    settings.setProperty(JacocoConfiguration.ANT_TARGETS_PROPERTY, "test1,test2");
    assertThat(jacocoSettings.getAntTargets()).isEqualTo(new String[]{"test1", "test2"});
  }

  @Test
  public void shouldReturnItReportPath() {
    settings.setProperty(JacocoConfiguration.IT_REPORT_PATH_PROPERTY, "target/it-jacoco.exec");

    assertThat(jacocoSettings.getItReportPath()).isEqualTo("target/it-jacoco.exec");
  }

  @Test
  public void shouldSetDestfile() {
    settings.setProperty(JacocoConfiguration.REPORT_PATH_PROPERTY, "jacoco.exec");

    assertThat(jacocoSettings.getReportPath()).isEqualTo("jacoco.exec");
    assertThat(jacocoSettings.getJvmArgument()).isEqualTo("-javaagent:jacocoagent.jar=destfile=jacoco.exec,excludes=*_javassist_*");
  }

  @Test
  public void shouldSetIncludesAndExcludes() {
    settings.setProperty(JacocoConfiguration.INCLUDES_PROPERTY, "org.sonar.*");
    settings.setProperty(JacocoConfiguration.EXCLUDES_PROPERTY, "org.sonar.api.*");
    settings.setProperty(JacocoConfiguration.EXCLCLASSLOADER_PROPERTY, "sun.reflect.DelegatingClassLoader");

    assertThat(jacocoSettings.getJvmArgument()).isEqualTo(
      "-javaagent:jacocoagent.jar=destfile=target/jacoco.exec,includes=org.sonar.*,excludes=org.sonar.api.*,exclclassloader=sun.reflect.DelegatingClassLoader"
    );
  }

}
