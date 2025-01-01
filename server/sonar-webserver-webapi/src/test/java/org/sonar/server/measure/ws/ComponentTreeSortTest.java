/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.measure.ws;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.core.util.Uuids;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.measure.ImpactMeasureBuilder;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.server.measure.ws.ComponentTreeAction.METRIC_PERIOD_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.METRIC_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.NAME_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.PATH_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.QUALIFIER_SORT;
import static org.sonar.server.measure.ws.ComponentTreeData.Measure.createFromMetricValue;

class ComponentTreeSortTest {
  private static final String NUM_METRIC_KEY = "violations";
  private static final String NEW_METRIC_KEY = "new_violations";
  private static final String TEXT_METRIC_KEY = "sqale_index";

  private static final String DATA_IMPACT_METRIC_KEY = "reliability_issues";
  private static final String NEW_DATA_IMPACT_METRIC_KEY = "new_reliability_issues";

  private List<MetricDto> metrics;
  private Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric;
  private List<ComponentDto> components;

  @BeforeEach
  void setUp() {
    components = newArrayList(
      newComponentWithoutSnapshotId("name-1", "qualifier-2", "path-9"),
      newComponentWithoutSnapshotId("name-3", "qualifier-3", "path-8"),
      newComponentWithoutSnapshotId("name-2", "qualifier-4", "path-7"),
      newComponentWithoutSnapshotId("name-4", "qualifier-5", "path-6"),
      newComponentWithoutSnapshotId("name-7", "qualifier-6", "path-5"),
      newComponentWithoutSnapshotId("name-6", "qualifier-7", "path-4"),
      newComponentWithoutSnapshotId("name-5", "qualifier-8", "path-3"),
      newComponentWithoutSnapshotId("name-9", "qualifier-9", "path-2"),
      newComponentWithoutSnapshotId("name-8", "qualifier-1", "path-1"));

    MetricDto violationsMetric = newMetricDto()
      .setKey(NUM_METRIC_KEY)
      .setValueType(ValueType.INT.name());
    MetricDto newViolationsMetric = newMetricDto()
      .setKey(NEW_METRIC_KEY)
      .setValueType(ValueType.INT.name());
    MetricDto sqaleIndexMetric = newMetricDto()
      .setKey(TEXT_METRIC_KEY)
      .setValueType(ValueType.STRING.name());
    MetricDto reliabilityIssueMetric = newMetricDto()
      .setKey(DATA_IMPACT_METRIC_KEY)
      .setValueType(ValueType.DATA.name());
    MetricDto newReliabilityIssueMetric = newMetricDto()
      .setKey(NEW_DATA_IMPACT_METRIC_KEY)
      .setValueType(ValueType.DATA.name());

    metrics = newArrayList(violationsMetric, sqaleIndexMetric, newViolationsMetric, reliabilityIssueMetric, newReliabilityIssueMetric);

    measuresByComponentUuidAndMetric = HashBasedTable.create(components.size(), 2);
    // same number than path field
    double currentValue = 9;
    for (ComponentDto component : components) {
      measuresByComponentUuidAndMetric.put(component.uuid(), violationsMetric, createFromMetricValue(violationsMetric, currentValue));
      measuresByComponentUuidAndMetric.put(component.uuid(), newViolationsMetric, createFromMetricValue(newViolationsMetric, currentValue));
      measuresByComponentUuidAndMetric.put(component.uuid(), sqaleIndexMetric, createFromMetricValue(sqaleIndexMetric, currentValue));
      measuresByComponentUuidAndMetric.put(component.uuid(), reliabilityIssueMetric, createFromMetricValue(reliabilityIssueMetric, buildJsonImpact((int) currentValue)));
      measuresByComponentUuidAndMetric.put(component.uuid(), newReliabilityIssueMetric, createFromMetricValue(newReliabilityIssueMetric, buildJsonImpact((int) currentValue)));
      currentValue--;
    }
  }

  private static String buildJsonImpact(int currentValue) {
    return String.valueOf(ImpactMeasureBuilder.newInstance()
      .setSeverity(Severity.HIGH, currentValue)
      .setSeverity(Severity.MEDIUM, currentValue)
      .setSeverity(Severity.LOW, currentValue)
      .setTotal(currentValue)
      .buildAsString());
  }

