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

import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsQualityGates.ProjectStatusWsResponse;
import org.sonarqube.ws.WsQualityGates.ProjectStatusWsResponse.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.measure.MeasureTesting.newMeasureDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ANALYSIS_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_KEY;

public class ProjectStatusActionTest {
  private static final String ANALYSIS_ID = "task-uuid";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private WsActionTester ws;
  private DbClient dbClient;
  private DbSession dbSession;

  @Before
  public void setUp() {
    dbClient = db.getDbClient();
    dbSession = db.getSession();

    ws = new WsActionTester(new ProjectStatusAction(dbClient, TestComponentFinder.from(db), userSession));
  }

  @Test
  public void json_example() throws IOException {
    ComponentDto project = db.components().insertPrivateProject(db.organizations().insert());
    userSession.addProjectPermission(UserRole.USER, project);

    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project)
      .setPeriodMode("last_version")
      .setPeriodParam("2015-12-07")
      .setPeriodDate(956789123987L));
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setEnabled(true)
      .setKey(CoreMetrics.QUALITY_GATE_DETAILS_KEY));
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(metric, project, snapshot)
        .setData(IOUtils.toString(getClass().getResource("ProjectStatusActionTest/measure_data.json"))));
    dbSession.commit();

    String response = ws.newRequest()
      .setParam("analysisId", snapshot.getUuid())
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("project_status-example.json"));
  }

  @Test
  public void return_status_by_project_id() throws IOException {
    ComponentDto project = db.components().insertPrivateProject(db.organizations().insert());
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project)
      .setPeriodMode("last_version")
      .setPeriodParam("2015-12-07")
      .setPeriodDate(956789123987L));
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setEnabled(true)
      .setKey(CoreMetrics.QUALITY_GATE_DETAILS_KEY));
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(metric, project, snapshot)
        .setData(IOUtils.toString(getClass().getResource("ProjectStatusActionTest/measure_data.json"))));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.USER, project);

    String response = ws.newRequest()
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("project_status-example.json"));
  }

  @Test
  public void return_status_by_project_key() throws IOException {
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert()).setKey("project-key"));
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project)
      .setPeriodMode("last_version")
      .setPeriodParam("2015-12-07")
      .setPeriodDate(956789123987L));
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setEnabled(true)
      .setKey(CoreMetrics.QUALITY_GATE_DETAILS_KEY));
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(metric, project, snapshot)
        .setData(IOUtils.toString(getClass().getResource("ProjectStatusActionTest/measure_data.json"))));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.USER, project);

    String response = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "project-key")
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("project_status-example.json"));
  }

  @Test
  public void return_undefined_status_if_measure_is_not_found() {
    ComponentDto project = db.components().insertPrivateProject(db.organizations().insert());
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.USER, project);

    ProjectStatusWsResponse result = call(snapshot.getUuid());

    assertThat(result.getProjectStatus().getStatus()).isEqualTo(Status.NONE);
    assertThat(result.getProjectStatus().getConditionsCount()).isEqualTo(0);
  }

  @Test
  public void return_undefined_status_if_snapshot_is_not_found() {
    ComponentDto project = db.components().insertPrivateProject(db.organizations().insert());
    userSession.addProjectPermission(UserRole.USER, project);

    ProjectStatusWsResponse result = callByProjectUuid(project.uuid());

    assertThat(result.getProjectStatus().getStatus()).isEqualTo(Status.NONE);
    assertThat(result.getProjectStatus().getConditionsCount()).isEqualTo(0);
  }

  @Test
  public void project_administrator_is_allowed_to_get_project_status() {
    ComponentDto project = db.components().insertPrivateProject(db.organizations().insert());
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.ADMIN, project);

    call(snapshot.getUuid());
  }

  @Test
  public void project_user_is_allowed_to_get_project_status() {
    ComponentDto project = db.components().insertPrivateProject(db.organizations().insert());
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.USER, project);

    call(snapshot.getUuid());
  }

  @Test
  public void fail_if_no_snapshot_id_found() {
    logInAsSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Analysis with id 'task-uuid' is not found");

    call(ANALYSIS_ID);
  }

  @Test
  public void fail_if_insufficient_privileges() {
    ComponentDto project = db.components().insertPrivateProject(db.organizations().insert());
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project));
    dbSession.commit();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    call(snapshot.getUuid());
  }

  @Test
  public void fail_if_project_id_and_ce_task_id_provided() {
    logInAsSystemAdministrator();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("One (and only one) of the following parameters must be provided 'analysisId', 'projectId', 'projectKey'");

    ws.newRequest()
      .setParam(PARAM_ANALYSIS_ID, "analysis-id")
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .execute().getInput();
  }

  @Test
  public void fail_if_no_parameter_provided() {
    logInAsSystemAdministrator();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("One (and only one) of the following parameters must be provided 'analysisId', 'projectId', 'projectKey'");

    ws.newRequest().execute().getInput();
  }

  private ProjectStatusWsResponse call(String taskId) {
    return ws.newRequest()
      .setParam("analysisId", taskId)
      .executeProtobuf(ProjectStatusWsResponse.class);
  }

  private ProjectStatusWsResponse callByProjectUuid(String projectUuid) {
    return ws.newRequest()
      .setParam(PARAM_PROJECT_ID, projectUuid)
      .executeProtobuf(ProjectStatusWsResponse.class);
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }
}
