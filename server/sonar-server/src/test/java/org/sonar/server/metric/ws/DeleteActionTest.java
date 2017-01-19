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
package org.sonar.server.metric.ws;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.measure.custom.CustomMeasureTesting;
import org.sonar.db.metric.MetricDao;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.metric.MetricTesting.newMetricDto;

public class DeleteActionTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  DbClient dbClient = db.getDbClient();
  final DbSession dbSession = db.getSession();
  MetricDao metricDao;

  WsTester ws;

  @Before
  public void setUp() {
    userSessionRule.login("login").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    ws = new WsTester(new MetricsWs(new DeleteAction(dbClient, userSessionRule)));
    metricDao = dbClient.metricDao();
  }

  @Test
  public void delete_by_keys() throws Exception {
    insertCustomEnabledMetrics(1, 2, 3);
    dbSession.commit();

    newRequest().setParam("keys", "key-1, key-3").execute();
    dbSession.commit();

    List<MetricDto> disabledMetrics = metricDao.selectByKeys(dbSession, Arrays.asList("key-1", "key-3"));
    assertThat(disabledMetrics).extracting("enabled").containsOnly(false);
    assertThat(metricDao.selectByKey(dbSession, "key-2").isEnabled()).isTrue();
  }

  @Test
  public void delete_by_id() throws Exception {
    MetricDto metric = newCustomEnabledMetric(1);
    metricDao.insert(dbSession, metric);
    dbSession.commit();

    WsTester.Result result = newRequest().setParam("ids", String.valueOf(metric.getId())).execute();
    dbSession.commit();

    assertThat(metricDao.selectEnabled(dbSession)).isEmpty();
    result.assertNoContent();
  }

  @Test
  public void do_not_delete_non_custom_metric() throws Exception {
    metricDao.insert(dbSession, newCustomEnabledMetric(1).setUserManaged(false));
    dbSession.commit();

    newRequest().setParam("keys", "key-1").execute();
    dbSession.commit();

    MetricDto metric = metricDao.selectByKey(dbSession, "key-1");
    assertThat(metric.isEnabled()).isTrue();
  }

  @Test
  public void delete_associated_measures() throws Exception {
    MetricDto metric = newCustomEnabledMetric(1);
    metricDao.insert(dbSession, metric);
    CustomMeasureDto customMeasure = CustomMeasureTesting.newCustomMeasureDto().setMetricId(metric.getId());
    CustomMeasureDto undeletedCustomMeasure = CustomMeasureTesting.newCustomMeasureDto().setMetricId(metric.getId() + 1);
    dbClient.customMeasureDao().insert(dbSession, customMeasure);
    dbClient.customMeasureDao().insert(dbSession, undeletedCustomMeasure);
    dbSession.commit();

    newRequest().setParam("keys", "key-1").execute();

    assertThat(dbClient.customMeasureDao().selectById(dbSession, customMeasure.getId())).isNull();
    assertThat(dbClient.customMeasureDao().selectById(dbSession, undeletedCustomMeasure.getId())).isNotNull();
  }

  @Test
  public void fail_when_no_argument() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    newRequest().execute();
  }

  @Test
  public void fail_when_insufficient_privileges() throws Exception {
    expectedException.expect(ForbiddenException.class);

    userSessionRule.setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    insertCustomEnabledMetrics(1);

    newRequest().setParam("keys", "key-1").execute();
  }

  private MetricDto newCustomEnabledMetric(int id) {
    return newMetricDto().setEnabled(true).setUserManaged(true).setKey("key-" + id);
  }

  private void insertCustomEnabledMetrics(int... ids) {
    for (int id : ids) {
      metricDao.insert(dbSession, newCustomEnabledMetric(id));
    }

    dbSession.commit();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest(MetricsWs.ENDPOINT, "delete");
  }
}
