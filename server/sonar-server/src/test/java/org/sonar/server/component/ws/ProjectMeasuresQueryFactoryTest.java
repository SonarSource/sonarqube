/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.measure.index.ProjectMeasuresQuery;
import org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import org.sonar.server.measure.index.ProjectMeasuresQuery.Operator;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.component.ws.ProjectMeasuresQueryFactory.newProjectMeasuresQuery;
import static org.sonar.server.component.ws.ProjectMeasuresQueryFactory.toCriteria;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.Level.OK;

public class ProjectMeasuresQueryFactoryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Test
  public void create_query() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(toCriteria("ncloc > 10 and coverage <= 80"), emptySet());

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(
        tuple("ncloc", Operator.GT, 10d),
        tuple("coverage", Operator.LTE, 80d));
  }

  @Test
  public void create_query_having_lesser_than_operation() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(toCriteria("ncloc < 10"), emptySet());

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("ncloc", Operator.LT, 10d));
  }

  @Test
  public void create_query_having_lesser_than_or_equals_operation() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(toCriteria("ncloc <= 10"), emptySet());

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("ncloc", Operator.LTE, 10d));
  }

  @Test
  public void create_query_having_greater_than_operation() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(toCriteria("ncloc > 10"), emptySet());

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("ncloc", Operator.GT, 10d));
  }

  @Test
  public void create_query_having_greater_than_or_equals_operation() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(toCriteria("ncloc >= 10"), emptySet());

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("ncloc", Operator.GTE, 10d));
  }

  @Test
  public void create_query_having_equal_operation() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(toCriteria("ncloc = 10"), emptySet());

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("ncloc", Operator.EQ, 10d));
  }

  @Test
  public void create_query_on_quality_gate() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(toCriteria("alert_status = OK"), emptySet());

    assertThat(query.getQualityGateStatus().get().name()).isEqualTo(OK.name());
  }

  @Test
  public void do_not_filter_on_projectUuids_if_criteria_non_empty_and_projectUuid_is_null() {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(toCriteria("ncloc = 10"), null);

    assertThat(query.getProjectUuids()).isEmpty();
  }

  @Test
  public void filter_on_projectUuids_if_projectUuid_is_empty_and_criteria_non_empty() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(toCriteria("ncloc > 10"), emptySet());

    assertThat(query.getProjectUuids()).isPresent();
  }

  @Test
  public void filter_on_projectUuids_if_projectUuid_is_non_empty_and_criteria_non_empty() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(toCriteria("ncloc > 10"), Collections.singleton("foo"));

    assertThat(query.getProjectUuids()).isPresent();
  }

  @Test
  public void filter_on_projectUuids_if_projectUuid_is_empty_and_criteria_is_empty() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(emptyList(), emptySet());

    assertThat(query.getProjectUuids()).isPresent();
  }

  @Test
  public void filter_on_projectUuids_if_projectUuid_is_non_empty_and_criteria_empty() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(emptyList(), Collections.singleton("foo"));

    assertThat(query.getProjectUuids()).isPresent();
  }

  @Test
  public void fail_to_create_query_on_quality_gate_when_operator_is_not_equal() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    newProjectMeasuresQuery(toCriteria("alert_status > OK"), emptySet());
  }

  @Test
  public void search_is_case_insensitive() throws Exception {
    assertThat(newProjectMeasuresQuery(toCriteria("ncloc > 10 AnD coverage <= 80 AND debt = 10 AND issues = 20"), emptySet()).getMetricCriteria()).hasSize(4);
  }

  @Test
  public void convert_metric_to_lower_case() throws Exception {
    assertThat(newProjectMeasuresQuery(toCriteria("NCLOC > 10 AND coVERage <= 80"), emptySet()).getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(
        tuple("ncloc", Operator.GT, 10d),
        tuple("coverage", Operator.LTE, 80d));
  }

  @Test
  public void ignore_white_spaces() throws Exception {
    assertThat(newProjectMeasuresQuery(toCriteria("   ncloc    >    10   "), emptySet()).getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("ncloc", Operator.GT, 10d));
  }

  @Test
  public void accept_empty_query() throws Exception {
    ProjectMeasuresQuery result = newProjectMeasuresQuery(emptyList(), emptySet());

    assertThat(result.getMetricCriteria()).isEmpty();
  }

  @Test
  public void fail_on_invalid_criteria() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criterion 'ncloc ? 10'");

    newProjectMeasuresQuery(toCriteria("ncloc ? 10"), emptySet());
  }

  @Test
  public void fail_on_invalid_criteria_ignore_whitespaces() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criterion 'ncloc ? 10'");

    newProjectMeasuresQuery(toCriteria("    ncloc ? 10    "), emptySet());
  }

  @Test
  public void fail_when_not_double() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criterion 'ncloc > ten'");
    newProjectMeasuresQuery(toCriteria("ncloc > ten"), emptySet());
  }

  @Test
  public void fail_when_no_operator() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criterion 'ncloc 10'");
    newProjectMeasuresQuery(toCriteria("ncloc 10"), emptySet());
  }

  @Test
  public void fail_when_no_key() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criterion '>= 10'");
    newProjectMeasuresQuery(toCriteria(">= 10"), emptySet());
  }

  @Test
  public void fail_when_no_value() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criterion 'ncloc >='");
    newProjectMeasuresQuery(toCriteria("ncloc >="), emptySet());
  }

  @Test
  public void test_hasIsFavouriteCriterion() throws Exception {
    assertThat(ProjectMeasuresQueryFactory.hasIsFavoriteCriterion(toCriteria("isFavorite"))).isTrue();
    assertThat(ProjectMeasuresQueryFactory.hasIsFavoriteCriterion(toCriteria("isFavorite "))).isTrue();
    assertThat(ProjectMeasuresQueryFactory.hasIsFavoriteCriterion(toCriteria("    isFavorite  "))).isTrue();
    assertThat(ProjectMeasuresQueryFactory.hasIsFavoriteCriterion(toCriteria("isFavorite and reliability_rating = 1"))).isTrue();
    assertThat(ProjectMeasuresQueryFactory.hasIsFavoriteCriterion(toCriteria("  isFavorite  and  reliability_rating = 1"))).isTrue();

    assertThat(ProjectMeasuresQueryFactory.hasIsFavoriteCriterion(toCriteria("ncloc > 10"))).isFalse();
    assertThat(ProjectMeasuresQueryFactory.hasIsFavoriteCriterion(toCriteria("ncloc > 10 and coverage <= 80"))).isFalse();
  }

}
