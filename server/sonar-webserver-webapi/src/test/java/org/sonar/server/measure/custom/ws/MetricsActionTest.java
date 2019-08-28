/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.measure.custom.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.measure.custom.CustomMeasureTesting.newCustomMeasureDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;

public class MetricsActionTest {
  private static final String DEFAULT_PROJECT_UUID = "project-uuid";
  private static final String DEFAULT_PROJECT_KEY = "project-key";

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private ComponentDto defaultProject;
  private MetricsAction underTest = new MetricsAction(dbClient, userSession, TestComponentFinder.from(db));
  private WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void setUp() throws Exception {
    defaultProject = insertDefaultProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, defaultProject);
  }

  @Test
  public void list_metrics() {
    insertCustomMetric("metric-key-1");
    insertCustomMetric("metric-key-2");
    insertCustomMetric("metric-key-3");

    String response = newRequest().getInput();

    assertThat(response).contains("metric-key-1", "metric-key-2", "metric-key-3");
  }

  @Test
  public void list_metrics_active_and_custom_only() {
    insertCustomMetric("metric-key-1");
    dbClient.metricDao().insert(dbSession, newMetricDto().setEnabled(true).setUserManaged(false).setKey("metric-key-2"));
    dbClient.metricDao().insert(dbSession, newMetricDto().setEnabled(false).setUserManaged(true).setKey("metric-key-3"));
    dbSession.commit();

    String response = newRequest().getInput();

    assertThat(response).contains("metric-key-1")
      .doesNotContain("metric-key-2")
      .doesNotContain("metric-key-3");
  }

  @Test
  public void list_metrics_where_no_existing_custom_measure() {
    MetricDto metric = insertCustomMetric("metric-key-1");
    insertCustomMetric("metric-key-2");
    insertProject("project-uuid-2", "project-key-2");

    CustomMeasureDto customMeasure = newCustomMeasureDto()
      .setComponentUuid(defaultProject.uuid())
      .setMetricId(metric.getId());
    dbClient.customMeasureDao().insert(dbSession, customMeasure);
    dbSession.commit();

    String response = newRequest().getInput();

    assertThat(response).contains("metric-key-2")
      .doesNotContain("metric-key-1");
  }

  @Test
  public void list_metrics_based_on_project_key() {
    MetricDto metric = insertCustomMetric("metric-key-1");
    insertCustomMetric("metric-key-2");
    insertProject("project-uuid-2", "project-key-2");

    CustomMeasureDto customMeasure = newCustomMeasureDto()
      .setComponentUuid(defaultProject.uuid())
      .setMetricId(metric.getId());
    dbClient.customMeasureDao().insert(dbSession, customMeasure);
    dbSession.commit();

    String response = tester.newRequest()
      .setParam(MetricsAction.PARAM_PROJECT_KEY, DEFAULT_PROJECT_KEY)
      .execute().getInput();

    assertThat(response).contains("metric-key-2")
      .doesNotContain("metric-key-1");
  }

  @Test
  public void list_metrics_as_a_project_admin() {
    insertCustomMetric("metric-key-1");
    userSession.logIn("login").addProjectPermission(UserRole.ADMIN, defaultProject);

    String response = newRequest().getInput();

    assertThat(response).contains("metric-key-1");
  }

  @Test
  public void response_with_correct_formatting() {
    dbClient.metricDao().insert(dbSession, newCustomMetric("custom-key-1")
      .setShortName("custom-name-1")
      .setDescription("custom-description-1")
      .setDomain("custom-domain-1")
      .setValueType(Metric.ValueType.INT.name())
      .setDirection(1)
      .setQualitative(false)
      .setHidden(false));
    dbClient.metricDao().insert(dbSession, newCustomMetric("custom-key-2")
      .setShortName("custom-name-2")
      .setDescription("custom-description-2")
      .setDomain("custom-domain-2")
      .setValueType(Metric.ValueType.INT.name())
      .setDirection(-1)
      .setQualitative(true)
      .setHidden(true));
    dbClient.metricDao().insert(dbSession, newCustomMetric("custom-key-3")
      .setShortName("custom-name-3")
      .setDescription("custom-description-3")
      .setDomain("custom-domain-3")
      .setValueType(Metric.ValueType.INT.name())
      .setDirection(0)
      .setQualitative(false)
      .setHidden(false));
    dbSession.commit();

    TestResponse response = tester.newRequest()
      .setParam(MetricsAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .execute();

    response.assertJson(getClass(), "metrics.json");
  }

  @Test
  public void fail_if_project_id_nor_project_key_provided() {
    expectedException.expect(IllegalArgumentException.class);

    tester.newRequest().execute();
  }

  @Test
  public void fail_if_insufficient_privilege() {
    expectedException.expect(ForbiddenException.class);
    userSession.logIn("login");

    insertCustomMetric("metric-key-1");

    newRequest();
  }

  private TestResponse newRequest() {
    return tester.newRequest()
      .setParam(MetricsAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .execute();
  }

  private MetricDto insertCustomMetric(String metricKey) {
    MetricDto metric = newCustomMetric(metricKey);
    dbClient.metricDao().insert(dbSession, metric);
    dbSession.commit();

    return metric;
  }

  private static MetricDto newCustomMetric(String metricKey) {
    return newMetricDto().setEnabled(true).setUserManaged(true).setKey(metricKey);
  }

  private ComponentDto insertDefaultProject() {
    return insertProject(DEFAULT_PROJECT_UUID, DEFAULT_PROJECT_KEY);
  }

  private ComponentDto insertProject(String projectUuid, String projectKey) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization(), projectUuid)
      .setDbKey(projectKey);
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();

    return project;
  }

}
