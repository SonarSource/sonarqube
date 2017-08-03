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
package org.sonar.server.measure.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsMeasures.ComponentWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_COMPONENT;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_DEVELOPER_ID;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_KEYS;

public class ComponentActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private WsActionTester ws = new WsActionTester(new ComponentAction(db.getDbClient(), TestComponentFinder.from(db), userSession));

  @Test
  public void test_definition_of_web_service() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("5.4");
    assertThat(def.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("componentId", "component", "metricKeys", "additionalFields", "developerId", "developerKey");

    assertThat(def.param("developerId").deprecatedSince()).isEqualTo("6.4");
    assertThat(def.param("developerKey").deprecatedSince()).isEqualTo("6.4");
    assertThat(def.param("componentId").deprecatedSince()).isEqualTo("6.6");
  }

  @Test
  public void provided_project() {
    ComponentDto project = db.components().insertPrivateProject();
    logAsUser(project);
    insertNclocMetric();

    ComponentWsResponse response = newRequest(project.getKey(), "ncloc");

    assertThat(response.getMetrics().getMetricsCount()).isEqualTo(1);
    assertThat(response.getPeriods().getPeriodsCount()).isEqualTo(0);
    assertThat(response.getComponent().getKey()).isEqualTo(project.getDbKey());
  }

  @Test
  public void without_additional_fields() {
    ComponentDto project = db.components().insertPrivateProject();
    logAsUser(project);
    db.components().insertSnapshot(project);
    insertNclocMetric();

    String response = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .execute().getInput();

    assertThat(response)
      .doesNotContain("periods")
      .doesNotContain("metrics");
  }

  @Test
  public void reference_uuid_in_the_response() {
    userSession.logIn().setRoot();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto view = db.components().insertView();
    db.components().insertSnapshot(view);
    ComponentDto projectCopy = db.components().insertComponent(newProjectCopy("project-uuid-copy", project, view));
    insertNclocMetric();

    ComponentWsResponse response = newRequest(projectCopy.getKey(), "ncloc");

    assertThat(response.getComponent().getRefId()).isEqualTo(project.uuid());
    assertThat(response.getComponent().getRefKey()).isEqualTo(project.getKey());
  }

  @Test
  public void return_deprecated_id_in_the_response() {
    ComponentDto project = db.components().insertPrivateProject();
    logAsUser(project);
    db.components().insertSnapshot(project);
    insertNclocMetric();

    ComponentWsResponse response = newRequest(project.getKey(), "ncloc");

    assertThat(response.getComponent().getId()).isEqualTo(project.uuid());
  }

  @Test
  public void use_deprecated_component_id_parameter() {
    ComponentDto project = db.components().insertPrivateProject();
    logAsUser(project);
    userSession.addProjectPermission(USER, project);
    insertNclocMetric();

    ComponentWsResponse response = ws.newRequest()
      .setParam("componentId", project.uuid())
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .executeProtobuf(ComponentWsResponse.class);

    assertThat(response.getComponent().getKey()).isEqualTo(project.getDbKey());
  }

  @Test
  public void use_deprecated_component_key_parameter() {
    ComponentDto project = db.components().insertPrivateProject();
    logAsUser(project);
    userSession.addProjectPermission(USER, project);
    insertNclocMetric();

    ComponentWsResponse response = ws.newRequest()
      .setParam("componentKey", project.getKey())
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .executeProtobuf(ComponentWsResponse.class);

    assertThat(response.getComponent().getKey()).isEqualTo(project.getDbKey());
  }

  @Test
  public void fail_when_developer_is_not_found() {
    ComponentDto project = db.components().insertPrivateProject();
    logAsUser(project);
    db.components().insertSnapshot(project);

    insertNclocMetric();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'unknown-developer-id' not found");

    ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .setParam(PARAM_DEVELOPER_ID, "unknown-developer-id").executeProtobuf(ComponentWsResponse.class);
  }

  @Test
  public void fail_when_a_metric_is_not_found() {
    ComponentDto project = db.components().insertPrivateProject();
    logAsUser(project);
    db.components().insertSnapshot(project);
    insertNclocMetric();
    insertComplexityMetric();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("The following metric keys are not found: unknown-metric, another-unknown-metric");

    newRequest(project.getKey(), "ncloc, complexity, unknown-metric, another-unknown-metric");
  }

  @Test
  public void fail_when_empty_metric_keys_parameter() {
    ComponentDto project = db.components().insertPrivateProject();
    logAsUser(project);
    db.components().insertSnapshot(project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("At least one metric key must be provided");

    newRequest(project.getKey(), "");
  }

  @Test
  public void fail_when_not_enough_permission() {
    userSession.logIn();
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertSnapshot(project);
    insertNclocMetric();

    expectedException.expect(ForbiddenException.class);

    newRequest(project.getKey(), "ncloc");
  }

  @Test
  public void fail_when_component_does_not_exist() {
    insertNclocMetric();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'project-key' not found");

    ws.newRequest()
      .setParam(PARAM_COMPONENT, "project-key")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .execute();
  }

  @Test
  public void fail_when_component_is_removed() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setEnabled(false));
    logAsUser(project);
    userSession.addProjectPermission(USER, project);
    insertNclocMetric();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(String.format("Component key '%s' not found", project.getKey()));

    ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .execute();
  }

  @Test
  public void json_example() {
    ComponentDto project = db.components().insertPrivateProject();
    logAsUser(project);
    SnapshotDto analysis = db.components().insertSnapshot(project,
      s -> s.setPeriodDate(parseDateTime("2016-01-11T10:49:50+0100").getTime())
        .setPeriodMode("previous_version")
        .setPeriodParam("1.0-SNAPSHOT"));
    ComponentDto file = db.components().insertComponent(newFileDto(project)
      .setDbKey("MY_PROJECT:ElementImpl.java")
      .setName("ElementImpl.java")
      .setLanguage("java")
      .setPath("src/main/java/com/sonarsource/markdown/impl/ElementImpl.java"));
    MetricDto complexity = insertComplexityMetric();
    db.measureDbTester().insertMeasure(file, analysis, complexity,
      m -> m.setValue(12.0d)
        .setVariation(2.0d)
        .setData(null));
    MetricDto ncloc = insertNclocMetric();
    db.measureDbTester().insertMeasure(file, analysis, ncloc,
      m -> m.setValue(114.0d)
        .setVariation(3.0d)
        .setData(null));
    MetricDto newViolations = insertNewViolationMetric();
    db.measureDbTester().insertMeasure(file, analysis, newViolations,
      m -> m.setVariation(25.0d)
        .setValue(null)
        .setData(null));

    String response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_METRIC_KEYS, "ncloc, complexity, new_violations")
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics,periods")
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("component-example.json"));
  }

  private ComponentWsResponse newRequest(String componentKey, String metricKeys) {
    return ws.newRequest()
      .setParam(PARAM_COMPONENT, componentKey)
      .setParam(PARAM_METRIC_KEYS, metricKeys)
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics,periods")
      .executeProtobuf(ComponentWsResponse.class);
  }

  private MetricDto insertNclocMetric() {
    return db.measureDbTester().insertMetric(m -> m.setKey("ncloc")
      .setShortName("Lines of code")
      .setDescription("Non Commenting Lines of Code")
      .setDomain("Size")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(false)
      .setHidden(false)
      .setUserManaged(false));
  }

  private MetricDto insertComplexityMetric() {
    return db.measureDbTester().insertMetric(m -> m.setKey("complexity")
      .setShortName("Complexity")
      .setDescription("Cyclomatic complexity")
      .setDomain("Complexity")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(false)
      .setHidden(false)
      .setUserManaged(false));
  }

  private MetricDto insertNewViolationMetric() {
    return db.measureDbTester().insertMetric(m -> m.setKey("new_violations")
      .setShortName("New issues")
      .setDescription("New Issues")
      .setDomain("Issues")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(true)
      .setHidden(false)
      .setUserManaged(false));
  }

  private void logAsUser(ComponentDto project) {
    userSession.addProjectPermission(UserRole.USER, project);
  }

}
