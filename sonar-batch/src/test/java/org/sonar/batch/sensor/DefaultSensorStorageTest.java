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
package org.sonar.batch.sensor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.issue.ModuleIssues;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.batch.sensor.coverage.CoverageExclusions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultSensorStorageTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ActiveRules activeRules;
  private DefaultFileSystem fs;
  private DefaultSensorStorage sensorStorage;
  private Settings settings;
  private ModuleIssues moduleIssues;
  private Project project;
  private MeasureCache measureCache;

  private BatchComponentCache resourceCache;

  @Before
  public void prepare() throws Exception {
    activeRules = new ActiveRulesBuilder().build();
    fs = new DefaultFileSystem(temp.newFolder().toPath());
    MetricFinder metricFinder = mock(MetricFinder.class);
    when(metricFinder.findByKey(CoreMetrics.NCLOC_KEY)).thenReturn(CoreMetrics.NCLOC);
    when(metricFinder.findByKey(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY)).thenReturn(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION);
    settings = new Settings();
    moduleIssues = mock(ModuleIssues.class);
    project = new Project("myProject");
    measureCache = mock(MeasureCache.class);
    CoverageExclusions coverageExclusions = mock(CoverageExclusions.class);
    when(coverageExclusions.accept(any(Resource.class), any(Measure.class))).thenReturn(true);
    resourceCache = new BatchComponentCache();
    sensorStorage = new DefaultSensorStorage(metricFinder,
      moduleIssues, settings, fs, activeRules, mock(DuplicationCache.class), coverageExclusions, resourceCache, mock(ReportPublisher.class), measureCache);
  }

  @Test
  public void shouldFailIfUnknowMetric() {
    InputFile file = new DefaultInputFile("foo", "src/Foo.php");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unknow metric with key: lines");

    sensorStorage.store(new DefaultMeasure()
      .on(file)
      .forMetric(CoreMetrics.LINES)
      .withValue(10));
  }

  @Test
  public void shouldSaveFileMeasureToSensorContext() {
    InputFile file = new DefaultInputFile("foo", "src/Foo.php");

    ArgumentCaptor<org.sonar.api.measures.Measure> argumentCaptor = ArgumentCaptor.forClass(org.sonar.api.measures.Measure.class);
    Resource sonarFile = File.create("src/Foo.php").setEffectiveKey("foo:src/Foo.php");
    resourceCache.add(sonarFile, null).setInputComponent(file);
    when(measureCache.put(eq(sonarFile), argumentCaptor.capture())).thenReturn(null);
    sensorStorage.store(new DefaultMeasure()
      .on(file)
      .forMetric(CoreMetrics.NCLOC)
      .withValue(10));

    org.sonar.api.measures.Measure m = argumentCaptor.getValue();
    assertThat(m.getValue()).isEqualTo(10.0);
    assertThat(m.getMetric()).isEqualTo(CoreMetrics.NCLOC);
  }

  @Test
  public void shouldSaveProjectMeasureToSensorContext() {
    DefaultInputModule module = new DefaultInputModule(project.getEffectiveKey());
    resourceCache.add(project, null).setInputComponent(module);

    ArgumentCaptor<org.sonar.api.measures.Measure> argumentCaptor = ArgumentCaptor.forClass(org.sonar.api.measures.Measure.class);
    when(measureCache.put(eq(project), argumentCaptor.capture())).thenReturn(null);

    sensorStorage.store(new DefaultMeasure()
      .on(module)
      .forMetric(CoreMetrics.NCLOC)
      .withValue(10));

    org.sonar.api.measures.Measure m = argumentCaptor.getValue();
    assertThat(m.getValue()).isEqualTo(10.0);
    assertThat(m.getMetric()).isEqualTo(CoreMetrics.NCLOC);
  }

}
