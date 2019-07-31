/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.projectanalysis.metric.ReportMetricValidator;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDto;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class MapBasedRawMeasureRepositoryTest {
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String FILE_COMPONENT_KEY = "file cpt key";
  private static final ReportComponent FILE_COMPONENT = ReportComponent.builder(Component.Type.FILE, 1)
    .setKey(FILE_COMPONENT_KEY)
    .setUuid("1")
    .build();
  private static final ReportComponent OTHER_COMPONENT = ReportComponent.builder(Component.Type.FILE, 2)
    .setKey("some other key")
    .setUuid("2")
    .build();

  private static final String METRIC_KEY_1 = "metric 1";
  private static final String METRIC_KEY_2 = "metric 2";
  private final Metric metric1 = mock(Metric.class);
  private final Metric metric2 = mock(Metric.class);

  private static final Measure SOME_MEASURE = Measure.newMeasureBuilder().create("some value");

  private static final RuleDto SOME_RULE = RuleDto.createFor(RuleKey.of("A", "1")).setId(963);

  private ReportMetricValidator reportMetricValidator = mock(ReportMetricValidator.class);

  private MetricRepository metricRepository = mock(MetricRepository.class);
  private MapBasedRawMeasureRepository<Integer> underTest = new MapBasedRawMeasureRepository<>(component -> component.getReportAttributes().getRef());
  private DbClient mockedDbClient = mock(DbClient.class);
  private BatchReportReader mockBatchReportReader = mock(BatchReportReader.class);
  private MeasureRepositoryImpl underTestWithMock = new MeasureRepositoryImpl(mockedDbClient, mockBatchReportReader, metricRepository, reportMetricValidator);

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

  @Test(expected = NullPointerException.class)
  public void add_throws_NPE_if_Component_argument_is_null() {
    underTest.add(null, metric1, SOME_MEASURE);
  }

  @Test(expected = NullPointerException.class)
  public void add_throws_NPE_if_Component_metric_is_null() {
    underTest.add(FILE_COMPONENT, null, SOME_MEASURE);
  }

  @Test(expected = NullPointerException.class)
  public void add_throws_NPE_if_Component_measure_is_null() {
    underTest.add(FILE_COMPONENT, metric1, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void add_throws_UOE_if_measure_already_exists() {
    underTest.add(FILE_COMPONENT, metric1, SOME_MEASURE);
    underTest.add(FILE_COMPONENT, metric1, SOME_MEASURE);
  }

  @Test(expected = NullPointerException.class)
  public void update_throws_NPE_if_Component_argument_is_null() {
    underTest.update(null, metric1, SOME_MEASURE);
  }

  @Test(expected = NullPointerException.class)
  public void update_throws_NPE_if_Component_metric_is_null() {
    underTest.update(FILE_COMPONENT, null, SOME_MEASURE);
  }

  @Test(expected = NullPointerException.class)
  public void update_throws_NPE_if_Component_measure_is_null() {
    underTest.update(FILE_COMPONENT, metric1, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void update_throws_UOE_if_measure_does_not_exists() {
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
    Measure.newMeasureBuilder().createNoValue());

  @DataProvider
  public static Object[][] measures() {
    return MEASURES.stream()
      .map(c -> new Measure[] {c})
      .toArray(i -> new Object[i][]);
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
    return MEASURES.stream()
      .filter(measure -> measure.getValueType() == metricType.getValueType())
      .findFirst().get();
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
    assertThat(underTest.getRawMeasure(OTHER_COMPONENT, metric1)).isNotPresent();
    assertThat(underTest.getRawMeasure(FILE_COMPONENT, metric2)).isNotPresent();
  }
}
