/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.measure;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.common.scanner.ScannerReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.projectanalysis.metric.ReportMetricValidator;
import org.sonar.db.DbClient;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;

public class MeasureRepositoryImplTest {

  private static final int FILE_REF = 1;
  private static final int TEST_FILE_REF = 2;
  private static final int DIRECTORY_REF = 3;

  @Rule
  public ScannerReportReaderRule reportReader = new ScannerReportReaderRule();

  private DbClient dbClient = mock(DbClient.class);
  private MetricRepository metricRepository = mock(MetricRepository.class);
  private ReportMetricValidator reportMetricValidator = mock(ReportMetricValidator.class);

  private MeasureRepositoryImpl underTest;

  private Metric nclocMetric;
  private Metric linesMetric;
  private Metric coverageMetric;
  private Metric complexityMetric;
  private Metric testsMetric;

  @Before
  public void setUp() {
    when(reportMetricValidator.validate(anyString())).thenReturn(true);

    nclocMetric = createMetric(CoreMetrics.NCLOC_KEY, Metric.MetricType.INT);
    linesMetric = createMetric(CoreMetrics.LINES_KEY, Metric.MetricType.INT);
    coverageMetric = createMetric(CoreMetrics.COVERAGE_KEY, Metric.MetricType.PERCENT);
    complexityMetric = createMetric(CoreMetrics.COMPLEXITY_KEY, Metric.MetricType.INT);
    testsMetric = createMetric(CoreMetrics.TESTS_KEY, Metric.MetricType.INT);

    when(metricRepository.getByKey(CoreMetrics.NCLOC_KEY)).thenReturn(nclocMetric);
    when(metricRepository.getByKey(CoreMetrics.LINES_KEY)).thenReturn(linesMetric);
    when(metricRepository.getByKey(CoreMetrics.COVERAGE_KEY)).thenReturn(coverageMetric);
    when(metricRepository.getByKey(CoreMetrics.COMPLEXITY_KEY)).thenReturn(complexityMetric);
    when(metricRepository.getByKey(CoreMetrics.TESTS_KEY)).thenReturn(testsMetric);

    underTest = new MeasureRepositoryImpl(dbClient, reportReader, metricRepository, reportMetricValidator);
  }

  @Test
  public void should_filter_ncloc_from_test_files() {
    Component testFile = createTestFile(TEST_FILE_REF);
    reportReader.putMeasures(TEST_FILE_REF, List.of(
      createMeasure(CoreMetrics.NCLOC_KEY, 22),
      createMeasure(CoreMetrics.TESTS_KEY, 2)
    ));

    Optional<Measure> nclocMeasure = underTest.getRawMeasure(testFile, nclocMetric);
    Optional<Measure> testsMeasure = underTest.getRawMeasure(testFile, testsMetric);

    assertThat(nclocMeasure).isEmpty();
    assertThat(testsMeasure).isPresent();
  }

  @Test
  public void should_not_filter_ncloc_from_main_files() {
    Component mainFile = createMainFile(FILE_REF);
    reportReader.putMeasures(FILE_REF, List.of(
      createMeasure(CoreMetrics.NCLOC_KEY, 13)
    ));

    Optional<Measure> nclocMeasure = underTest.getRawMeasure(mainFile, nclocMetric);

    assertThat(nclocMeasure).isPresent();
    assertThat(nclocMeasure.get().getIntValue()).isEqualTo(13);
  }

  @Test
  public void should_not_filter_lines_from_test_files() {
    Component testFile = createTestFile(TEST_FILE_REF);
    reportReader.putMeasures(TEST_FILE_REF, List.of(
      createMeasure(CoreMetrics.LINES_KEY, 30),
      createMeasure(CoreMetrics.NCLOC_KEY, 22)
    ));

    Optional<Measure> linesMeasure = underTest.getRawMeasure(testFile, linesMetric);

    assertThat(linesMeasure).isPresent();
    assertThat(linesMeasure.get().getIntValue()).isEqualTo(30);
  }

