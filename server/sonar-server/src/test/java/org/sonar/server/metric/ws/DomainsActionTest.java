/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.metric.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.db.DbClient;
import org.sonar.db.metric.MetricDao;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;
import org.sonar.test.JsonAssert;

import static org.sonar.db.metric.MetricTesting.newMetricDto;

@Category(DbTests.class)
public class DomainsActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  WsTester ws;
  DbClient dbClient;
  DbSession dbSession;

  @Before
  public void setUp() {
    dbClient = new DbClient(db.database(), db.myBatis(), new MetricDao());
    dbSession = dbClient.openSession(false);
    ws = new WsTester(new MetricsWs(new DomainsAction(dbClient)));
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Test
  public void json_example_validated() throws Exception {
    insertNewMetricDto(newEnabledMetric("API Compatibility"));
    insertNewMetricDto(newEnabledMetric("Issues"));
    insertNewMetricDto(newEnabledMetric("Rules"));
    insertNewMetricDto(newEnabledMetric("Tests"));
    insertNewMetricDto(newEnabledMetric("Documentation"));
    insertNewMetricDto(newEnabledMetric(null));
    insertNewMetricDto(newEnabledMetric(""));
    insertNewMetricDto(newMetricDto().setDomain("Domain of Deactivated Metric").setEnabled(false));

    WsTester.Result result = ws.newGetRequest(MetricsWs.ENDPOINT, "domains").execute();

    JsonAssert.assertJson(result.outputAsString()).isSimilarTo(getClass().getResource("example-domains.json"));
  }

  private void insertNewMetricDto(MetricDto metric) {
    dbClient.metricDao().insert(dbSession, metric);
    dbSession.commit();
  }

  private MetricDto newEnabledMetric(String domain) {
    return newMetricDto().setDomain(domain).setEnabled(true);
  }
}
