/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.core.util.Uuids;
import org.sonar.db.component.ComponentDtoWithSnapshotId;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.client.measure.ComponentTreeWsRequest;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.server.measure.ws.ComponentTreeAction.METRIC_PERIOD_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.METRIC_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.NAME_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.PATH_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.QUALIFIER_SORT;

public class ComponentTreeSortTest {
  private static final String NUM_METRIC_KEY = "violations";
  private static final String TEXT_METRIC_KEY = "sqale_index";

  private List<MetricDto> metrics;
  private Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric;
  private List<ComponentDtoWithSnapshotId> components;

  @Before
  public void setUp() {
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
    MetricDto sqaleIndexMetric = newMetricDto()
      .setKey(TEXT_METRIC_KEY)
      .setValueType(ValueType.DATA.name());

    metrics = newArrayList(violationsMetric, sqaleIndexMetric);

    measuresByComponentUuidAndMetric = HashBasedTable.create(components.size(), 1);
    // same number than path field
    double currentValue = 9;
    for (ComponentDtoWithSnapshotId component : components) {
      measuresByComponentUuidAndMetric.put(component.uuid(), violationsMetric, new MeasureDto().setValue(currentValue)
        .setVariation(1, -currentValue)
        .setVariation(5, currentValue));
      measuresByComponentUuidAndMetric.put(component.uuid(), sqaleIndexMetric, new MeasureDto().setData(String.valueOf(currentValue)));
      currentValue--;
    }
  }

  @Test
  public void sort_by_names() {
    ComponentTreeWsRequest wsRequest = newRequest(singletonList(NAME_SORT), true, null);
    List<ComponentDtoWithSnapshotId> result = sortComponents(wsRequest);

    assertThat(result).extracting("name")
      .containsExactly("name-1", "name-2", "name-3", "name-4", "name-5", "name-6", "name-7", "name-8", "name-9");
  }

  @Test
  public void sort_by_qualifier() {
    ComponentTreeWsRequest wsRequest = newRequest(singletonList(QUALIFIER_SORT), false, null);

    List<ComponentDtoWithSnapshotId> result = sortComponents(wsRequest);

    assertThat(result).extracting("qualifier")
      .containsExactly("qualifier-9", "qualifier-8", "qualifier-7", "qualifier-6", "qualifier-5", "qualifier-4", "qualifier-3", "qualifier-2", "qualifier-1");
  }

  @Test
  public void sort_by_path() {
    ComponentTreeWsRequest wsRequest = newRequest(singletonList(PATH_SORT), true, null);

    List<ComponentDtoWithSnapshotId> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-1", "path-2", "path-3", "path-4", "path-5", "path-6", "path-7", "path-8", "path-9");
  }

  @Test
  public void sort_by_numerical_metric_key_ascending() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeWsRequest wsRequest = newRequest(singletonList(METRIC_SORT), true, NUM_METRIC_KEY);

    List<ComponentDtoWithSnapshotId> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-1", "path-2", "path-3", "path-4", "path-5", "path-6", "path-7", "path-8", "path-9", "path-without-measure");
  }

  @Test
  public void sort_by_numerical_metric_key_descending() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeWsRequest wsRequest = newRequest(singletonList(METRIC_SORT), false, NUM_METRIC_KEY);

    List<ComponentDtoWithSnapshotId> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-9", "path-8", "path-7", "path-6", "path-5", "path-4", "path-3", "path-2", "path-1", "path-without-measure");
  }

  @Test
  public void sort_by_numerical_metric_period_1_key_ascending() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeWsRequest wsRequest = newRequest(singletonList(METRIC_PERIOD_SORT), true, NUM_METRIC_KEY).setMetricPeriodSort(1);

    List<ComponentDtoWithSnapshotId> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-9", "path-8", "path-7", "path-6", "path-5", "path-4", "path-3", "path-2", "path-1", "path-without-measure");
  }

  @Test
  public void sort_by_numerical_metric_period_1_key_descending() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeWsRequest wsRequest = newRequest(singletonList(METRIC_PERIOD_SORT), false, NUM_METRIC_KEY).setMetricPeriodSort(1);

    List<ComponentDtoWithSnapshotId> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-1", "path-2", "path-3", "path-4", "path-5", "path-6", "path-7", "path-8", "path-9", "path-without-measure");
  }

  @Test
  public void sort_by_numerical_metric_period_5_key() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeWsRequest wsRequest = newRequest(singletonList(METRIC_SORT), false, NUM_METRIC_KEY).setMetricPeriodSort(5);

    List<ComponentDtoWithSnapshotId> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-9", "path-8", "path-7", "path-6", "path-5", "path-4", "path-3", "path-2", "path-1", "path-without-measure");
  }

  @Test
  public void sort_by_textual_metric_key_ascending() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeWsRequest wsRequest = newRequest(singletonList(METRIC_SORT), true, TEXT_METRIC_KEY);

    List<ComponentDtoWithSnapshotId> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-1", "path-2", "path-3", "path-4", "path-5", "path-6", "path-7", "path-8", "path-9", "path-without-measure");
  }

  @Test
  public void sort_by_textual_metric_key_descending() {
    components.add(newComponentWithoutSnapshotId("name-without-measure", "qualifier-without-measure", "path-without-measure"));
    ComponentTreeWsRequest wsRequest = newRequest(singletonList(METRIC_SORT), false, TEXT_METRIC_KEY);

    List<ComponentDtoWithSnapshotId> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-9", "path-8", "path-7", "path-6", "path-5", "path-4", "path-3", "path-2", "path-1", "path-without-measure");
  }

  @Test
  public void sort_on_multiple_fields() {
    components = newArrayList(
      newComponentWithoutSnapshotId("name-1", "qualifier-1", "path-2"),
      newComponentWithoutSnapshotId("name-1", "qualifier-1", "path-3"),
      newComponentWithoutSnapshotId("name-1", "qualifier-1", "path-1"));
    ComponentTreeWsRequest wsRequest = newRequest(newArrayList(NAME_SORT, QUALIFIER_SORT, PATH_SORT), true, null);

    List<ComponentDtoWithSnapshotId> result = sortComponents(wsRequest);

    assertThat(result).extracting("path")
      .containsExactly("path-1", "path-2", "path-3");
  }

  private List<ComponentDtoWithSnapshotId> sortComponents(ComponentTreeWsRequest wsRequest) {
    return ComponentTreeSort.sortComponents(components, wsRequest, metrics, measuresByComponentUuidAndMetric);
  }

  private static ComponentDtoWithSnapshotId newComponentWithoutSnapshotId(String name, String qualifier, String path) {
    return (ComponentDtoWithSnapshotId) new ComponentDtoWithSnapshotId()
      .setUuid(Uuids.createFast())
      .setName(name)
      .setQualifier(qualifier)
      .setPath(path);
  }

  private static ComponentTreeWsRequest newRequest(List<String> sortFields, boolean isAscending, @Nullable String metricKey) {
    return new ComponentTreeWsRequest()
      .setAsc(isAscending)
      .setSort(sortFields)
      .setMetricSort(metricKey);
  }
}
