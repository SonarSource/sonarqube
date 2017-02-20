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
package org.sonar.server.measure.index;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric.Level;
import org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.server.component.ws.FilterParser.Operator.EQ;

public class ProjectMeasuresQueryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ProjectMeasuresQuery underTest = new ProjectMeasuresQuery();

  @Test
  public void empty_query() throws Exception {
    assertThat(underTest.getMetricCriteria()).isEmpty();
    assertThat(underTest.getQualityGateStatus()).isEmpty();
    assertThat(underTest.getOrganizationUuid()).isEmpty();
  }

  @Test
  public void add_metric_criterion() throws Exception {
    underTest.addMetricCriterion(new MetricCriterion("coverage", EQ, 10d));

    assertThat(underTest.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("coverage", EQ, 10d));
  }

  @Test
  public void set_quality_gate_status() throws Exception {
    underTest.setQualityGateStatus(OK);

    assertThat(underTest.getQualityGateStatus().get()).isEqualTo(Level.OK);
  }

  @Test
  public void default_sort_is_by_name() throws Exception {
    assertThat(underTest.getSort()).isEqualTo("name");
  }

  @Test
  public void fail_to_set_null_sort() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Sort cannot be null");

    underTest.setSort(null);
  }
}
