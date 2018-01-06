/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.metric.ws;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.measure.custom.CustomMeasureTesting.newCustomMeasureDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;

public class DeleteActionTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private WsActionTester ws = new WsActionTester(new DeleteAction(dbClient, userSessionRule));

  @Test
  public void delete_by_keys() {
    loggedAsSystemAdministrator();
    insertCustomEnabledMetrics("key-1", "key-2", "key-3");

    newRequest().setParam("keys", "key-1, key-3").execute();

    List<MetricDto> disabledMetrics = db.getDbClient().metricDao().selectByKeys(db.getSession(), asList("key-1", "key-3"));
    assertThat(disabledMetrics).extracting("enabled").containsOnly(false);
    assertThat(db.getDbClient().metricDao().selectByKey(db.getSession(), "key-2").isEnabled()).isTrue();
  }

  @Test
  public void delete_by_id() {
    loggedAsSystemAdministrator();
    MetricDto metric = insertCustomMetric("custom-key");

    TestResponse response = newRequest().setParam("ids", String.valueOf(metric.getId())).execute();

    assertThat(db.getDbClient().metricDao().selectEnabled(db.getSession())).isEmpty();
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  public void does_not_delete_non_custom_metric() {
    loggedAsSystemAdministrator();
    db.getDbClient().metricDao().insert(db.getSession(), newCustomEnabledMetric("custom-key").setUserManaged(false));
    db.getSession().commit();

    newRequest().setParam("keys", "custom-key").execute();

    MetricDto metric = db.getDbClient().metricDao().selectByKey(db.getSession(), "custom-key");
    assertThat(metric.isEnabled()).isTrue();
  }

  @Test
  public void delete_associated_measures() {
    loggedAsSystemAdministrator();
    MetricDto metric = insertCustomMetric("custom-key");
    CustomMeasureDto customMeasure = newCustomMeasureDto().setMetricId(metric.getId());
    CustomMeasureDto undeletedCustomMeasure = newCustomMeasureDto().setMetricId(metric.getId() + 1);
    dbClient.customMeasureDao().insert(db.getSession(), customMeasure);
    dbClient.customMeasureDao().insert(db.getSession(), undeletedCustomMeasure);
    db.getSession().commit();

    newRequest().setParam("keys", "custom-key").execute();

    assertThat(dbClient.customMeasureDao().selectById(db.getSession(), customMeasure.getId())).isNull();
    assertThat(dbClient.customMeasureDao().selectById(db.getSession(), undeletedCustomMeasure.getId())).isNotNull();
  }

  @Test
  public void delete_associated_quality_gate_conditions() {
    loggedAsSystemAdministrator();
    MetricDto customMetric = insertCustomMetric("custom-key");
    MetricDto nonCustomMetric = insertMetric(newMetricDto().setEnabled(true).setUserManaged(false).setKey("non-custom"));
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    db.qualityGates().addCondition(qualityGate1, customMetric);
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    db.qualityGates().addCondition(qualityGate2, customMetric);
    db.qualityGates().addCondition(qualityGate2, nonCustomMetric);

    newRequest().setParam("keys", "custom-key").execute();

    assertThat(dbClient.gateConditionDao().selectForQualityGate(db.getSession(), qualityGate1.getId())).isEmpty();
    assertThat(dbClient.gateConditionDao().selectForQualityGate(db.getSession(), qualityGate2.getId()))
      .extracting(QualityGateConditionDto::getMetricId).containsOnly(nonCustomMetric.getId().longValue());
  }

  @Test
  public void fail_when_no_argument() {
    loggedAsSystemAdministrator();
    expectedException.expect(IllegalArgumentException.class);

    newRequest().execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_system_administrator() {
    userSessionRule.logIn().setNonSystemAdministrator();
    insertCustomEnabledMetrics("custom-key");

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    newRequest().setParam("keys", "key-1").execute();
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSessionRule.anonymous();
    insertCustomEnabledMetrics("custom-key");

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    newRequest().setParam("keys", "key-1").execute();
  }

  private MetricDto newCustomEnabledMetric(String key) {
    return newMetricDto().setEnabled(true).setUserManaged(true).setKey(key);
  }

  private void insertCustomEnabledMetrics(String... keys) {
    for (String key : keys) {
      db.getDbClient().metricDao().insert(db.getSession(), newCustomEnabledMetric(key));
    }
    db.getSession().commit();
  }

  private MetricDto insertCustomMetric(String key) {
    return insertMetric(newCustomEnabledMetric(key));
  }

  private MetricDto insertMetric(MetricDto metric) {
    db.getDbClient().metricDao().insert(db.getSession(), metric);
    db.getSession().commit();
    return metric;
  }

  private void loggedAsSystemAdministrator() {
    userSessionRule.logIn().setSystemAdministrator();
  }

  private TestRequest newRequest() {
    return ws.newRequest();
  }
}
