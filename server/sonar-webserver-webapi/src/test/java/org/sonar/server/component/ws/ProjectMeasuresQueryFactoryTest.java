/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.component.ws;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.sonar.server.component.ws.FilterParser.Criterion;
import org.sonar.server.measure.index.ProjectMeasuresQuery;
import org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.component.ws.ProjectMeasuresQueryFactory.newProjectMeasuresQuery;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.Operator;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.Operator.EQ;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.Operator.GT;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.Operator.IN;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.Operator.LT;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.Operator.LTE;

public class ProjectMeasuresQueryFactoryTest {

  @Test
  public void create_query() {
    List<Criterion> criteria = asList(
      Criterion.builder().setKey("ncloc").setOperator(GT).setValue("10").build(),
      Criterion.builder().setKey("coverage").setOperator(LTE).setValue("80").build());

    ProjectMeasuresQuery underTest = newProjectMeasuresQuery(criteria, emptySet());

    assertThat(underTest.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(
        tuple("ncloc", GT, 10d),
        tuple("coverage", Operator.LTE, 80d));
  }

  @Test
  public void fail_when_no_value() {
    assertThatThrownBy(() -> {
      newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator(GT).setValue(null).build()),
        emptySet());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value cannot be null for 'ncloc'");
  }

  @Test
  public void fail_when_not_double() {
    assertThatThrownBy(() -> {
      newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator(GT).setValue("ten").build()),
        emptySet());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value 'ten' is not a number");
  }

  @Test
  public void fail_when_no_operator() {
    assertThatThrownBy(() -> {
      newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator(null).setValue("ten").build()),
        emptySet());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Operator cannot be null for 'ncloc'");
  }

  @Test
  public void create_query_on_quality_gate() {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("alert_status").setOperator(EQ).setValue("OK").build()),
      emptySet());

