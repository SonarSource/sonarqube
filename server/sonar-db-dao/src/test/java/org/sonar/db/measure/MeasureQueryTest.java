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
package org.sonar.db.measure;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class MeasureQueryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_query_from_projects() {
    MeasureQuery query = MeasureQuery.builder().setProjectUuids(asList("PROJECT_1", "PROJECT_2")).build();

    assertThat(query.getProjectUuids()).containsOnly("PROJECT_1", "PROJECT_2");
    assertThat(query.isOnProjects()).isTrue();
    assertThat(query.isOnComponents()).isFalse();
    assertThat(query.isOnSingleComponent()).isFalse();
  }

  @Test
  public void create_query_from_project_and_components() {
    MeasureQuery query = MeasureQuery.builder().setComponentUuids("PROJECT_1", asList("FILE_1", "FILE_2")).build();

    assertThat(query.getProjectUuids()).containsOnly("PROJECT_1");
    assertThat(query.getProjectUuid()).isEqualTo("PROJECT_1");
    assertThat(query.getComponentUuids()).containsOnly("FILE_1", "FILE_2");
    assertThat(query.isOnProjects()).isFalse();
    assertThat(query.isOnComponents()).isTrue();
    assertThat(query.isOnSingleComponent()).isFalse();
  }

  @Test
  public void create_query_from_single_component_uuid() {
    MeasureQuery query = MeasureQuery.builder().setComponentUuid("FILE_1").build();

    assertThat(query.getComponentUuids()).containsOnly("FILE_1");
    assertThat(query.getComponentUuid()).isEqualTo("FILE_1");
    assertThat(query.isOnProjects()).isFalse();
    assertThat(query.isOnComponents()).isFalse();
    assertThat(query.isOnSingleComponent()).isTrue();
  }

  @Test
  public void create_query_from_metric_ids() {
    MeasureQuery query = MeasureQuery.builder().setProjectUuids(asList("PROJECT_1", "PROJECT_2")).setMetricIds(asList(10, 11)).build();

    assertThat(query.getMetricIds()).containsOnly(10, 11);
    assertThat(query.getMetricKeys()).isNull();
  }

  @Test
  public void create_query_from_metric_keys() {
    MeasureQuery query = MeasureQuery.builder().setProjectUuids(asList("PROJECT_1", "PROJECT_2")).setMetricKeys(asList("M1", "M2")).build();

    assertThat(query.getMetricKeys()).containsOnly("M1", "M2");
    assertThat(query.getMetricIds()).isNull();
  }

  @Test
  public void return_empty_when_metrics_are_empty() {
    assertThat(MeasureQuery.builder()
      .setProjectUuids(asList("PROJECT_1", "PROJECT_2"))
      .setMetricKeys(emptyList())
      .build().returnsEmpty()).isTrue();

    assertThat(MeasureQuery.builder()
      .setProjectUuids(asList("PROJECT_1", "PROJECT_2"))
      .setMetricIds(emptyList())
      .build().returnsEmpty()).isTrue();
  }

  @Test
  public void return_empty_when_projects_are_empty() {
    assertThat(MeasureQuery.builder()
      .setProjectUuids(emptyList())
      .build().returnsEmpty()).isTrue();
  }

  @Test
  public void return_empty_when_components_are_empty() {
    assertThat(MeasureQuery.builder()
      .setComponentUuids("PROJECT", emptyList())
      .build().returnsEmpty()).isTrue();
  }

  @Test
  public void fail_when_no_component_uuid_filter() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("At least one filter on component UUID is expected");
    MeasureQuery.builder().build();
  }

  @Test
  public void fail_when_component_uuids_without_project_uuid() {
    expectedException.expect(NullPointerException.class);
    MeasureQuery.builder().setComponentUuids(null, asList("FILE_1", "FILE_2")).build();
  }

  @Test
  public void fail_when_using_metric_ids_and_metric_keys() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Metric IDs and keys must not be set both");
    MeasureQuery.builder().setMetricIds(asList(10, 11)).setMetricKeys(asList("M1", "M2")).setProjectUuids(asList("PROJECT_1", "PROJECT_2")).build();
  }

}