  @Test
  public void should_not_filter_other_metrics_from_test_files() {
    Component testFile = createTestFile(TEST_FILE_REF);
    reportReader.putMeasures(TEST_FILE_REF, List.of(
      createMeasure(CoreMetrics.COVERAGE_KEY, 80.5),
      createMeasure(CoreMetrics.COMPLEXITY_KEY, 10),
      createMeasure(CoreMetrics.TESTS_KEY, 2)
    ));

    Optional<Measure> coverageMeasure = underTest.getRawMeasure(testFile, coverageMetric);
    Optional<Measure> complexityMeasure = underTest.getRawMeasure(testFile, complexityMetric);
    Optional<Measure> testsMeasure = underTest.getRawMeasure(testFile, testsMetric);

    assertThat(coverageMeasure).isPresent();
    assertThat(complexityMeasure).isPresent();
    assertThat(testsMeasure).isPresent();
  }

  @Test
  public void should_not_filter_measures_from_directory_components() {
    Component directory = ReportComponent.builder(DIRECTORY, DIRECTORY_REF)
      .build();
    reportReader.putMeasures(DIRECTORY_REF, List.of(
      createMeasure(CoreMetrics.NCLOC_KEY, 100)
    ));

    Optional<Measure> nclocMeasure = underTest.getRawMeasure(directory, nclocMetric);

    assertThat(nclocMeasure).isPresent();
    assertThat(nclocMeasure.get().getIntValue()).isEqualTo(100);
  }

  @Test
  public void getRawMeasures_should_return_filtered_map_for_test_files() {
    Component testFile = createTestFile(TEST_FILE_REF);
    reportReader.putMeasures(TEST_FILE_REF, List.of(
      createMeasure(CoreMetrics.NCLOC_KEY, 22),
      createMeasure(CoreMetrics.LINES_KEY, 30),
      createMeasure(CoreMetrics.TESTS_KEY, 2)
    ));

    Map<String, Measure> measures = underTest.getRawMeasures(testFile);

    assertThat(measures)
      .doesNotContainKey(CoreMetrics.NCLOC_KEY)
      .containsKeys(CoreMetrics.LINES_KEY, CoreMetrics.TESTS_KEY);
  }

  @Test
  public void getRawMeasures_should_not_filter_for_main_files() {
    Component mainFile = createMainFile(FILE_REF);
    reportReader.putMeasures(FILE_REF, List.of(
      createMeasure(CoreMetrics.NCLOC_KEY, 13),
      createMeasure(CoreMetrics.LINES_KEY, 20)
    ));

    Map<String, Measure> measures = underTest.getRawMeasures(mainFile);

    assertThat(measures).containsKeys(CoreMetrics.NCLOC_KEY, CoreMetrics.LINES_KEY);
  }

  private Component createMainFile(int ref) {
    return ReportComponent.builder(FILE, ref)
      .setFileAttributes(new FileAttributes(false, "xoo", 1))
      .build();
  }

  private Component createTestFile(int ref) {
    return ReportComponent.builder(FILE, ref)
      .setFileAttributes(new FileAttributes(true, "xoo", 1))
      .build();
  }

  private Metric createMetric(String key, Metric.MetricType type) {
    return new MetricImpl("uuid-" + key, key, key + " name", type);
  }

  private ScannerReport.Measure createMeasure(String metricKey, int value) {
    return ScannerReport.Measure.newBuilder()
      .setMetricKey(metricKey)
      .setIntValue(ScannerReport.Measure.IntValue.newBuilder().setValue(value).build())
      .build();
  }

  private ScannerReport.Measure createMeasure(String metricKey, double value) {
    return ScannerReport.Measure.newBuilder()
      .setMetricKey(metricKey)
      .setDoubleValue(ScannerReport.Measure.DoubleValue.newBuilder().setValue(value).build())
      .build();
  }

}
