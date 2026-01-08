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

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.utils.System2;
import org.sonar.ce.common.scanner.ScannerReportReader;
import org.sonar.ce.common.scanner.ScannerReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.projectanalysis.metric.ReportMetricValidator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.ProjectMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.StringValue;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newFileDto;

class MeasureRepositoryImplIT {

  @RegisterExtension
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @RegisterExtension
  public ScannerReportReaderRule reportReader = new ScannerReportReaderRule();

  private static final String FILE_COMPONENT_KEY = "file cpt key";
  private static final ReportComponent FILE_COMPONENT = ReportComponent.builder(Component.Type.FILE, 1).setKey(FILE_COMPONENT_KEY).build();
  private static final ReportComponent OTHER_COMPONENT = ReportComponent.builder(Component.Type.FILE, 2).setKey("some other key").build();
  private static final String METRIC_KEY_1 = "metric 1";
  private static final int METRIC_ID_1 = 1;
  private static final String METRIC_KEY_2 = "metric 2";
  private static final int METRIC_ID_2 = 2;
  private final Metric metric1 = mock(Metric.class);
  private final Metric metric2 = mock(Metric.class);
  private static final String LAST_ANALYSIS_UUID = "u123";
  private static final String OTHER_ANALYSIS_UUID = "u369";
  private static final Measure SOME_MEASURE = Measure.newMeasureBuilder().create("some value");
  private static final String SOME_DATA = "some data";

  private ReportMetricValidator reportMetricValidator = mock(ReportMetricValidator.class);

  private DbClient dbClient = dbTester.getDbClient();
  private MetricRepository metricRepository = mock(MetricRepository.class);
  private MeasureRepositoryImpl underTest = new MeasureRepositoryImpl(dbClient, reportReader, metricRepository, reportMetricValidator);

  private DbClient mockedDbClient = mock(DbClient.class);
  private ScannerReportReader mockScannerReportReader = mock(ScannerReportReader.class);
  private MeasureRepositoryImpl underTestWithMock = new MeasureRepositoryImpl(mockedDbClient, mockScannerReportReader, metricRepository, reportMetricValidator);

  private DbSession dbSession = dbTester.getSession();

  @BeforeEach
  void setUp() {
    when(metric1.getKey()).thenReturn(METRIC_KEY_1);
    when(metric1.getType()).thenReturn(Metric.MetricType.STRING);
    when(metric2.getKey()).thenReturn(METRIC_KEY_2);
    when(metric2.getType()).thenReturn(Metric.MetricType.STRING);

    // references to metrics are consistent with DB by design
    when(metricRepository.getByKey(METRIC_KEY_1)).thenReturn(metric1);
    when(metricRepository.getByKey(METRIC_KEY_2)).thenReturn(metric2);
  }

  @Test
  void getBaseMeasureReturnsAbsentIfMeasureDoesNotExistInDB() {
    Optional<Measure> res = underTest.getBaseMeasure(FILE_COMPONENT, metric1);

    assertThat(res).isNotPresent();
  }

  @Test
  void getBaseMeasureReturnsMeasureIfMeasureOfLastSnapshotOnlyInDB() {
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    dbTester.components().insertComponent(newFileDto(project).setUuid(FILE_COMPONENT.getUuid()));
    SnapshotDto lastAnalysis = dbTester.components().insertSnapshot(project, t -> t.setLast(true));
    SnapshotDto oldAnalysis = dbTester.components().insertSnapshot(project, t -> t.setLast(false));
    MetricDto metric1 = dbTester.measures().insertMetric(t -> t.setValueType(org.sonar.api.measures.Metric.ValueType.STRING.name()));
    MetricDto metric2 = dbTester.measures().insertMetric(t -> t.setValueType(org.sonar.api.measures.Metric.ValueType.STRING.name()));
    dbClient.projectMeasureDao().insert(dbSession, createMeasureDto(metric1.getUuid(), FILE_COMPONENT.getUuid(), lastAnalysis.getUuid()));
    dbClient.projectMeasureDao().insert(dbSession, createMeasureDto(metric1.getUuid(), FILE_COMPONENT.getUuid(), oldAnalysis.getUuid()));
    dbSession.commit();

    // metric 1 is associated to snapshot with "last=true"
    assertThat(underTest.getBaseMeasure(FILE_COMPONENT, metricOf(metric1)).get().getStringValue())
      .isEqualTo(SOME_DATA);
    // metric 2 is associated to snapshot with "last=false" => not retrieved
    assertThat(underTest.getBaseMeasure(FILE_COMPONENT, metricOf(metric2))).isNotPresent();
  }

