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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsMeasures.ComponentWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.measure.MeasureTesting.newMeasureDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_DEVELOPER_ID;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_KEYS;

public class ComponentActionTest {
  private static final String PROJECT_UUID = "project-uuid";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  ComponentDbTester componentDb = new ComponentDbTester(db);
  DbClient dbClient = db.getDbClient();
  final DbSession dbSession = db.getSession();

  WsActionTester ws = new WsActionTester(new ComponentAction(dbClient, new ComponentFinder(dbClient), userSession));

  @Before
  public void setUp() {
    userSession.logIn().setRoot();
  }

  @Test
  public void json_example() {
    insertJsonExampleData();

    String response = ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, "AVIwDXE-bJbJqrw6wFv5")
      .setParam(PARAM_METRIC_KEYS, "ncloc, complexity, new_violations")
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics,periods")
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("component-example.json"));
  }

  @Test
  public void provided_project() {
    ComponentDto project = componentDb.insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), PROJECT_UUID));
    userSession.addProjectPermission(UserRole.USER, project);
    insertNclocMetric();

    ComponentWsResponse response = newRequest(PROJECT_UUID, "ncloc");

    assertThat(response.getMetrics().getMetricsCount()).isEqualTo(1);
    assertThat(response.getPeriods().getPeriodsCount()).isEqualTo(0);
    assertThat(response.getComponent().getId()).isEqualTo(PROJECT_UUID);
  }

  @Test
  public void without_additional_fields() {
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.organizations().insert(), "project-uuid"));
    insertNclocMetric();

    String response = ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .execute().getInput();

    assertThat(response)
      .doesNotContain("periods")
      .doesNotContain("metrics");
  }

  @Test
  public void reference_uuid_in_the_response() {
    ComponentDto project = newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid").setKey("project-key");
    componentDb.insertProjectAndSnapshot(project);
    ComponentDto view = newView(db.getDefaultOrganization(), "view-uuid");
    componentDb.insertViewAndSnapshot(view);
    componentDb.insertComponent(newProjectCopy("project-uuid-copy", project, view));
    insertNclocMetric();

    ComponentWsResponse response = newRequest("project-uuid-copy", "ncloc");

    assertThat(response.getComponent().getId()).isEqualTo("project-uuid-copy");
    assertThat(response.getComponent().getRefId()).isEqualTo("project-uuid");
    assertThat(response.getComponent().getRefKey()).isEqualTo("project-key");
  }

  @Test
  public void fail_when_developer_is_not_found() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'unknown-developer-id' not found");

    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), PROJECT_UUID));
    insertNclocMetric();

    ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, PROJECT_UUID)
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .setParam(PARAM_DEVELOPER_ID, "unknown-developer-id").executeProtobuf(ComponentWsResponse.class);
  }

  @Test
  public void fail_when_a_metric_is_not_found() {
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.organizations().insert(), PROJECT_UUID));
    insertNclocMetric();
    insertComplexityMetric();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("The following metric keys are not found: unknown-metric, another-unknown-metric");

    newRequest(PROJECT_UUID, "ncloc, complexity, unknown-metric, another-unknown-metric");
  }

  @Test
  public void fail_when_empty_metric_keys_parameter() {
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), PROJECT_UUID));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("At least one metric key must be provided");

    newRequest(PROJECT_UUID, "");
  }

  @Test
  public void fail_when_not_enough_permission() {
    userSession.logIn();
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.organizations().insert(), PROJECT_UUID));
    insertNclocMetric();

    expectedException.expect(ForbiddenException.class);

    newRequest(PROJECT_UUID, "ncloc");
  }

  private ComponentWsResponse newRequest(String componentUuid, String metricKeys) {
    return ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, componentUuid)
      .setParam(PARAM_METRIC_KEYS, metricKeys)
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics,periods").executeProtobuf(ComponentWsResponse.class);
  }

  private static MetricDto newMetricDtoWithoutOptimization() {
    return newMetricDto()
      .setWorstValue(null)
      .setOptimizedBestValue(false)
      .setBestValue(null)
      .setUserManaged(false);
  }

  private MetricDto insertNclocMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
      .setKey("ncloc")
      .setShortName("Lines of code")
      .setDescription("Non Commenting Lines of Code")
      .setDomain("Size")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(false)
      .setHidden(false)
      .setUserManaged(false));
    db.commit();
    return metric;
  }

  private MetricDto insertComplexityMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
      .setKey("complexity")
      .setShortName("Complexity")
      .setDescription("Cyclomatic complexity")
      .setDomain("Complexity")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(false)
      .setHidden(false)
      .setUserManaged(false));
    db.commit();
    return metric;
  }

  private MetricDto insertNewViolationMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
      .setKey("new_violations")
      .setShortName("New issues")
      .setDescription("New Issues")
      .setDomain("Issues")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(true)
      .setHidden(false)
      .setUserManaged(false));
    db.commit();
    return metric;
  }

  private void insertJsonExampleData() {
    ComponentDto project = newPrivateProjectDto(db.getDefaultOrganization(), PROJECT_UUID);
    SnapshotDto projectSnapshot = SnapshotTesting.newAnalysis(project)
      .setPeriodDate(parseDateTime("2016-01-11T10:49:50+0100").getTime())
      .setPeriodMode("previous_version")
      .setPeriodParam("1.0-SNAPSHOT");
    ComponentDto file = newFileDto(project, null)
      .setUuid("AVIwDXE-bJbJqrw6wFv5")
      .setKey("MY_PROJECT:ElementImpl.java")
      .setName("ElementImpl.java")
      .setQualifier(Qualifiers.FILE)
      .setLanguage("java")
      .setPath("src/main/java/com/sonarsource/markdown/impl/ElementImpl.java");
    componentDb.insertComponents(project, file);
    dbClient.snapshotDao().insert(dbSession, projectSnapshot);

    MetricDto complexity = insertComplexityMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(complexity, file, projectSnapshot)
        .setValue(12.0d)
        .setVariation(2.0d));

    MetricDto ncloc = insertNclocMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(ncloc, file, projectSnapshot)
        .setValue(114.0d)
        .setVariation(3.0d));

    MetricDto newViolations = insertNewViolationMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(newViolations, file, projectSnapshot)
        .setVariation(25.0d));
    db.commit();
  }

}