  @Test
  void sort_by_names() {
    ComponentTreeRequest wsRequest = newRequest(singletonList(NAME_SORT), true, null);
    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("name")
      .containsExactly("name-1", "name-2", "name-3", "name-4", "name-5", "name-6", "name-7", "name-8", "name-9");
  }

  @Test
  void sort_by_qualifier() {
    ComponentTreeRequest wsRequest = newRequest(singletonList(QUALIFIER_SORT), false, null);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("qualifier")
      .containsExactly("qualifier-9", "qualifier-8", "qualifier-7", "qualifier-6", "qualifier-5", "qualifier-4", "qualifier-3", "qualifier-2", "qualifier-1");
  }

  @Test
  void sort_by_path() {
    ComponentTreeRequest wsRequest = newRequest(singletonList(PATH_SORT), true, null);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-1", "path-2", "path-3", "path-4", "path-5", "path-6", "path-7", "path-8", "path-9");
  }

  @Test
  void sort_by_numerical_metric_key_ascending() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeRequest wsRequest = newRequest(singletonList(METRIC_SORT), true, NUM_METRIC_KEY);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-1", "path-2", "path-3", "path-4", "path-5", "path-6", "path-7", "path-8", "path-9", "path-without-measure");
  }

  @Test
  void sort_by_numerical_metric_key_descending() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeRequest wsRequest = newRequest(singletonList(METRIC_SORT), false, NUM_METRIC_KEY);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-9", "path-8", "path-7", "path-6", "path-5", "path-4", "path-3", "path-2", "path-1", "path-without-measure");
  }

  @Test
  void sort_by_name_ascending_in_case_of_equality() {
    components = newArrayList(
      newComponentWithoutSnapshotId("PROJECT 12", ComponentQualifiers.PROJECT, "PROJECT_PATH_1"),
      newComponentWithoutSnapshotId("PROJECT 11", ComponentQualifiers.PROJECT, "PROJECT_PATH_1"),
      newComponentWithoutSnapshotId("PROJECT 0", ComponentQualifiers.PROJECT, "PROJECT_PATH_2"));

    ComponentTreeRequest wsRequest = newRequest(newArrayList(PATH_SORT), false, null);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("name").containsExactly("PROJECT 0", "PROJECT 11", "PROJECT 12");
  }

  @Test
  void sort_by_alert_status_ascending() {
    components = newArrayList(
      newComponentWithoutSnapshotId("PROJECT OK 1", ComponentQualifiers.PROJECT, "PROJECT_OK_PATH_1"),
      newComponentWithoutSnapshotId("PROJECT ERROR 1", ComponentQualifiers.PROJECT, "PROJECT_ERROR_PATH_1"),
      newComponentWithoutSnapshotId("PROJECT OK 2", ComponentQualifiers.PROJECT, "PROJECT_OK_PATH_2"),
      newComponentWithoutSnapshotId("PROJECT ERROR 2", ComponentQualifiers.PROJECT, "PROJECT_ERROR_PATH_2"));
    metrics = singletonList(newMetricDto()
      .setKey(CoreMetrics.ALERT_STATUS_KEY)
      .setValueType(ValueType.LEVEL.name()));
    measuresByComponentUuidAndMetric = HashBasedTable.create();
    List<String> statuses = newArrayList("OK", "ERROR");
    for (int i = 0; i < components.size(); i++) {
      ComponentDto component = components.get(i);
      String alertStatus = statuses.get(i % 2);
      measuresByComponentUuidAndMetric.put(component.uuid(), metrics.get(0), createFromMetricValue(metrics.get(0), alertStatus));
    }
    ComponentTreeRequest wsRequest = newRequest(newArrayList(METRIC_SORT, NAME_SORT), true, CoreMetrics.ALERT_STATUS_KEY);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("name").containsExactly(
      "PROJECT ERROR 1", "PROJECT ERROR 2",
      "PROJECT OK 1", "PROJECT OK 2");
  }

  @Test
  void sort_by_numerical_metric_period_1_key_descending() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeRequest wsRequest = newRequest(singletonList(METRIC_PERIOD_SORT), false, NEW_METRIC_KEY).setMetricPeriodSort(1);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-9", "path-8", "path-7", "path-6", "path-5", "path-4", "path-3", "path-2", "path-1", "path-without-measure");
  }

  @Test
  void sort_by_numerical_metric_period_1_key_ascending() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeRequest wsRequest = newRequest(singletonList(METRIC_PERIOD_SORT), true, NEW_METRIC_KEY).setMetricPeriodSort(1);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-1", "path-2", "path-3", "path-4", "path-5", "path-6", "path-7", "path-8", "path-9", "path-without-measure");
  }

  @Test
  void sort_by_numerical_metric_period_5_key() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeRequest wsRequest = newRequest(singletonList(METRIC_SORT), false, NUM_METRIC_KEY).setMetricPeriodSort(5);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-9", "path-8", "path-7", "path-6", "path-5", "path-4", "path-3", "path-2", "path-1", "path-without-measure");
  }

  @Test
  void sort_by_textual_metric_key_ascending() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeRequest wsRequest = newRequest(singletonList(METRIC_SORT), true, TEXT_METRIC_KEY);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-1", "path-2", "path-3", "path-4", "path-5", "path-6", "path-7", "path-8", "path-9", "path-without-measure");
  }

  @Test
  void sort_by_textual_metric_key_descending() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeRequest wsRequest = newRequest(singletonList(METRIC_SORT), false, TEXT_METRIC_KEY);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-9", "path-8", "path-7", "path-6", "path-5", "path-4", "path-3", "path-2", "path-1", "path-without-measure");
  }

  @Test
  void sort_on_multiple_fields() {
    components = newArrayList(
      newComponentWithoutSnapshotId("name-1", "qualifier-1", "path-2"),
      newComponentWithoutSnapshotId("name-1", "qualifier-1", "path-3"),
      newComponentWithoutSnapshotId("name-1", "qualifier-1", "path-1"));
    ComponentTreeRequest wsRequest = newRequest(newArrayList(NAME_SORT, QUALIFIER_SORT, PATH_SORT), true, null);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-1", "path-2", "path-3");
  }

  @Test
  void sortComponent_whenMetricIsImpactDataType_shouldOrderByTotalAscending() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeRequest wsRequest = newRequest(singletonList(METRIC_SORT), true, DATA_IMPACT_METRIC_KEY);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-1", "path-2", "path-3", "path-4", "path-5", "path-6", "path-7", "path-8", "path-9", "path-without-measure");
  }

  @Test
  void sortComponent_whenMetricIsImpactDataType_shouldOrderByTotalDescending() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeRequest wsRequest = newRequest(singletonList(METRIC_SORT), false, DATA_IMPACT_METRIC_KEY);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-9", "path-8", "path-7", "path-6", "path-5", "path-4", "path-3", "path-2", "path-1", "path-without-measure");
  }

  @Test
  void sortComponent_whenMetricIsNewAndMetricPeriodSort_shouldOrderByTotal() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeRequest wsRequest = newRequest(singletonList(METRIC_PERIOD_SORT), false, NEW_DATA_IMPACT_METRIC_KEY).setMetricPeriodSort(1);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-9", "path-8", "path-7", "path-6", "path-5", "path-4", "path-3", "path-2", "path-1", "path-without-measure");
  }

  @Test
  void sortComponent_whenMetricIsNotNewAndMetricPeriodSort_shouldNotOrder() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeRequest wsRequest = newRequest(singletonList(METRIC_PERIOD_SORT), false, DATA_IMPACT_METRIC_KEY).setMetricPeriodSort(1);

    List<ComponentDto> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-9", "path-7", "path-8", "path-6", "path-3", "path-4", "path-5", "path-1", "path-2", "path-without-measure");
  }

  private List<ComponentDto> sortComponents(ComponentTreeRequest wsRequest) {
    return ComponentTreeSort.sortComponents(components, wsRequest, metrics, measuresByComponentUuidAndMetric);
  }

  private static ComponentDto newComponentWithoutSnapshotId(String name, String qualifier, String path) {
    return new ComponentDto()
      .setUuid(Uuids.createFast())
      .setName(name)
      .setQualifier(qualifier)
      .setPath(path);
  }

  private static ComponentTreeRequest newRequest(List<String> sortFields, boolean isAscending, @Nullable String metricKey) {
    return new ComponentTreeRequest()
      .setAsc(isAscending)
      .setSort(sortFields)
      .setMetricSort(metricKey);
  }
}
