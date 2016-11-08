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
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.emptySet;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.api.measures.Metric.ValueType.WORK_DUR;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.server.component.ws.ProjectMeasuresQueryFactory.newProjectMeasuresQuery;

public class ProjectMeasuresQueryValidatorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private ProjectMeasuresQueryValidator underTest = new ProjectMeasuresQueryValidator(dbClient);

  @Test
  public void query_with_empty_metrics_is_valid() throws Exception {
    underTest.validate(dbSession, newProjectMeasuresQuery("", emptySet()));
  }

  @Test
  public void does_not_fail_when_metric_criteria_contains_an_existing_metric() throws Exception {
    insertValidMetric("ncloc");

    underTest.validate(dbSession, newProjectMeasuresQuery("ncloc > 10", emptySet()));
  }

  @Test
  public void fail_when_metric_are_not_numeric() throws Exception {
    insertMetric(createValidMetric("ncloc").setValueType(INT.name()));
    insertMetric(createValidMetric("debt").setValueType(WORK_DUR.name()));
    insertMetric(createValidMetric("data").setValueType(DATA.name()));
    insertMetric(createValidMetric("distrib").setValueType(DISTRIB.name()));
    insertMetric(createValidMetric("string").setValueType(STRING.name()));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Following metrics are not numeric : [data, distrib, string]");
    underTest.validate(dbSession, newProjectMeasuresQuery("data > 10 and distrib = 11 and ncloc <= 20 and debt < 30 and string = 40", emptySet()));
  }

  @Test
  public void fail_when_metric_is_disabled() throws Exception {
    insertMetric(createValidMetric("ncloc").setEnabled(false));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Following metrics are disabled : [ncloc]");
    underTest.validate(dbSession, newProjectMeasuresQuery("ncloc > 10", emptySet()));
  }

  @Test
  public void fail_when_metric_does_not_exists() throws Exception {
    insertValidMetric("ncloc");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unknown metric(s) [unknown]");
    underTest.validate(dbSession, newProjectMeasuresQuery("unknown > 10", emptySet()));
  }

  @Test
  public void return_all_unknown_metrics() throws Exception {
    insertValidMetric("ncloc");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unknown metric(s) [coverage, debt]");
    underTest.validate(dbSession, newProjectMeasuresQuery("debt > 10 AND ncloc <= 20 AND coverage > 30", emptySet()));
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