  private Metric metricOf(MetricDto metricDto) {
    Metric res = mock(Metric.class);
    when(res.getKey()).thenReturn(metricDto.getKey());
    when(res.getUuid()).thenReturn(metricDto.getUuid());
    when(res.getType()).thenReturn(Metric.MetricType.valueOf(metricDto.getValueType()));
    return res;
  }

  private static class AlertStatusTestSetup {
    final ComponentDto file;
    final Metric metric;

    AlertStatusTestSetup(ComponentDto file, Metric metric) {
      this.file = file;
      this.metric = metric;
    }
  }

  private AlertStatusTestSetup setupAlertStatusTest() {
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project).setUuid(FILE_COMPONENT.getUuid()));
    MetricDto alertStatusMetric = dbTester.measures().insertMetric(t -> t.setKey("alert_status").setValueType(org.sonar.api.measures.Metric.ValueType.LEVEL.name()));
    Metric metric = metricOf(alertStatusMetric);
    return new AlertStatusTestSetup(file, metric);
  }

  @Test
  void addThrowsUOEIfMeasureAlreadyExists() {
    assertThatThrownBy(() -> {
      underTest.add(FILE_COMPONENT, metric1, SOME_MEASURE);
      underTest.add(FILE_COMPONENT, metric1, SOME_MEASURE);
    })
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void updateThrowsUOEIfMeasureDoesNotExists() {
    assertThatThrownBy(() -> underTest.update(FILE_COMPONENT, metric1, SOME_MEASURE))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  private static final List<Measure> MEASURES = List.of(
    Measure.newMeasureBuilder().create(1),
    Measure.newMeasureBuilder().create(1L),
    Measure.newMeasureBuilder().create(1d, 1),
    Measure.newMeasureBuilder().create(true),
    Measure.newMeasureBuilder().create(false),
    Measure.newMeasureBuilder().create("sds"),
    Measure.newMeasureBuilder().create(Measure.Level.OK),
    Measure.newMeasureBuilder().createNoValue());

  static Stream<Measure> measures() {
    return MEASURES.stream();
  }

  @Test
  void addAcceptsNoValueAsMeasureArg() {
    for (Metric.MetricType metricType : Metric.MetricType.values()) {
      MetricImpl metric = new MetricImpl("1", "key" + metricType, "name" + metricType, metricType);
      Measure noValueMeasure = Measure.newMeasureBuilder().createNoValue();
      underTest.add(FILE_COMPONENT, metric, noValueMeasure);

      // Verify the measure was added
      Optional<Measure> result = underTest.getRawMeasure(FILE_COMPONENT, metric);
      assertThat(result).isPresent();
      assertThat(result.get().getValueType()).isEqualTo(ValueType.NO_VALUE);
    }
  }

  @ParameterizedTest
  @MethodSource("measures")
  void updateThrowsIAEIfValueTypeOfMeasureIsNotTheSameAsTheMetricValueTypeUnlessNoValue(Measure measure) {
    for (Metric.MetricType metricType : Metric.MetricType.values()) {
      if (metricType.getValueType() == measure.getValueType() || measure.getValueType() == ValueType.NO_VALUE) {
        continue;
      }

      final MetricImpl metric = new MetricImpl("1", "key" + metricType, "name" + metricType, metricType);
      underTest.add(FILE_COMPONENT, metric, getSomeMeasureByValueType(metricType));

      assertThatThrownBy(() -> underTest.update(FILE_COMPONENT, metric, measure))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(format(
          "Measure's ValueType (%s) is not consistent with the Metric's ValueType (%s)",
          measure.getValueType(), metricType.getValueType()));
    }
  }

  @Test
  void updateAcceptsNoValueAsMeasureArg() {
    for (Metric.MetricType metricType : Metric.MetricType.values()) {
      MetricImpl metric = new MetricImpl("1", "key" + metricType, "name" + metricType, metricType);
      underTest.add(FILE_COMPONENT, metric, getSomeMeasureByValueType(metricType));
      Measure noValueMeasure = Measure.newMeasureBuilder().createNoValue();
      underTest.update(FILE_COMPONENT, metric, noValueMeasure);

      // Verify the measure was updated to NO_VALUE
      Optional<Measure> result = underTest.getRawMeasure(FILE_COMPONENT, metric);
      assertThat(result).isPresent();
      assertThat(result.get().getValueType()).isEqualTo(ValueType.NO_VALUE);
    }
  }

  private Measure getSomeMeasureByValueType(final Metric.MetricType metricType) {
    return MEASURES.stream().filter(input -> input.getValueType() == metricType.getValueType()).findFirst().get();
  }

  @Test
  void updateSupportsUpdatingToTheSameValue() {
    underTest.add(FILE_COMPONENT, metric1, SOME_MEASURE);
    underTest.update(FILE_COMPONENT, metric1, SOME_MEASURE);

    // Verify the measure still exists with the same value
    Optional<Measure> result = underTest.getRawMeasure(FILE_COMPONENT, metric1);
    assertThat(result)
      .isPresent()
      .containsSame(SOME_MEASURE);
  }

  @Test
  void updateUpdatesTheStoredValue() {
    Measure newMeasure = Measure.updatedMeasureBuilder(SOME_MEASURE).create();

    underTest.add(FILE_COMPONENT, metric1, SOME_MEASURE);
    underTest.update(FILE_COMPONENT, metric1, newMeasure);

    assertThat(underTest.getRawMeasure(FILE_COMPONENT, metric1)).containsSame(newMeasure);
  }

  @Test
  void getRawMeasureReturnsMeasureAddedThroughAddMethod() {
    underTest.add(FILE_COMPONENT, metric1, SOME_MEASURE);

    Optional<Measure> res = underTest.getRawMeasure(FILE_COMPONENT, metric1);

    assertThat(res)
      .isPresent()
      .containsSame(SOME_MEASURE);

    // make sure we really match on the specified component and metric
    assertThat(underTest.getRawMeasure(OTHER_COMPONENT, metric1)).isNotPresent();
    assertThat(underTest.getRawMeasure(FILE_COMPONENT, metric2)).isNotPresent();
  }

  @Test
  void getRawMeasureReturnsMeasureFromBatchIfNotAddedThroughAddMethod() {
    String value = "trololo";

    when(reportMetricValidator.validate(METRIC_KEY_1)).thenReturn(true);

    reportReader.putMeasures(FILE_COMPONENT.getReportAttributes().getRef(), ImmutableList.of(
      ScannerReport.Measure.newBuilder().setMetricKey(METRIC_KEY_1).setStringValue(StringValue.newBuilder().setValue(value)).build()));

    Optional<Measure> res = underTest.getRawMeasure(FILE_COMPONENT, metric1);

    assertThat(res).isPresent();
    assertThat(res.get().getStringValue()).isEqualTo(value);

    // make sure we really match on the specified component and metric
    assertThat(underTest.getRawMeasure(FILE_COMPONENT, metric2)).isNotPresent();
    assertThat(underTest.getRawMeasure(OTHER_COMPONENT, metric1)).isNotPresent();
  }

  @Test
  void getRawMeasureReturnsOnlyValidateMeasureFromBatchIfNotAddedThroughAddMethod() {
    when(reportMetricValidator.validate(METRIC_KEY_1)).thenReturn(true);
    when(reportMetricValidator.validate(METRIC_KEY_2)).thenReturn(false);

    reportReader.putMeasures(FILE_COMPONENT.getReportAttributes().getRef(), ImmutableList.of(
      ScannerReport.Measure.newBuilder().setMetricKey(METRIC_KEY_1).setStringValue(StringValue.newBuilder().setValue("value1")).build(),
      ScannerReport.Measure.newBuilder().setMetricKey(METRIC_KEY_2).setStringValue(StringValue.newBuilder().setValue("value2")).build()));

    assertThat(underTest.getRawMeasure(FILE_COMPONENT, metric1)).isPresent();
    assertThat(underTest.getRawMeasure(FILE_COMPONENT, metric2)).isNotPresent();
  }

  @Test
  void getRawMeasureRetrievesAddedMeasureOverBatchMeasure() {
    when(reportMetricValidator.validate(METRIC_KEY_1)).thenReturn(true);
    reportReader.putMeasures(FILE_COMPONENT.getReportAttributes().getRef(), ImmutableList.of(
      ScannerReport.Measure.newBuilder().setMetricKey(METRIC_KEY_1).setStringValue(StringValue.newBuilder().setValue("some value")).build()));

    Measure addedMeasure = SOME_MEASURE;
    underTest.add(FILE_COMPONENT, metric1, addedMeasure);

    Optional<Measure> res = underTest.getRawMeasure(FILE_COMPONENT, metric1);

    assertThat(res)
      .isPresent()
      .containsSame(addedMeasure);
  }

  @Test
  void getRawMeasureRetrievesMeasureFromBatchAndCachesItLocallySoThatItCanBeUpdated() {
    when(reportMetricValidator.validate(METRIC_KEY_1)).thenReturn(true);
    reportReader.putMeasures(FILE_COMPONENT.getReportAttributes().getRef(), ImmutableList.of(
      ScannerReport.Measure.newBuilder().setMetricKey(METRIC_KEY_1).setStringValue(StringValue.newBuilder().setValue("some value")).build()));

    Optional<Measure> measure = underTest.getRawMeasure(FILE_COMPONENT, metric1);

    underTest.update(FILE_COMPONENT, metric1, Measure.updatedMeasureBuilder(measure.get()).create());
  }

  @Test
  void getRawMeasuresReturnsAddedMeasuresOverBatchMeasures() {
    when(reportMetricValidator.validate(METRIC_KEY_1)).thenReturn(true);
    when(reportMetricValidator.validate(METRIC_KEY_2)).thenReturn(true);
    ScannerReport.Measure batchMeasure1 = ScannerReport.Measure.newBuilder().setMetricKey(METRIC_KEY_1).setStringValue(StringValue.newBuilder().setValue("some value")).build();
    ScannerReport.Measure batchMeasure2 = ScannerReport.Measure.newBuilder().setMetricKey(METRIC_KEY_2).setStringValue(StringValue.newBuilder().setValue("some value")).build();
    reportReader.putMeasures(FILE_COMPONENT.getReportAttributes().getRef(), ImmutableList.of(batchMeasure1, batchMeasure2));

    Measure addedMeasure = SOME_MEASURE;
    underTest.add(FILE_COMPONENT, metric1, addedMeasure);

    Map<String, Measure> rawMeasures = underTest.getRawMeasures(FILE_COMPONENT);

    assertThat(rawMeasures.keySet()).hasSize(2);
    assertThat(rawMeasures).containsEntry(METRIC_KEY_1, addedMeasure);
    assertThat(rawMeasures.get(METRIC_KEY_2)).extracting(Measure::getStringValue).isEqualTo("some value");
  }

  @Test
  void getCurrentLiveMeasureSupportsAllMetricsNotJustAlertStatus() {
    // Now all metrics are supported, not just alert_status
    // Insert a measure for a non-alert_status metric
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project).setUuid(FILE_COMPONENT.getUuid()));
    MetricDto intMetric = dbTester.measures().insertMetric(t -> t.setKey("test_int_metric").setValueType(org.sonar.api.measures.Metric.ValueType.INT.name()));
    Metric metric = metricOf(intMetric);

    // Insert a measure in the MEASURES table
    dbTester.measures().insertMeasure(file, m -> m.addValue("test_int_metric", 42));

    Optional<Measure> result = underTest.getCurrentLiveMeasure(FILE_COMPONENT, metric);

    assertThat(result).isPresent();
    assertThat(result.get().getIntValue()).isEqualTo(42);
  }

  @Test
  void getCurrentLiveMeasureReturnsAbsentIfMeasureDoesNotExistInDb() {
    AlertStatusTestSetup setup = setupAlertStatusTest();

    // No measure inserted, so it should return empty after querying DB
    Optional<Measure> res = underTest.getCurrentLiveMeasure(FILE_COMPONENT, setup.metric);

    assertThat(res).isNotPresent();
  }

  @Test
  void getCurrentLiveMeasureReturnsQualityGateMeasureFromLiveMeasures() {
    AlertStatusTestSetup setup = setupAlertStatusTest();

    // Insert a measure in the MEASURES table (live measures)
    dbTester.measures().insertMeasure(setup.file, m -> m.addValue("alert_status", "OK"));

    Optional<Measure> result = underTest.getCurrentLiveMeasure(FILE_COMPONENT, setup.metric);

    assertThat(result).isPresent();
    assertThat(result.get().getQualityGateStatus().getStatus()).isEqualTo(Measure.Level.OK);
  }

  @Test
  void getCurrentLiveMeasureReturnsEmptyForInvalidQualityGateValue() {
    AlertStatusTestSetup setup = setupAlertStatusTest();

    // Insert a measure with invalid QG value
    dbTester.measures().insertMeasure(setup.file, m -> m.addValue("alert_status", "INVALID_VALUE"));

    Optional<Measure> result = underTest.getCurrentLiveMeasure(FILE_COMPONENT, setup.metric);

    assertThat(result).isNotPresent();
  }

  @Test
  void getCurrentLiveMeasureReturnsEmptyWhenMeasureValueIsNull() {
    AlertStatusTestSetup setup = setupAlertStatusTest();

    // Insert a measure with null value - addValue with null doesn't add the key
    dbTester.measures().insertMeasure(setup.file);

    Optional<Measure> result = underTest.getCurrentLiveMeasure(FILE_COMPONENT, setup.metric);

    assertThat(result).isNotPresent();
  }

  private static ProjectMeasureDto createMeasureDto(String metricUuid, String componentUuid, String analysisUuid) {
    return new ProjectMeasureDto()
      .setComponentUuid(componentUuid)
      .setAnalysisUuid(analysisUuid)
      .setData(SOME_DATA)
      .setMetricUuid(metricUuid);
  }

}
