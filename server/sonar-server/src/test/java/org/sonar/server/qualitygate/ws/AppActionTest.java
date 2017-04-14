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
package org.sonar.server.qualitygate.ws;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsQualityGates.AppWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.Metric.ValueType.BOOL;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.api.measures.Metric.ValueType.WORK_DUR;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.MediaTypes.JSON;

public class AppActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private AppAction underTest = new AppAction(userSession, dbClient, defaultOrganizationProvider);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void return_metrics() throws Exception {
    dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("metric")
      .setShortName("Metric")
      .setDomain("General")
      .setValueType(BOOL.name())
      .setHidden(true));
    dbSession.commit();

    AppWsResponse response = executeRequest();

    List<AppWsResponse.Metric> metrics = response.getMetricsList();
    assertThat(metrics).hasSize(1);
    AppWsResponse.Metric metric = metrics.get(0);
    assertThat(metric.getKey()).isEqualTo("metric");
    assertThat(metric.getName()).isEqualTo("Metric");
    assertThat(metric.getDomain()).isEqualTo("General");
    assertThat(metric.getType()).isEqualTo(BOOL.name());
    assertThat(metric.getHidden()).isTrue();
  }

  @Test
  public void return_metric_without_domain() throws Exception {
    dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("metric")
      .setShortName("Metric")
      .setDomain(null)
      .setValueType(BOOL.name())
      .setHidden(true));
    dbSession.commit();

    AppWsResponse response = executeRequest();

    List<AppWsResponse.Metric> metrics = response.getMetricsList();
    assertThat(metrics).hasSize(1);
    AppWsResponse.Metric metric = metrics.get(0);
    assertThat(metric.getKey()).isEqualTo("metric");
    assertThat(metric.hasDomain()).isFalse();
  }

  @Test
  public void return_rating_metrics_only_from_core_metrics() throws Exception {
    insertMetrics(
      newMetricDto().setKey("reliability_rating").setValueType(RATING.name()).setHidden(false),
      newMetricDto().setKey("new_reliability_rating").setValueType(RATING.name()).setHidden(false),
      newMetricDto().setKey("sqale_rating").setValueType(RATING.name()).setHidden(false),
      newMetricDto().setKey("none_core_rating").setValueType(RATING.name()).setHidden(false));

    AppWsResponse response = executeRequest();

    assertThat(response.getMetricsList()).extracting(AppWsResponse.Metric::getKey).containsOnly(
      "reliability_rating", "new_reliability_rating", "sqale_rating");
  }

  @Test
  public void does_not_return_DISTRIB_metric() throws Exception {
    dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("function_complexity_distribution")
      .setShortName("Function Distribution / Complexity")
      .setDomain("Complexity")
      .setValueType(DISTRIB.name())
      .setHidden(false));
    dbSession.commit();

    AppWsResponse response = executeRequest();

    assertThat(response.getMetricsList()).isEmpty();
  }

  @Test
  public void does_not_return_DATA_metric() throws Exception {
    dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("ncloc_language_distribution")
      .setShortName("Lines of Code Per Language")
      .setDomain("Size")
      .setValueType(DATA.name())
      .setHidden(false));
    dbSession.commit();

    AppWsResponse response = executeRequest();

    assertThat(response.getMetricsList()).isEmpty();
  }

  @Test
  public void does_not_return_quality_gate_metric() throws Exception {
    dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("alert_status")
      .setShortName("Quality Gate Status")
      .setDomain("Releasability")
      .setValueType(INT.name())
      .setHidden(false));
    dbSession.commit();

    AppWsResponse response = executeRequest();

    assertThat(response.getMetricsList()).isEmpty();
  }

  @Test
  public void return_edit_to_false_when_not_quality_gate_permission() throws Exception {
    userSession.logIn();

    AppWsResponse response = executeRequest();

    assertThat(response.getEdit()).isFalse();
  }

  @Test
  public void return_edit_to_true_when_quality_gate_permission() throws Exception {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());

    AppWsResponse response = executeRequest();

    assertThat(response.getEdit()).isTrue();
  }

  @Test
  public void test_example_json_response() {
    dbClient.metricDao().insert(dbSession,
      newMetricDto()
        .setKey("accessors")
        .setShortName("Accessors")
        .setDomain("Size")
        .setValueType(INT.name())
        .setHidden(true),
      newMetricDto()
        .setKey("blocker_remediation_cost")
        .setShortName("Blocker Technical Debt")
        .setDomain("SQALE")
        .setValueType(WORK_DUR.name())
        .setHidden(false));
    dbSession.commit();

    String result = ws.newRequest()
      .setMediaType(JSON)
      .execute()
      .getInput();

    assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(result);
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).isEmpty();
  }

  private void insertMetrics(MetricDto... metricDtos) {
    for (MetricDto metricDto : metricDtos) {
      dbClient.metricDao().insert(dbSession, metricDto);
    }
    dbSession.commit();
  }

  private AppWsResponse executeRequest() {
    return ws.newRequest().executeProtobuf(AppWsResponse.class);
  }
}
