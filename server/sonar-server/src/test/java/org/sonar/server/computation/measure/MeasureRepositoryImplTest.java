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
package org.sonar.server.computation.measure;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.debt.Characteristic;
import org.sonar.server.computation.debt.CharacteristicImpl;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.metric.ReportMetricValidator;

import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class MeasureRepositoryImplTest {

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

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
  private static final long LAST_SNAPSHOT_ID = 123;
  private static final long OTHER_SNAPSHOT_ID = 369;
  private static final long COMPONENT_ID = 567;
  private static final Measure SOME_MEASURE = Measure.newMeasureBuilder().create("some value");
  private static final String SOME_DATA = "some data";
  private static final RuleDto SOME_RULE = RuleDto.createFor(RuleKey.of("A", "1")).setId(963);
  private static final Characteristic SOME_CHARACTERISTIC = new CharacteristicImpl(741, "key", null);

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
      verifyZeroInteractions(mockedDbClient);
    }
  }

  @Test
  public void getBaseMeasure_throws_NPE_and_does_not_open_session_if_metric_is_null() {
    try {
      underTestWithMock.getBaseMeasure(FILE_COMPONENT, null);
      fail("an NPE should have been raised");
    } catch (NullPointerException e) {
      verifyZeroInteractions(mockedDbClient);
    }
  }

  @Test
  public void getBaseMeasure_returns_absent_if_measure_does_not_exist_in_DB() {
    Optional<Measure> res = underTest.getBaseMeasure(FILE_COMPONENT, metric1);

    assertThat(res).isAbsent();
  }

  @Test
  public void getBaseMeasure_returns_Measure_if_measure_of_last_snapshot_only_in_DB() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    dbClient.measureDao().insert(dbSession, createMeasureDto(METRIC_ID_1, LAST_SNAPSHOT_ID));
    dbClient.measureDao().insert(dbSession, createMeasureDto(METRIC_ID_2, OTHER_SNAPSHOT_ID));
    dbSession.commit();

    // metric 1 is associated to snapshot with "last=true"
    Optional<Measure> res = underTest.getBaseMeasure(FILE_COMPONENT, metric1);

    assertThat(res).isPresent();
    assertThat(res.get().getStringValue()).isEqualTo(SOME_DATA);

    // metric 2 is associated to snapshot with "last=false" => not retrieved
    res = underTest.getBaseMeasure(FILE_COMPONENT, metric2);

    assertThat(res).isAbsent();
  }

  @Test
  public void getBaseMeasure_does_not_return_measure_with_rule() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    dbClient.measureDao().insert(dbSession, createMeasureDto(METRIC_ID_1, LAST_SNAPSHOT_ID).setRuleId(10));
    dbSession.commit();

    assertThat(underTest.getBaseMeasure(FILE_COMPONENT, metric1)).isAbsent();
  }

  @Test
  public void getBaseMeasure_does_not_return_measure_with_characteristic() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    dbClient.measureDao().insert(dbSession, createMeasureDto(METRIC_ID_1, LAST_SNAPSHOT_ID).setCharacteristicId(100));
    dbSession.commit();

    assertThat(underTest.getBaseMeasure(FILE_COMPONENT, metric1)).isAbsent();
  }

  @Test
  public void add_throws_NPE_if_Component_argument_is_null() {
    expectedException.expect(NullPointerException.class);
    underTest.add(null, metric1, SOME_MEASURE);
  }

  @Test
  public void add_throws_NPE_if_Component_metric_is_null() {
    expectedException.expect(NullPointerException.class);
    underTest.add(FILE_COMPONENT, null, SOME_MEASURE);
  }

  @Test
  public void add_throws_NPE_if_Component_measure_is_null() {
    expectedException.expect(NullPointerException.class);
    underTest.add(FILE_COMPONENT, metric1, null);
  }

  @Test
  public void add_throws_UOE_if_measure_already_exists() {
    expectedException.expect(UnsupportedOperationException.class);
    underTest.add(FILE_COMPONENT, metric1, SOME_MEASURE);
    underTest.add(FILE_COMPONENT, metric1, SOME_MEASURE);
  }

  @Test
  public void update_throws_NPE_if_Component_argument_is_null() {
    expectedException.expect(NullPointerException.class);
    underTest.update(null, metric1, SOME_MEASURE);
  }

  @Test
  public void update_throws_NPE_if_Component_metric_is_null() {
    expectedException.expect(NullPointerException.class);
    underTest.update(FILE_COMPONENT, null, SOME_MEASURE);
  }

  @Test
  public void update_throws_NPE_if_Component_measure_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expect(NullPointerException.class);
    underTest.update(FILE_COMPONENT, metric1, null);
  }

  @Test
  public void update_throws_UOE_if_measure_does_not_exists() {
    expectedException.expect(UnsupportedOperationException.class);
    underTest.update(FILE_COMPONENT, metric1, SOME_MEASURE);
  }

  private static final List<Measure> MEASURES = ImmutableList.of(
    Measure.newMeasureBuilder().create(1),
    Measure.newMeasureBuilder().create(1l),
    Measure.newMeasureBuilder().create(1d, 1),
    Measure.newMeasureBuilder().create(true),
    Measure.newMeasureBuilder().create(false),
    Measure.newMeasureBuilder().create("sds"),
    Measure.newMeasureBuilder().create(Measure.Level.OK),
    Measure.newMeasureBuilder().createNoValue()
    );

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
      underTest.add(FILE_COMPONENT, new MetricImpl(1, "key" + metricType, "name" + metricType, metricType), Measure.newMeasureBuilder().createNoValue());
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
        final MetricImpl metric = new MetricImpl(1, "key" + metricType, "name" + metricType, metricType);
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
      MetricImpl metric = new MetricImpl(1, "key" + metricType, "name" + metricType, metricType);
      underTest.add(FILE_COMPONENT, metric, getSomeMeasureByValueType(metricType));
      underTest.update(FILE_COMPONENT, metric, Measure.newMeasureBuilder().createNoValue());
    }
  }

  private Measure getSomeMeasureByValueType(final Metric.MetricType metricType) {
    return from(MEASURES).filter(new Predicate<Measure>() {
      @Override
      public boolean apply(@Nullable Measure input) {
        return input.getValueType() == metricType.getValueType();
      }
    }).first().get();
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

    assertThat(underTest.getRawMeasure(FILE_COMPONENT, metric1).get()).isSameAs(newMeasure);
  }

  @Test
  public void update_updates_the_stored_value_for_rule() {
    Measure initialMeasure = Measure.newMeasureBuilder().forRule(123).createNoValue();
    Measure newMeasure = Measure.updatedMeasureBuilder(initialMeasure).create();

    underTest.add(FILE_COMPONENT, metric1, initialMeasure);
    underTest.update(FILE_COMPONENT, metric1, newMeasure);

    assertThat(underTest.getRawMeasures(FILE_COMPONENT).get(metric1.getKey()).iterator().next()).isSameAs(newMeasure);
  }

  @Test
  public void update_updates_the_stored_value_for_characteristic() {
    Measure initialMeasure = Measure.newMeasureBuilder().forCharacteristic(952).createNoValue();
    Measure newMeasure = Measure.updatedMeasureBuilder(initialMeasure).create();

    underTest.add(FILE_COMPONENT, metric1, initialMeasure);
    underTest.update(FILE_COMPONENT, metric1, newMeasure);

    assertThat(underTest.getRawMeasures(FILE_COMPONENT).get(metric1.getKey()).iterator().next()).isSameAs(newMeasure);
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

    assertThat(res).isPresent();
    assertThat(res.get()).isSameAs(SOME_MEASURE);

    // make sure we really match on the specified component and metric
    assertThat(underTest.getRawMeasure(OTHER_COMPONENT, metric1)).isAbsent();
    assertThat(underTest.getRawMeasure(FILE_COMPONENT, metric2)).isAbsent();
  }

  @Test
  public void getRawMeasure_returns_measure_from_batch_if_not_added_through_add_method() {
    String value = "trololo";

    when(reportMetricValidator.validate(METRIC_KEY_1)).thenReturn(true);

    reportReader.putMeasures(FILE_COMPONENT.getReportAttributes().getRef(), ImmutableList.of(
      BatchReport.Measure.newBuilder().setMetricKey(METRIC_KEY_1).setStringValue(value).build()
      ));

    Optional<Measure> res = underTest.getRawMeasure(FILE_COMPONENT, metric1);

    assertThat(res).isPresent();
    assertThat(res.get().getStringValue()).isEqualTo(value);

    // make sure we really match on the specified component and metric
    assertThat(underTest.getRawMeasure(FILE_COMPONENT, metric2)).isAbsent();
    assertThat(underTest.getRawMeasure(OTHER_COMPONENT, metric1)).isAbsent();
  }

  @Test
  public void getRawMeasure_returns_only_validate_measure_from_batch_if_not_added_through_add_method() {
    when(reportMetricValidator.validate(METRIC_KEY_1)).thenReturn(true);
    when(reportMetricValidator.validate(METRIC_KEY_2)).thenReturn(false);

    reportReader.putMeasures(FILE_COMPONENT.getReportAttributes().getRef(), ImmutableList.of(
      BatchReport.Measure.newBuilder().setMetricKey(METRIC_KEY_1).setStringValue("value1").build(),
      BatchReport.Measure.newBuilder().setMetricKey(METRIC_KEY_2).setStringValue("value2").build()
      ));

    assertThat(underTest.getRawMeasure(FILE_COMPONENT, metric1)).isPresent();
    assertThat(underTest.getRawMeasure(FILE_COMPONENT, metric2)).isAbsent();
  }

  @Test
  public void getRawMeasure_retrieves_added_measure_over_batch_measure() {
    when(reportMetricValidator.validate(METRIC_KEY_1)).thenReturn(true);
    reportReader.putMeasures(FILE_COMPONENT.getReportAttributes().getRef(), ImmutableList.of(
      BatchReport.Measure.newBuilder().setMetricKey(METRIC_KEY_1).setStringValue("some value").build()
      ));

    Measure addedMeasure = SOME_MEASURE;
    underTest.add(FILE_COMPONENT, metric1, addedMeasure);

    Optional<Measure> res = underTest.getRawMeasure(FILE_COMPONENT, metric1);

    assertThat(res).isPresent();
    assertThat(res.get()).isSameAs(addedMeasure);
  }

  @Test
  public void getRawMeasure_retrieves_measure_from_batch_and_caches_it_locally_so_that_it_can_be_updated() {
    when(reportMetricValidator.validate(METRIC_KEY_1)).thenReturn(true);
    reportReader.putMeasures(FILE_COMPONENT.getReportAttributes().getRef(), ImmutableList.of(
      BatchReport.Measure.newBuilder().setMetricKey(METRIC_KEY_1).setStringValue("some value").build()
      ));

    Optional<Measure> measure = underTest.getRawMeasure(FILE_COMPONENT, metric1);

    underTest.update(FILE_COMPONENT, metric1, Measure.updatedMeasureBuilder(measure.get()).create());
  }

  @Test
  public void getRawMeasures_for_metric_throws_NPE_if_Component_arg_is_null() {
    expectedException.expect(NullPointerException.class);
    underTest.getRawMeasures(null, metric1);
  }

  @Test
  public void getRawMeasures_for_metric_throws_NPE_if_Metric_arg_is_null() {
    expectedException.expect(NullPointerException.class);
    underTest.getRawMeasures(FILE_COMPONENT, null);
  }

  @Test
  public void getRawMeasures_for_metric_returns_empty_if_repository_is_empty() {
    assertThat(underTest.getRawMeasures(FILE_COMPONENT, metric1)).isEmpty();
  }

  @Test
  public void getRawMeasures_for_metric_returns_rule_measure() {
    Measure measure = Measure.newMeasureBuilder().forRule(SOME_RULE.getId()).createNoValue();

    underTest.add(FILE_COMPONENT, metric1, measure);

    Set<Measure> measures = underTest.getRawMeasures(FILE_COMPONENT, metric1);
    assertThat(measures).hasSize(1);
    assertThat(measures.iterator().next()).isSameAs(measure);
  }

  @Test
  public void getRawMeasures_for_measures_returns_characteristic_measure() {
    when(reportMetricValidator.validate(metric1.getKey())).thenReturn(true);
    Measure measure = Measure.newMeasureBuilder().forCharacteristic(SOME_CHARACTERISTIC.getId()).createNoValue();

    underTest.add(FILE_COMPONENT, metric1, measure);

    Set<Measure> measures = underTest.getRawMeasures(FILE_COMPONENT, metric1);
    assertThat(measures).hasSize(1);
    assertThat(measures.iterator().next()).isSameAs(measure);
  }

  @Test
  public void getRawMeasures_returns_added_measures_over_batch_measures() {
    when(reportMetricValidator.validate(METRIC_KEY_1)).thenReturn(true);
    when(reportMetricValidator.validate(METRIC_KEY_2)).thenReturn(true);
    BatchReport.Measure batchMeasure1 = BatchReport.Measure.newBuilder().setMetricKey(METRIC_KEY_1).setStringValue("some value").build();
    BatchReport.Measure batchMeasure2 = BatchReport.Measure.newBuilder().setMetricKey(METRIC_KEY_2).setStringValue("some value").build();
    reportReader.putMeasures(FILE_COMPONENT.getReportAttributes().getRef(), ImmutableList.of(batchMeasure1, batchMeasure2));

    Measure addedMeasure = SOME_MEASURE;
    underTest.add(FILE_COMPONENT, metric1, addedMeasure);
    Measure addedMeasure2 = Measure.newMeasureBuilder().forCharacteristic(SOME_CHARACTERISTIC.getId()).createNoValue();
    underTest.add(FILE_COMPONENT, metric1, addedMeasure2);

    SetMultimap<String, Measure> rawMeasures = underTest.getRawMeasures(FILE_COMPONENT);

    assertThat(rawMeasures.keySet()).hasSize(2);
    assertThat(rawMeasures.get(METRIC_KEY_1)).containsOnly(addedMeasure, addedMeasure2);
    assertThat(rawMeasures.get(METRIC_KEY_2)).containsOnly(Measure.newMeasureBuilder().create("some value"));
  }

  private static MeasureDto createMeasureDto(int metricId, long snapshotId) {
    return new MeasureDto()
      .setComponentId(COMPONENT_ID)
      .setSnapshotId(snapshotId)
      .setData(SOME_DATA)
      .setMetricId(metricId);
  }
}
