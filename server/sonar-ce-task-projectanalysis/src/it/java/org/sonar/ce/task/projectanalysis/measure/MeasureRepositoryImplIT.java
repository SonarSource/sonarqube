/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
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
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.StringValue;

import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newFileDto;

@RunWith(DataProviderRunner.class)
public class MeasureRepositoryImplIT {


  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

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
  private BatchReportReader mockBatchReportReader = mock(BatchReportReader.class);
  private MeasureRepositoryImpl underTestWithMock = new MeasureRepositoryImpl(mockedDbClient, mockBatchReportReader, metricRepository, reportMetricValidator);

  private DbSession dbSession = dbTester.getSession();

  @Before
  public void setUp() {
    when(metric1.getKey()).thenReturn(METRIC_KEY_1);
    when(metric1.getType()).thenReturn(Metric.MetricType.STRING);
    when(metric2.getKey()).thenReturn(METRIC_KEY_2);
    when(metric2.getType()).thenReturn(Metric.MetricType.STRING);

    // references to metrics are consistent with DB by design
    when(metricRepository.getByKey(METRIC_KEY_1)).thenReturn(metric1);
    when(metricRepository.getByKey(METRIC_KEY_2)).thenReturn(metric2);
  }

  @Test
  public void getBaseMeasure_throws_NPE_and_does_not_open_session_if_component_is_null() {
    try {
      underTestWithMock.getBaseMeasure(null, metric1);
      fail("an NPE should have been raised");
    } catch (NullPointerException e) {
      verifyNoInteractions(mockedDbClient);
    }
  }

  @Test
  public void getBaseMeasure_throws_NPE_and_does_not_open_session_if_metric_is_null() {
    try {
      underTestWithMock.getBaseMeasure(FILE_COMPONENT, null);
      fail("an NPE should have been raised");
    } catch (NullPointerException e) {
      verifyNoInteractions(mockedDbClient);
    }
  }

  @Test
  public void getBaseMeasure_returns_absent_if_measure_does_not_exist_in_DB() {
    Optional<Measure> res = underTest.getBaseMeasure(FILE_COMPONENT, metric1);

    assertThat(res).isNotPresent();
  }

  @Test
  public void getBaseMeasure_returns_Measure_if_measure_of_last_snapshot_only_in_DB() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    dbTester.components().insertComponent(newFileDto(project).setUuid(FILE_COMPONENT.getUuid()));
    SnapshotDto lastAnalysis = dbTester.components().insertSnapshot(project, t -> t.setLast(true));
    SnapshotDto oldAnalysis = dbTester.components().insertSnapshot(project, t -> t.setLast(false));
    MetricDto metric1 = dbTester.measures().insertMetric(t -> t.setValueType(org.sonar.api.measures.Metric.ValueType.STRING.name()));
    MetricDto metric2 = dbTester.measures().insertMetric(t -> t.setValueType(org.sonar.api.measures.Metric.ValueType.STRING.name()));
    dbClient.measureDao().insert(dbSession, createMeasureDto(metric1.getUuid(), FILE_COMPONENT.getUuid(), lastAnalysis.getUuid()));
    dbClient.measureDao().insert(dbSession, createMeasureDto(metric1.getUuid(), FILE_COMPONENT.getUuid(), oldAnalysis.getUuid()));
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

