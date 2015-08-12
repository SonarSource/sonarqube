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
package org.sonar.batch.rule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.test.IsMeasure;
import org.sonar.core.util.UtcDateUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QProfileSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  static final Date DATE = UtcDateUtils.parseDateTime("2014-01-15T12:00:00+0000");
  static final QProfile JAVA_PROFILE = new QProfile().setKey("java-two").setName("Java Two").setLanguage("java")
    .setRulesUpdatedAt(DATE);
  static final QProfile PHP_PROFILE = new QProfile().setKey("php-one").setName("Php One").setLanguage("php")
    .setRulesUpdatedAt(DATE);

  ModuleQProfiles moduleQProfiles = mock(ModuleQProfiles.class);
  Project project = mock(Project.class);
  SensorContext sensorContext = mock(SensorContext.class);
  DefaultFileSystem fs;

  @Before
  public void prepare() throws Exception {
    fs = new DefaultFileSystem(temp.newFolder().toPath());
  }

  @Test
  public void to_string() {
    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, fs, mock(AnalysisMode.class));
    assertThat(sensor.toString()).isEqualTo("QProfileSensor");
  }

  @Test
  public void no_execution_in_issues_mode() {
    AnalysisMode analysisMode = mock(AnalysisMode.class);
    when(analysisMode.isIssues()).thenReturn(true);
    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, fs, analysisMode);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

  }

  @Test
  public void no_qprofiles() {
    when(moduleQProfiles.findAll()).thenReturn(Collections.<QProfile>emptyList());

    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, fs, mock(AnalysisMode.class));
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    sensor.analyse(project, sensorContext);

    // measures are not saved
    verify(sensorContext).saveMeasure(argThat(new IsMeasure(CoreMetrics.QUALITY_PROFILES, "[]")));
  }

  @Test
  public void mark_profiles_as_used() {
    when(moduleQProfiles.findByLanguage("java")).thenReturn(JAVA_PROFILE);
    when(moduleQProfiles.findByLanguage("php")).thenReturn(PHP_PROFILE);
    when(moduleQProfiles.findByLanguage("abap")).thenReturn(null);
    fs.addLanguages("java", "php", "abap");

    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, fs, mock(AnalysisMode.class));
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    sensor.analyse(project, sensorContext);
  }

  @Test
  public void store_measures_on_single_lang_module() {
    when(moduleQProfiles.findByLanguage("java")).thenReturn(JAVA_PROFILE);
    when(moduleQProfiles.findByLanguage("php")).thenReturn(PHP_PROFILE);
    when(moduleQProfiles.findByLanguage("abap")).thenReturn(null);
    fs.addLanguages("java");

    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, fs, mock(AnalysisMode.class));
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    sensor.analyse(project, sensorContext);

    verify(sensorContext).saveMeasure(
      argThat(new IsMeasure(CoreMetrics.QUALITY_PROFILES,
        "[{\"key\":\"java-two\",\"language\":\"java\",\"name\":\"Java Two\",\"rulesUpdatedAt\":\"2014-01-15T12:00:00+0000\"}]")));
  }

  @Test
  public void store_measures_on_multi_lang_module() {
    when(moduleQProfiles.findByLanguage("java")).thenReturn(JAVA_PROFILE);
    when(moduleQProfiles.findByLanguage("php")).thenReturn(PHP_PROFILE);
    when(moduleQProfiles.findByLanguage("abap")).thenReturn(null);
    fs.addLanguages("java", "php");

    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, fs, mock(AnalysisMode.class));
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    sensor.analyse(project, sensorContext);

    verify(sensorContext).saveMeasure(
      argThat(new IsMeasure(CoreMetrics.QUALITY_PROFILES,
        "[{\"key\":\"java-two\",\"language\":\"java\",\"name\":\"Java Two\",\"rulesUpdatedAt\":\"2014-01-15T12:00:00+0000\"}," +
          "{\"key\":\"php-one\",\"language\":\"php\",\"name\":\"Php One\",\"rulesUpdatedAt\":\"2014-01-15T12:00:00+0000\"}]")));
  }
}
