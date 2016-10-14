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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricTesting;

@Ignore
public class SearchProjectsQueryBuilderValidatorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();

  SearchProjectsQueryBuilderValidator validator = new SearchProjectsQueryBuilderValidator(dbClient);

  @Test
  public void does_not_fail_when_metric_criteria_contains_an_existing_metric() throws Exception {
    insertMetric("ncloc");

    validator.validate(dbSession, SearchProjectsQueryBuilder.build("ncloc > 10"));
  }

  @Test
  public void fail_when_metric_does_not_exists() throws Exception {
    insertMetric("ncloc");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unknown metric(s) [unknown]");
    validator.validate(dbSession, SearchProjectsQueryBuilder.build("unknown > 10"));
  }

  @Test
  public void return_all_unknown_metrics() throws Exception {
    insertMetric("ncloc");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unknown metric(s) [coverage, debt]");
    validator.validate(dbSession, SearchProjectsQueryBuilder.build("debt > 10 AND ncloc <= 20 AND coverage > 30"));
  }

  private void insertMetric(String metricKey) {
    dbClient.metricDao().insert(dbSession, MetricTesting.newMetricDto().setKey(metricKey));
  }
}
