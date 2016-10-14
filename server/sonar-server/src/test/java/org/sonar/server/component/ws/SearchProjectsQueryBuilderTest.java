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

package org.sonar.server.component.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.component.ws.SearchProjectsQueryBuilder.SearchProjectsCriteriaQuery;
import static org.sonar.server.component.ws.SearchProjectsQueryBuilder.build;
import static org.sonar.server.component.ws.SearchProjectsQueryBuilder.SearchProjectsCriteriaQuery.MetricCriteria;
import static org.sonar.server.component.ws.SearchProjectsQueryBuilder.SearchProjectsCriteriaQuery.Operator;

public class SearchProjectsQueryBuilderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_query() throws Exception {
    SearchProjectsCriteriaQuery query = build("ncloc > 10 and coverage <= 80");

    assertThat(query.getMetricCriterias())
      .extracting(MetricCriteria::getMetricKey, MetricCriteria::getOperator, MetricCriteria::getValue)
      .containsOnly(
        tuple("ncloc", Operator.GT, 10d),
        tuple("coverage", Operator.LT, 80d));
  }

  @Test
  public void convert_upper_case_to_lower_case() throws Exception {
    assertThat(build("NCLOC > 10 AND coVERage <= 80").getMetricCriterias())
      .extracting(MetricCriteria::getMetricKey, MetricCriteria::getOperator, MetricCriteria::getValue)
      .containsOnly(
        tuple("ncloc", Operator.GT, 10d),
        tuple("coverage", Operator.LT, 80d));
  }

  @Test
  public void ignore_white_spaces() throws Exception {
    assertThat(build("   ncloc    >    10   ").getMetricCriterias())
      .extracting(MetricCriteria::getMetricKey, MetricCriteria::getOperator, MetricCriteria::getValue)
      .containsOnly(tuple("ncloc", Operator.GT, 10d));
  }

  @Test
  public void fail_on_unknown_operator() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unknown operator '>='");
    build("ncloc >= 10");
  }

  @Test
  public void fail_on_invalid_criteria() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criteria 'ncloc ? 10'");
    build("ncloc ? 10");
  }

  @Test
  public void fail_when_no_operator() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criteria 'ncloc 10'");
    build("ncloc 10");
  }

  @Test
  public void fail_when_no_key() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criteria '>= 10'");
    build(">= 10");
  }

  @Test
  public void fail_when_no_value() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criteria 'ncloc >='");
    build("ncloc >=");
  }

  @Test
  public void fail_when_no_criteria_provided() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criteria ''");
    build("");
  }
}
