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
package org.sonar.server.measure.index;

import org.junit.Test;
import org.sonar.api.measures.Metric.Level;
import org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.Operator.EQ;

public class ProjectMeasuresQueryTest {

  private ProjectMeasuresQuery underTest = new ProjectMeasuresQuery();

  @Test
  public void empty_query() {
    assertThat(underTest.getMetricCriteria()).isEmpty();
    assertThat(underTest.getQualityGateStatus()).isEmpty();
  }

  @Test
  public void add_metric_criterion() {
    underTest.addMetricCriterion(MetricCriterion.create("coverage", EQ, 10d));

    assertThat(underTest.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("coverage", EQ, 10d));
  }

  @Test
  public void isNoData_returns_true_when_no_data() {
    underTest.addMetricCriterion(MetricCriterion.createNoData("coverage"));

    assertThat(underTest.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::isNoData)
      .containsOnly(tuple("coverage", true));
  }

  @Test
  public void isNoData_returns_false_when_data_exists() {
    underTest.addMetricCriterion(MetricCriterion.create("coverage", EQ, 10d));

    assertThat(underTest.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::isNoData)
      .containsOnly(tuple("coverage", EQ, false));
  }

  @Test
  public void set_quality_gate_status() {
    underTest.setQualityGateStatus(OK);

    assertThat(underTest.getQualityGateStatus()).contains(Level.OK);
  }

  @Test
  public void default_sort_is_by_name() {
    assertThat(underTest.getSort()).isEqualTo("name");
  }

  @Test
  public void fail_to_set_null_sort() {
    assertThatThrownBy(() -> underTest.setSort(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("Sort cannot be null");
  }

  @Test
  public void fail_to_get_value_when_no_data() {
    assertThatThrownBy(() -> MetricCriterion.createNoData("coverage").getValue())
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("The criterion for metric coverage has no data");
  }

  @Test
  public void fail_to_get_operator_when_no_data() {
    assertThatThrownBy(() -> MetricCriterion.createNoData("coverage").getOperator())
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("The criterion for metric coverage has no data");
  }
}
