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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.measure.index.ProjectMeasuresQuery;

import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.api.measures.Metric.ValueType.WORK_DUR;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.server.component.ws.FilterParser.Operator.EQ;
import static org.sonar.server.component.ws.FilterParser.Operator.GT;
import static org.sonar.server.component.ws.FilterParser.Operator.LT;
import static org.sonar.server.component.ws.FilterParser.Operator.LTE;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;

public class ProjectMeasuresQueryValidatorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private ProjectMeasuresQueryValidator underTest = new ProjectMeasuresQueryValidator(dbClient);

  @Test
  public void query_with_empty_metrics_is_valid() throws Exception {
    underTest.validate(dbSession, new ProjectMeasuresQuery());
  }

  @Test
  public void does_not_fail_when_metric_criteria_contains_an_existing_metric() throws Exception {
    insertValidMetric("ncloc");
    ProjectMeasuresQuery query = new ProjectMeasuresQuery().addMetricCriterion(new MetricCriterion("ncloc", GT, 10d));

    underTest.validate(dbSession, query);
  }

  @Test
  public void does_not_fail_when_sort_is_by_name() throws Exception {
    insertValidMetric("ncloc");
    ProjectMeasuresQuery query = new ProjectMeasuresQuery()
      .addMetricCriterion(new MetricCriterion("ncloc", GT, 10d))
      .setSort("name");

    underTest.validate(dbSession, query);
  }

  @Test
  public void does_not_fail_when_sort_contains_an_existing_metric() throws Exception {
    insertValidMetric("ncloc");
    insertValidMetric("debt");
    ProjectMeasuresQuery query = new ProjectMeasuresQuery()
      .addMetricCriterion(new MetricCriterion("ncloc", GT, 10d))
      .setSort("debt");

    underTest.validate(dbSession, query);
  }

  @Test
  public void fail_when_metric_are_not_numeric() throws Exception {
    insertMetric(createValidMetric("ncloc").setValueType(INT.name()));
    insertMetric(createValidMetric("debt").setValueType(WORK_DUR.name()));
    insertMetric(createValidMetric("data").setValueType(DATA.name()));
    insertMetric(createValidMetric("distrib").setValueType(DISTRIB.name()));
    insertMetric(createValidMetric("string").setValueType(STRING.name()));
    ProjectMeasuresQuery query = new ProjectMeasuresQuery()
      .addMetricCriterion(new MetricCriterion("data", GT, 10d))
      .addMetricCriterion(new MetricCriterion("distrib", EQ, 11d))
      .addMetricCriterion(new MetricCriterion("ncloc", LTE, 20d))
      .addMetricCriterion(new MetricCriterion("debt", LT, 20d))
      .addMetricCriterion(new MetricCriterion("string", EQ, 40d));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Following metrics are not numeric : [data, distrib, string]");
    underTest.validate(dbSession, query);
  }

  @Test
  public void fail_when_metric_is_disabled() throws Exception {
    insertMetric(createValidMetric("ncloc").setEnabled(false));
    insertMetric(createValidMetric("debt").setEnabled(false));
    ProjectMeasuresQuery query = new ProjectMeasuresQuery()
      .addMetricCriterion(new MetricCriterion("ncloc", GT, 10d))
      .setSort("debt");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Following metrics are disabled : [debt, ncloc]");
    underTest.validate(dbSession, query);
  }

  @Test
  public void fail_when_metric_does_not_exists() throws Exception {
    insertValidMetric("ncloc");
    ProjectMeasuresQuery query = new ProjectMeasuresQuery()
      .addMetricCriterion(new MetricCriterion("unknown", GT, 10d))
      .setSort("debt");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unknown metric(s) [debt, unknown]");
    underTest.validate(dbSession, query);
  }

  @Test
  public void return_all_unknown_metrics() throws Exception {
    insertValidMetric("ncloc");
    ProjectMeasuresQuery query = new ProjectMeasuresQuery()
      .addMetricCriterion(new MetricCriterion("debt", GT, 10d))
      .addMetricCriterion(new MetricCriterion("ncloc", LTE, 20d))
      .addMetricCriterion(new MetricCriterion("coverage", GT, 30d))
      .setSort("duplications");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unknown metric(s) [coverage, debt, duplications]");
    underTest.validate(dbSession, query);
  }

  private void insertValidMetric(String metricKey) {
    insertMetric(createValidMetric(metricKey));
  }

  private void insertMetric(MetricDto metricDto) {
    dbClient.metricDao().insert(dbSession, metricDto);
  }

  private static MetricDto createValidMetric(String metricKey) {
    return newMetricDto().setKey(metricKey).setValueType(INT.name()).setEnabled(true).setHidden(false);
  }
}