  @Test
  public void add_throws_NPE_if_Component_argument_is_null() {
    assertThatThrownBy(() -> underTest.add(null, metric1, SOME_MEASURE))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void add_throws_NPE_if_Component_metric_is_null() {
    assertThatThrownBy(() -> underTest.add(FILE_COMPONENT, null, SOME_MEASURE))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void add_throws_NPE_if_Component_measure_is_null() {
    assertThatThrownBy(() -> underTest.add(FILE_COMPONENT, metric1, null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void add_throws_UOE_if_measure_already_exists() {
    assertThatThrownBy(() -> {
      underTest.add(FILE_COMPONENT, metric1, SOME_MEASURE);
      underTest.add(FILE_COMPONENT, metric1, SOME_MEASURE);
    })
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void update_throws_NPE_if_Component_metric_is_null() {
    assertThatThrownBy(() -> underTest.update(FILE_COMPONENT, null, SOME_MEASURE))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void update_throws_NPE_if_Component_measure_is_null() {
    assertThatThrownBy(() -> underTest.update(FILE_COMPONENT, metric1, null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void update_throws_UOE_if_measure_does_not_exists() {
    assertThatThrownBy(() -> underTest.update(FILE_COMPONENT, metric1, SOME_MEASURE))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  private static final List<Measure> MEASURES = ImmutableList.of(
    Measure.newMeasureBuilder().create(1),
    Measure.newMeasureBuilder().create(1L),
    Measure.newMeasureBuilder().create(1d, 1),
    Measure.newMeasureBuilder().create(true),
    Measure.newMeasureBuilder().create(false),
    Measure.newMeasureBuilder().create("sds"),
    Measure.newMeasureBuilder().create(Measure.Level.OK),
    Measure.newMeasureBuilder().createNoValue());

  @DataProvider
  public static Object[][] measures() {
    return from(MEASURES).transform(new Function<Measure, Object[]>() {
      @Nullable
      @Override
      public Object[] apply(Measure input) {
        return new Measure[] {input};
      }
    }).toArray(Object[].class);
  }

  @Test
  public void add_accepts_NO_VALUE_as_measure_arg() {
    for (Metric.MetricType metricType : Metric.MetricType.values()) {
      underTest.add(FILE_COMPONENT, new MetricImpl("1", "key" + metricType, "name" + metricType, metricType), Measure.newMeasureBuilder().createNoValue());
    }
  }

  @Test
  @UseDataProvider("measures")
  public void update_throws_IAE_if_valueType_of_Measure_is_not_the_same_as_the_Metric_valueType_unless_NO_VALUE(Measure measure) {
    for (Metric.MetricType metricType : Metric.MetricType.values()) {
      if (metricType.getValueType() == measure.getValueType() || measure.getValueType() == Measure.ValueType.NO_VALUE) {
        continue;
      }

      try {
        final MetricImpl metric = new MetricImpl("1", "key" + metricType, "name" + metricType, metricType);
        underTest.add(FILE_COMPONENT, metric, getSomeMeasureByValueType(metricType));
        underTest.update(FILE_COMPONENT, metric, measure);
        fail("An IllegalArgumentException should have been raised");
      } catch (IllegalArgumentException e) {
        assertThat(e).hasMessage(format(
          "Measure's ValueType (%s) is not consistent with the Metric's ValueType (%s)",
          measure.getValueType(), metricType.getValueType()));
      }
    }
  }

  @Test
  public void update_accepts_NO_VALUE_as_measure_arg() {
    for (Metric.MetricType metricType : Metric.MetricType.values()) {
      MetricImpl metric = new MetricImpl("1", "key" + metricType, "name" + metricType, metricType);
      underTest.add(FILE_COMPONENT, metric, getSomeMeasureByValueType(metricType));
      underTest.update(FILE_COMPONENT, metric, Measure.newMeasureBuilder().createNoValue());
    }
  }

  private Measure getSomeMeasureByValueType(final Metric.MetricType metricType) {
    return MEASURES.stream().filter(input -> input.getValueType() == metricType.getValueType()).findFirst().get();
  }

  @Test
  public void update_supports_updating_to_the_same_value() {
    underTest.add(FILE_COMPONENT, metric1, SOME_MEASURE);
    underTest.update(FILE_COMPONENT, metric1, SOME_MEASURE);
  }

  @Test
  public void update_updates_the_stored_value() {
    Measure newMeasure = Measure.updatedMeasureBuilder(SOME_MEASURE).create();

    underTest.add(FILE_COMPONENT, metric1, SOME_MEASURE);
    underTest.update(FILE_COMPONENT, metric1, newMeasure);

    assertThat(underTest.getRawMeasure(FILE_COMPONENT, metric1)).containsSame(newMeasure);
  }

  @Test
  public void getRawMeasure_throws_NPE_without_reading_batch_report_if_component_arg_is_null() {
    try {
      underTestWithMock.getRawMeasure(null, metric1);
      fail("an NPE should have been raised");
    } catch (NullPointerException e) {
      verifyNoMoreInteractions(mockBatchReportReader);
    }
  }

  @Test
  public void getRawMeasure_throws_NPE_without_reading_batch_report_if_metric_arg_is_null() {
    try {
      underTestWithMock.getRawMeasure(FILE_COMPONENT, null);
      fail("an NPE should have been raised");
    } catch (NullPointerException e) {
      verifyNoMoreInteractions(mockBatchReportReader);
    }
  }

  @Test
  public void getRawMeasure_returns_measure_added_through_add_method() {
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
  public void getRawMeasure_returns_measure_from_batch_if_not_added_through_add_method() {
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
  public void getRawMeasure_returns_only_validate_measure_from_batch_if_not_added_through_add_method() {
    when(reportMetricValidator.validate(METRIC_KEY_1)).thenReturn(true);
    when(reportMetricValidator.validate(METRIC_KEY_2)).thenReturn(false);

    reportReader.putMeasures(FILE_COMPONENT.getReportAttributes().getRef(), ImmutableList.of(
      ScannerReport.Measure.newBuilder().setMetricKey(METRIC_KEY_1).setStringValue(StringValue.newBuilder().setValue("value1")).build(),
      ScannerReport.Measure.newBuilder().setMetricKey(METRIC_KEY_2).setStringValue(StringValue.newBuilder().setValue("value2")).build()));

    assertThat(underTest.getRawMeasure(FILE_COMPONENT, metric1)).isPresent();
    assertThat(underTest.getRawMeasure(FILE_COMPONENT, metric2)).isNotPresent();
  }

  @Test
  public void getRawMeasure_retrieves_added_measure_over_batch_measure() {
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
  public void getRawMeasure_retrieves_measure_from_batch_and_caches_it_locally_so_that_it_can_be_updated() {
    when(reportMetricValidator.validate(METRIC_KEY_1)).thenReturn(true);
    reportReader.putMeasures(FILE_COMPONENT.getReportAttributes().getRef(), ImmutableList.of(
      ScannerReport.Measure.newBuilder().setMetricKey(METRIC_KEY_1).setStringValue(StringValue.newBuilder().setValue("some value")).build()));

    Optional<Measure> measure = underTest.getRawMeasure(FILE_COMPONENT, metric1);

    underTest.update(FILE_COMPONENT, metric1, Measure.updatedMeasureBuilder(measure.get()).create());
  }

  @Test
  public void getRawMeasures_returns_added_measures_over_batch_measures() {
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

  private static MeasureDto createMeasureDto(String metricUuid, String componentUuid, String analysisUuid) {
    return new MeasureDto()
      .setComponentUuid(componentUuid)
      .setAnalysisUuid(analysisUuid)
      .setData(SOME_DATA)
      .setMetricUuid(metricUuid);
  }
}
