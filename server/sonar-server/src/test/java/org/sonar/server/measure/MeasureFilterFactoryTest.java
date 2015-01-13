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
package org.sonar.server.measure;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MeasureFilterFactoryTest {

  System2 system = mock(System2.class);

  @Test
  public void sort_on_measure_value() {
    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    Map<String, Object> props = ImmutableMap.<String, Object>of("sort", "metric:ncloc");
    MeasureFilter filter = factory.create(props);

    assertThat(filter.sort().column()).isEqualTo("pmsort.value");
    assertThat(filter.sort().metric().getKey()).isEqualTo("ncloc");
    assertThat(filter.sort().period()).isNull();
  }

  @Test
  public void sort_on_measure_variation() {
    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    Map<String, Object> props = ImmutableMap.<String, Object>of("sort", "metric:ncloc:3");
    MeasureFilter filter = factory.create(props);

    assertThat(filter.sort().column()).isEqualTo("pmsort.variation_value_3");
    assertThat(filter.sort().metric().getKey()).isEqualTo("ncloc");
    assertThat(filter.sort().period()).isEqualTo(3);
  }

  @Test
  public void sort_on_name() {
    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    Map<String, Object> props = ImmutableMap.<String, Object>of("sort", "name");
    MeasureFilter filter = factory.create(props);

    assertThat(filter.sort().column()).isEqualTo("p.long_name");
    assertThat(filter.sort().metric()).isNull();
    assertThat(filter.sort().period()).isNull();
  }

  @Test
  public void fallback_on_name_sort_when_metric_is_unknown() {
    MetricFinder finder = mock(MetricFinder.class);
    when(finder.findByKey(anyString())).thenReturn(null);
    MeasureFilterFactory factory = new MeasureFilterFactory(finder, system);
    Map<String, Object> props = ImmutableMap.<String, Object>of("sort", "metric:sqale_index");
    MeasureFilter filter = factory.create(props);

    assertThat(filter.sort().column()).isEqualTo("p.long_name");
    assertThat(filter.sort().metric()).isNull();
    assertThat(filter.sort().period()).isNull();
    assertThat(filter.sort().isAsc()).isTrue();
  }

  @Test
  public void fallback_on_name_sort_when_sort_is_unknown() {
    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    Map<String, Object> props = ImmutableMap.<String, Object>of("sort", "unknown");
    MeasureFilter filter = factory.create(props);

    assertThat(filter.sort().column()).isEqualTo("p.long_name");
    assertThat(filter.sort().metric()).isNull();
    assertThat(filter.sort().period()).isNull();
    assertThat(filter.sort().isAsc()).isTrue();
  }

  @Test
  public void descending_sort() {
    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    Map<String, Object> props = ImmutableMap.<String, Object>of("asc", "false");
    MeasureFilter filter = factory.create(props);

    assertThat(filter.sort().column()).isEqualTo("p.long_name");
    assertThat(filter.sort().metric()).isNull();
    assertThat(filter.sort().period()).isNull();
    assertThat(filter.sort().isAsc()).isFalse();
  }

  @Test
  public void ascending_sort_by_default() {
    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    Map<String, Object> props = Maps.newHashMap();
    MeasureFilter filter = factory.create(props);

    assertThat(filter.sort().column()).isEqualTo("p.long_name");
    assertThat(filter.sort().metric()).isNull();
    assertThat(filter.sort().period()).isNull();
    assertThat(filter.sort().isAsc()).isTrue();
  }

  @Test
  public void date_conditions() {
    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    Map<String, Object> props = ImmutableMap.<String, Object>of(
      "fromDate", "2012-01-25",
      "toDate", "2012-02-18"
      );
    MeasureFilter filter = factory.create(props);

    assertThat(DateUtils.formatDate(filter.getFromDate())).isEqualTo("2012-01-25");
    assertThat(DateUtils.formatDate(filter.getToDate())).isEqualTo("2012-02-18");
  }

  @Test
  public void age_conditions() {
    long today = DateUtils.parseDateTime("2013-05-18T11:00:00+0000").getTime();
    when(system.now()).thenReturn(today);

    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    Map<String, Object> props = ImmutableMap.<String, Object>of(
      "ageMaxDays", "3",
      "ageMinDays", "1"
      );
    MeasureFilter filter = factory.create(props);
    assertThat(DateUtils.formatDate(filter.getFromDate())).isEqualTo("2013-05-15");
    assertThat(DateUtils.formatDate(filter.getToDate())).isEqualTo("2013-05-17");
  }

  @Test
  public void measure_value_condition() {
    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    Map<String, Object> props = ImmutableMap.<String, Object>of(
      "c1_metric", "complexity",
      "c1_op", "gte",
      "c1_val", "3.14"
      );
    MeasureFilter filter = factory.create(props);

    List<MeasureFilterCondition> conditions = filter.getMeasureConditions();
    assertThat(conditions).hasSize(1);
    assertThat(conditions.get(0).metric().getKey()).isEqualTo("complexity");
    assertThat(conditions.get(0).operator()).isEqualTo(MeasureFilterCondition.Operator.GREATER_OR_EQUALS);
    assertThat(conditions.get(0).value()).isEqualTo(3.14);
    assertThat(conditions.get(0).period()).isNull();
  }

  @Test
  public void measure_variation_condition() {
    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    Map<String, Object> props = ImmutableMap.<String, Object>of(
      "c1_metric", "complexity",
      "c1_op", "gte",
      "c1_val", "3.14",
      "c1_period", "3"
      );
    MeasureFilter filter = factory.create(props);

    List<MeasureFilterCondition> conditions = filter.getMeasureConditions();
    assertThat(conditions).hasSize(1);
    assertThat(conditions.get(0).metric().getKey()).isEqualTo("complexity");
    assertThat(conditions.get(0).operator()).isEqualTo(MeasureFilterCondition.Operator.GREATER_OR_EQUALS);
    assertThat(conditions.get(0).value()).isEqualTo(3.14);
    assertThat(conditions.get(0).period()).isEqualTo(3);
  }

  @Test
  public void alert_level_condition() {
    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    MeasureFilter filter = factory.create(ImmutableMap.<String, Object>of(
      "alertLevels", Arrays.asList("error", "warn", "unknown")
      ));

    List<MeasureFilterCondition> conditions = filter.getMeasureConditions();
    assertThat(conditions).hasSize(1);
    assertThat(conditions.get(0).metric().getKey()).isEqualTo("alert_status");
    assertThat(conditions.get(0).operator()).isEqualTo(MeasureFilterCondition.Operator.IN);
    assertThat(conditions.get(0).value()).isEqualTo(0);
    assertThat(conditions.get(0).textValue()).isEqualTo("('ERROR', 'WARN')");
    assertThat(conditions.get(0).period()).isNull();
  }

  @Test
  public void alert_level_condition_with_no_alert_status_metric() {
    MeasureFilterFactory factory = new MeasureFilterFactory(mock(MetricFinder.class), system);
    MeasureFilter filter = factory.create(ImmutableMap.<String, Object>of(
      "alertLevels", Arrays.asList("error", "warn", "unknown")
      ));

    List<MeasureFilterCondition> conditions = filter.getMeasureConditions();
    assertThat(conditions).isEmpty();
  }

  @Test
  public void name_conditions() {
    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    Map<String, Object> props = ImmutableMap.<String, Object>of(
      "nameSearch", "SonarQube"
      );
    MeasureFilter filter = factory.create(props);

    assertThat(filter.getResourceName()).isEqualTo("SonarQube");
  }

  @Test
  public void not_fail_when_name_conditions_contains_array() {
    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    Map<String, Object> props = ImmutableMap.<String, Object>of(
      "nameSearch", new String[] {"sonar", "qube"}
      );
    MeasureFilter filter = factory.create(props);

    assertThat(filter.getResourceName()).isEqualTo("sonar,qube");
  }

  @Test
  public void not_fail_when_name_conditions_contains_list() {
    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    Map<String, Object> props = ImmutableMap.<String, Object>of(
      "nameSearch", newArrayList("sonar", "qube")
      );
    MeasureFilter filter = factory.create(props);

    assertThat(filter.getResourceName()).isEqualTo("sonar,qube");
  }

  @Test
  public void ignore_partial_measure_condition() {
    MeasureFilterFactory factory = new MeasureFilterFactory(newMetricFinder(), system);
    Map<String, Object> props = ImmutableMap.<String, Object>of(
      "c1_op", "gte",
      "c1_val", "3.14"
      );
    MeasureFilter filter = factory.create(props);

    List<MeasureFilterCondition> conditions = filter.getMeasureConditions();
    assertThat(conditions).isEmpty();
  }

  private MetricFinder newMetricFinder() {
    MetricFinder finder = mock(MetricFinder.class);
    when(finder.findByKey(anyString())).thenAnswer(new Answer<Metric>() {
      public Metric answer(InvocationOnMock invocationOnMock) throws Throwable {
        String key = (String) invocationOnMock.getArguments()[0];
        return new Metric.Builder(key, key, Metric.ValueType.INT).create();
      }
    });
    return finder;
  }
}
