/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.sensor;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.metric.ScannerMetrics;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;
import org.sonar.scanner.issue.ModuleIssues;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.repository.ContextPropertiesCache;
import org.sonar.scanner.scan.measure.MeasureCache;
import org.sonar.scanner.sensor.coverage.CoverageExclusions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultSensorStorageTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DefaultSensorStorage underTest;
  private MapSettings settings;
  private ModuleIssues moduleIssues;
  private MeasureCache measureCache;
  private ContextPropertiesCache contextPropertiesCache = new ContextPropertiesCache();

  @Before
  public void prepare() throws Exception {
    MetricFinder metricFinder = mock(MetricFinder.class);
    when(metricFinder.<Integer>findByKey(CoreMetrics.NCLOC_KEY)).thenReturn(CoreMetrics.NCLOC);
    when(metricFinder.<String>findByKey(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY)).thenReturn(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION);
    settings = new MapSettings();
    moduleIssues = mock(ModuleIssues.class);
    measureCache = mock(MeasureCache.class);
    CoverageExclusions coverageExclusions = mock(CoverageExclusions.class);
    when(coverageExclusions.isExcluded(any(InputFile.class))).thenReturn(false);
    ReportPublisher reportPublisher = mock(ReportPublisher.class);
    when(reportPublisher.getWriter()).thenReturn(new ScannerReportWriter(temp.newFolder()));
    underTest = new DefaultSensorStorage(metricFinder,
      moduleIssues, settings.asConfig(), coverageExclusions, reportPublisher, measureCache,
      mock(SonarCpdBlockIndex.class), contextPropertiesCache, new ScannerMetrics());
  }

  @Test
  public void shouldFailIfUnknownMetric() {
    InputFile file = new TestInputFileBuilder("foo", "src/Foo.php").build();

    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("Unknown metric: lines");

    underTest.store(new DefaultMeasure()
      .on(file)
      .forMetric(CoreMetrics.LINES)
      .withValue(10));
  }

  @Test
  public void shouldSaveFileMeasureToSensorContext() {
    InputFile file = new TestInputFileBuilder("foo", "src/Foo.php").build();

    ArgumentCaptor<DefaultMeasure> argumentCaptor = ArgumentCaptor.forClass(DefaultMeasure.class);
    when(measureCache.put(eq(file.key()), eq(CoreMetrics.NCLOC_KEY), argumentCaptor.capture())).thenReturn(null);
    underTest.store(new DefaultMeasure()
      .on(file)
      .forMetric(CoreMetrics.NCLOC)
      .withValue(10));

    DefaultMeasure m = argumentCaptor.getValue();
    assertThat(m.value()).isEqualTo(10);
    assertThat(m.metric()).isEqualTo(CoreMetrics.NCLOC);
  }

  @Test
  public void shouldSaveProjectMeasureToSensorContext() {
    String projectKey = "myProject";
    DefaultInputModule module = new DefaultInputModule(projectKey);

    ArgumentCaptor<DefaultMeasure> argumentCaptor = ArgumentCaptor.forClass(DefaultMeasure.class);
    when(measureCache.put(eq(module.key()), eq(CoreMetrics.NCLOC_KEY), argumentCaptor.capture())).thenReturn(null);

    underTest.store(new DefaultMeasure()
      .on(module)
      .forMetric(CoreMetrics.NCLOC)
      .withValue(10));

    DefaultMeasure m = argumentCaptor.getValue();
    assertThat(m.value()).isEqualTo(10);
    assertThat(m.metric()).isEqualTo(CoreMetrics.NCLOC);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void duplicateHighlighting() throws Exception {
    InputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.java")
      .setModuleBaseDir(temp.newFolder().toPath()).build();
    DefaultHighlighting h = new DefaultHighlighting(null)
      .onFile(inputFile);
    underTest.store(h);
    underTest.store(h);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void duplicateSymbolTable() throws Exception {
    InputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.java")
      .setModuleBaseDir(temp.newFolder().toPath()).build();
    DefaultSymbolTable st = new DefaultSymbolTable(null)
      .onFile(inputFile);
    underTest.store(st);
    underTest.store(st);
  }

  @Test
  public void shouldStoreContextProperty() {
    underTest.storeProperty("foo", "bar");
    assertThat(contextPropertiesCache.getAll()).containsOnly(entry("foo", "bar"));
  }

  @Test
  public void shouldValidateStrictlyPositiveLine() throws Exception {
    InputFile file = new TestInputFileBuilder("module", "testfile").setModuleBaseDir(temp.newFolder().toPath()).build();
    Map<Integer, Integer> map = ImmutableMap.of(0, 3);
    String data = KeyValueFormat.format(map);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("must be > 0");
    underTest.validateCoverageMeasure(data, file);
  }

  @Test
  public void shouldValidateMaxLine() throws Exception {
    InputFile file = new TestInputFileBuilder("module", "testfile").setModuleBaseDir(temp.newFolder().toPath()).build();
    Map<Integer, Integer> map = ImmutableMap.of(11, 3);
    String data = KeyValueFormat.format(map);

    thrown.expect(IllegalStateException.class);
    underTest.validateCoverageMeasure(data, file);
  }

  @Test
  public void mergeCoverageLineMetrics_should_be_sorted() {
    assertThat(DefaultSensorStorage.mergeCoverageLineMetric(CoreMetrics.COVERAGE_LINE_HITS_DATA, "1=1", "1=1")).isEqualTo("1=2");
    assertThat(DefaultSensorStorage.mergeCoverageLineMetric(CoreMetrics.COVERAGE_LINE_HITS_DATA, "1=1", "2=1")).isEqualTo("1=1;2=1");
    assertThat(DefaultSensorStorage.mergeCoverageLineMetric(CoreMetrics.COVERAGE_LINE_HITS_DATA, "2=1", "1=1")).isEqualTo("1=1;2=1");

    assertThat(DefaultSensorStorage.mergeCoverageLineMetric(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "1=1", "1=1")).isEqualTo("1=1");
    assertThat(DefaultSensorStorage.mergeCoverageLineMetric(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "1=1", "2=1")).isEqualTo("1=1;2=1");
    assertThat(DefaultSensorStorage.mergeCoverageLineMetric(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "2=1", "1=1")).isEqualTo("1=1;2=1");
  }

}