    assertThat(query.getQualityGateStatus().get().name()).isEqualTo("OK");
  }

  @Test
  public void fail_to_create_query_on_quality_gate_when_operator_is_not_equal() {
    assertThatThrownBy(() -> {
      newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("alert_status").setOperator(GT).setValue("OK").build()), emptySet());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Only equals operator is available for quality gate criteria");
  }

  @Test
  public void fail_to_create_query_on_quality_gate_when_value_is_incorrect() {
    assertThatThrownBy(() -> {
      newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("alert_status").setOperator(EQ).setValue("unknown").build()), emptySet());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unknown quality gate status : 'unknown'");
  }

  @Test
  public void create_query_on_qualifier() {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("qualifier").setOperator(EQ).setValue("APP").build()),
      emptySet());

    assertThat(query.getQualifiers().get()).containsOnly("APP");
  }

  @Test
  public void fail_to_create_query_on_qualifier_when_operator_is_not_equal() {
    assertThatThrownBy(() -> {
      newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("qualifier").setOperator(GT).setValue("APP").build()), emptySet());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Only equals operator is available for qualifier criteria");
  }

  @Test
  public void fail_to_create_query_on_qualifier_when_value_is_incorrect() {
    assertThatThrownBy(() -> {
      newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("qualifier").setOperator(EQ).setValue("unknown").build()), emptySet());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unknown qualifier : 'unknown'");
  }

  @Test
  public void create_query_on_language_using_in_operator() {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(
      singletonList(Criterion.builder().setKey("languages").setOperator(IN).setValues(asList("java", "js")).build()),
      emptySet());

    assertThat(query.getLanguages().get()).containsOnly("java", "js");
  }

  @Test
  public void create_query_on_language_using_equals_operator() {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(
      singletonList(Criterion.builder().setKey("languages").setOperator(EQ).setValue("java").build()),
      emptySet());

    assertThat(query.getLanguages().get()).containsOnly("java");
  }

  @Test
  public void fail_to_create_query_on_language_using_in_operator_and_value() {
    assertThatThrownBy(() -> {
      newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("languages").setOperator(IN).setValue("java").build()), emptySet());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Languages should be set either by using 'languages = java' or 'languages IN (java, js)'");
  }

  @Test
  public void fail_to_create_query_on_language_using_eq_operator_and_values() {
    assertThatThrownBy(() -> {
      newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("languages").setOperator(EQ).setValues(asList("java")).build()), emptySet());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Languages should be set either by using 'languages = java' or 'languages IN (java, js)'");
  }

  @Test
  public void create_query_on_tag_using_in_operator() {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(
      singletonList(Criterion.builder().setKey("tags").setOperator(IN).setValues(asList("java", "js")).build()),
      emptySet());

    assertThat(query.getTags().get()).containsOnly("java", "js");
  }

  @Test
  public void create_query_on_tag_using_equals_operator() {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(
      singletonList(Criterion.builder().setKey("tags").setOperator(EQ).setValue("java").build()),
      emptySet());

    assertThat(query.getTags().get()).containsOnly("java");
  }

  @Test
  public void fail_to_create_query_on_tag_using_in_operator_and_value() {
    assertThatThrownBy(() -> {
      newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("tags").setOperator(IN).setValue("java").build()), emptySet());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Tags should be set either by using 'tags = java' or 'tags IN (finance, platform)'");
  }

  @Test
  public void fail_to_create_query_on_tag_using_eq_operator_and_values() {
    assertThatThrownBy(() -> {
      newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("tags").setOperator(EQ).setValues(asList("java")).build()), emptySet());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Tags should be set either by using 'tags = java' or 'tags IN (finance, platform)'");
  }

  @Test
  public void create_query_having_q() {
    List<Criterion> criteria = singletonList(Criterion.builder().setKey("query").setOperator(EQ).setValue("Sonar Qube").build());

    ProjectMeasuresQuery underTest = newProjectMeasuresQuery(criteria, emptySet());

    assertThat(underTest.getQueryText()).contains("Sonar Qube");
  }

  @Test
  public void create_query_having_q_ignore_case_sensitive() {
    List<Criterion> criteria = singletonList(Criterion.builder().setKey("query").setOperator(EQ).setValue("Sonar Qube").build());

    ProjectMeasuresQuery underTest = newProjectMeasuresQuery(criteria, emptySet());

    assertThat(underTest.getQueryText()).contains("Sonar Qube");
  }

  @Test
  public void fail_to_create_query_having_q_with_no_value() {
    assertThatThrownBy(() -> {
      newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("query").setOperator(EQ).build()),
        emptySet());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Query is invalid");
  }

  @Test
  public void fail_to_create_query_having_q_with_other_operator_than_equals() {
    assertThatThrownBy(() -> {
      newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("query").setOperator(LT).setValue("java").build()),
        emptySet());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Query should only be used with equals operator");
  }

  @Test
  public void do_not_filter_on_projectUuids_if_criteria_non_empty_and_projectUuid_is_null() {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator(EQ).setValue("10").build()),
      null);

    assertThat(query.getProjectUuids()).isEmpty();
  }

  @Test
  public void filter_on_projectUuids_if_projectUuid_is_empty_and_criteria_non_empty() {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator(GT).setValue("10").build()),
      emptySet());

    assertThat(query.getProjectUuids()).isPresent();
  }

  @Test
  public void filter_on_projectUuids_if_projectUuid_is_non_empty_and_criteria_non_empty() {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator(GT).setValue("10").build()),
      Collections.singleton("foo"));

    assertThat(query.getProjectUuids()).isPresent();
  }

  @Test
  public void filter_on_projectUuids_if_projectUuid_is_empty_and_criteria_is_empty() {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(emptyList(), emptySet());

    assertThat(query.getProjectUuids()).isPresent();
  }

  @Test
  public void filter_on_projectUuids_if_projectUuid_is_non_empty_and_criteria_empty() {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(emptyList(), Collections.singleton("foo"));

    assertThat(query.getProjectUuids()).isPresent();
  }

  @Test
  public void convert_metric_to_lower_case() {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(asList(
      Criterion.builder().setKey("NCLOC").setOperator(GT).setValue("10").build(),
      Criterion.builder().setKey("coVERage").setOperator(LTE).setValue("80").build()),
      emptySet());

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(
        tuple("ncloc", GT, 10d),
        tuple("coverage", Operator.LTE, 80d));
  }

  @Test
  public void filter_no_data() {
    List<Criterion> criteria = singletonList(Criterion.builder().setKey("duplicated_lines_density").setOperator(EQ).setValue("NO_DATA").build());

    ProjectMeasuresQuery underTest = newProjectMeasuresQuery(criteria, emptySet());

    assertThat(underTest.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::isNoData)
      .containsOnly(tuple("duplicated_lines_density", true));
  }

  @Test
  public void fail_to_use_no_data_with_operator_lower_than() {
    List<Criterion> criteria = singletonList(Criterion.builder().setKey("duplicated_lines_density").setOperator(LT).setValue("NO_DATA").build());

    assertThatThrownBy(() -> newProjectMeasuresQuery(criteria, emptySet()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("NO_DATA can only be used with equals operator");
  }

  @Test
  public void filter_no_data_with_other_case() {
    List<Criterion> criteria = singletonList(Criterion.builder().setKey("duplicated_lines_density").setOperator(EQ).setValue("nO_DaTa").build());

    ProjectMeasuresQuery underTest = newProjectMeasuresQuery(criteria, emptySet());

    assertThat(underTest.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::isNoData)
      .containsOnly(tuple("duplicated_lines_density", true));
  }

  @Test
  public void accept_empty_query() {
    ProjectMeasuresQuery result = newProjectMeasuresQuery(emptyList(), emptySet());

    assertThat(result.getMetricCriteria()).isEmpty();
  }

}
