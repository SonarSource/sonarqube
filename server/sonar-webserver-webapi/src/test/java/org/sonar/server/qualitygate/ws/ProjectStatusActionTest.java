/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualitygate.QualityGateCaycChecker;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.ProjectStatusResponse;
import org.sonarqube.ws.Qualitygates.ProjectStatusResponse.Status;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.measure.MeasureTesting.newLiveMeasure;
import static org.sonar.db.measure.MeasureTesting.newMeasureDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.COMPLIANT;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.NON_COMPLIANT;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ANALYSIS_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_BRANCH;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PROJECT_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.test.JsonAssert.assertJson;

public class ProjectStatusActionTest {
  private static final String ANALYSIS_ID = "task-uuid";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final QualityGateCaycChecker qualityGateCaycChecker = mock(QualityGateCaycChecker.class);

  private final WsActionTester ws = new WsActionTester(new ProjectStatusAction(dbClient, TestComponentFinder.from(db), userSession, qualityGateCaycChecker));

  @Before
  public void setUp() {
    when(qualityGateCaycChecker.checkCaycCompliantFromProject(any(), any())).thenReturn(NON_COMPLIANT);
  }

  @Test
  public void test_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("analysisId", false),
        tuple("projectKey", false),
        tuple("projectId", false),
        tuple("branch", false),
        tuple("pullRequest", false));
  }

  @Test
  public void test_json_example() throws IOException {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto gateDetailsMetric = insertGateDetailMetric();

    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project)
      .setPeriodMode("last_version")
      .setPeriodParam("2015-12-07")
      .setPeriodDate(956789123987L));
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(gateDetailsMetric, project, snapshot)
        .setData(IOUtils.toString(getClass().getResource("ProjectStatusActionTest/measure_data.json"), StandardCharsets.UTF_8)));
    dbSession.commit();

    String response = ws.newRequest()
      .setParam("analysisId", snapshot.getUuid())
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("project_status-example.json"));
  }

  @Test
  public void return_past_status_when_project_is_referenced_by_past_analysis_id() throws IOException {
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto pastAnalysis = dbClient.snapshotDao().insert(dbSession, newAnalysis(project)
      .setLast(false)
      .setPeriodMode("last_version")
      .setPeriodParam("2015-12-07")
      .setPeriodDate(956789123987L));
    SnapshotDto lastAnalysis = dbClient.snapshotDao().insert(dbSession, newAnalysis(project)
      .setLast(true)
      .setPeriodMode("last_version")
      .setPeriodParam("2016-12-07")
      .setPeriodDate(1_500L));
    MetricDto gateDetailsMetric = insertGateDetailMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(gateDetailsMetric, project, pastAnalysis)
        .setData(IOUtils.toString(getClass().getResource("ProjectStatusActionTest/measure_data.json"))));
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(gateDetailsMetric, project, lastAnalysis)
        .setData("not_used"));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.USER, project);

    String response = ws.newRequest()
      .setParam(PARAM_ANALYSIS_ID, pastAnalysis.getUuid())
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("project_status-example.json"));
  }

  @Test
  public void return_live_status_when_project_is_referenced_by_its_id() throws IOException {
    ComponentDto project = db.components().insertPrivateProject();
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project)
      .setPeriodMode("last_version")
      .setPeriodParam("2015-12-07")
      .setPeriodDate(956789123987L));
    MetricDto gateDetailsMetric = insertGateDetailMetric();
    dbClient.liveMeasureDao().insert(dbSession,
      newLiveMeasure(project, gateDetailsMetric)
        .setData(IOUtils.toString(getClass().getResource("ProjectStatusActionTest/measure_data.json"))));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.USER, project);

    String response = ws.newRequest()
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("project_status-example.json"));
  }

  @Test
  public void return_past_status_when_branch_is_referenced_by_past_analysis_id() throws IOException {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project);
    SnapshotDto pastAnalysis = dbClient.snapshotDao().insert(dbSession, newAnalysis(branch)
      .setLast(false)
      .setPeriodMode("last_version")
      .setPeriodParam("2015-12-07")
      .setPeriodDate(956789123987L));
    SnapshotDto lastAnalysis = dbClient.snapshotDao().insert(dbSession, newAnalysis(branch)
      .setLast(true)
      .setPeriodMode("last_version")
      .setPeriodParam("2016-12-07")
      .setPeriodDate(1_500L));
    MetricDto gateDetailsMetric = insertGateDetailMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(gateDetailsMetric, branch, pastAnalysis)
        .setData(IOUtils.toString(getClass().getResource("ProjectStatusActionTest/measure_data.json"))));
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(gateDetailsMetric, branch, lastAnalysis)
        .setData("not_used"));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.USER, project);

    String response = ws.newRequest()
      .setParam(PARAM_ANALYSIS_ID, pastAnalysis.getUuid())
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("project_status-example.json"));
  }

  @Test
  public void return_live_status_when_project_is_referenced_by_its_key() throws IOException {
    ComponentDto project = db.components().insertPrivateProject();
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project)
      .setPeriodMode("last_version")
      .setPeriodParam("2015-12-07")
      .setPeriodDate(956789123987L));
    MetricDto gateDetailsMetric = insertGateDetailMetric();
    dbClient.liveMeasureDao().insert(dbSession,
      newLiveMeasure(project, gateDetailsMetric)
        .setData(IOUtils.toString(getClass().getResource("ProjectStatusActionTest/measure_data.json"))));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.USER, project);

    String response = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("project_status-example.json"));
  }

  @Test
  public void return_live_status_when_branch_is_referenced_by_its_key() throws IOException {
    ComponentDto project = db.components().insertPrivateProject();
    String branchName = randomAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(branchName));

    dbClient.snapshotDao().insert(dbSession, newAnalysis(branch)
      .setPeriodMode("last_version")
      .setPeriodParam("2015-12-07")
      .setPeriodDate(956789123987L));
    MetricDto gateDetailsMetric = insertGateDetailMetric();
    dbClient.liveMeasureDao().insert(dbSession,
      newLiveMeasure(branch, gateDetailsMetric)
        .setData(IOUtils.toString(getClass().getResource("ProjectStatusActionTest/measure_data.json"))));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.USER, project);

    String response = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_BRANCH, branchName)
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("project_status-example.json"));
  }

  @Test
  public void return_live_status_when_pull_request_is_referenced_by_its_key() throws IOException {
    ComponentDto project = db.components().insertPrivateProject();
    String pullRequestKey = RandomStringUtils.randomAlphanumeric(100);
    ComponentDto pr = db.components().insertProjectBranch(project, branch -> branch.setBranchType(BranchType.PULL_REQUEST)
      .setKey(pullRequestKey));

    dbClient.snapshotDao().insert(dbSession, newAnalysis(pr)
      .setPeriodMode("last_version")
      .setPeriodParam("2015-12-07")
      .setPeriodDate(956789123987L));
    MetricDto gateDetailsMetric = insertGateDetailMetric();
    dbClient.liveMeasureDao().insert(dbSession,
      newLiveMeasure(pr, gateDetailsMetric)
        .setData(IOUtils.toString(getClass().getResource("ProjectStatusActionTest/measure_data.json"))));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.USER, project);

    String response = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PULL_REQUEST, pullRequestKey)
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("project_status-example.json"));
  }

  @Test
  public void return_undefined_status_if_specified_analysis_is_not_found() {
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.USER, project);

    ProjectStatusResponse result = ws.newRequest()
      .setParam(PARAM_ANALYSIS_ID, snapshot.getUuid())
      .executeProtobuf(ProjectStatusResponse.class);

    assertThat(result.getProjectStatus().getStatus()).isEqualTo(Status.NONE);
    assertThat(result.getProjectStatus().getConditionsCount()).isZero();
  }

  @Test
  public void return_undefined_status_if_project_is_not_analyzed() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);

    ProjectStatusResponse result = ws.newRequest()
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .executeProtobuf(ProjectStatusResponse.class);

    assertThat(result.getProjectStatus().getStatus()).isEqualTo(Status.NONE);
    assertThat(result.getProjectStatus().getConditionsCount()).isZero();
  }

  @Test
  public void project_administrator_is_allowed_to_get_project_status() {
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.ADMIN, project);

    ws.newRequest()
      .setParam(PARAM_ANALYSIS_ID, snapshot.getUuid())
      .executeProtobuf(ProjectStatusResponse.class);
  }

  @Test
  public void project_user_is_allowed_to_get_project_status() {
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.USER, project);

    ws.newRequest()
      .setParam(PARAM_ANALYSIS_ID, snapshot.getUuid())
      .executeProtobuf(ProjectStatusResponse.class);
  }

  @Test
  public void check_cayc_compliant_flag() {
    ComponentDto project = db.components().insertPrivateProject();
    var qg = db.qualityGates().insertBuiltInQualityGate();
    db.qualityGates().setDefaultQualityGate(qg);
    when(qualityGateCaycChecker.checkCaycCompliantFromProject(any(DbSession.class), eq(project.uuid()))).thenReturn(COMPLIANT);
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.USER, project);

    ProjectStatusResponse result = ws.newRequest()
      .setParam(PARAM_ANALYSIS_ID, snapshot.getUuid())
      .executeProtobuf(ProjectStatusResponse.class);

    assertEquals(COMPLIANT.toString(), result.getProjectStatus().getCaycStatus());
  }

  @Test
  public void user_with_project_scan_permission_is_allowed_to_get_project_status() {
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project));
    dbSession.commit();
    userSession.addProjectPermission(UserRole.SCAN, project);

    var response = ws.newRequest()
      .setParam(PARAM_ANALYSIS_ID, snapshot.getUuid()).execute();

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void user_with_global_scan_permission_is_allowed_to_get_project_status() {
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project));
    dbSession.commit();
    userSession.addPermission(GlobalPermission.SCAN);

    var response = ws.newRequest()
      .setParam(PARAM_ANALYSIS_ID, snapshot.getUuid()).execute();

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void fail_if_no_snapshot_id_found() {
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_ANALYSIS_ID, ANALYSIS_ID)
      .executeProtobuf(ProjectStatusResponse.class))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Analysis with id 'task-uuid' is not found");
  }

  @Test
  public void fail_if_insufficient_privileges() {
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project));
    dbSession.commit();
    userSession.logIn();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_ANALYSIS_ID, snapshot.getUuid())
      .executeProtobuf(ProjectStatusResponse.class))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_project_id_and_ce_task_id_provided() {
    ComponentDto project = db.components().insertPrivateProject();
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_ANALYSIS_ID, "analysis-id")
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .execute().getInput())
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("Either 'analysisId', 'projectId' or 'projectKey' must be provided");
  }

  @Test
  public void fail_if_branch_key_and_pull_request_id_provided() {
    ComponentDto project = db.components().insertPrivateProject();
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, "key")
      .setParam(PARAM_BRANCH, "branch")
      .setParam(PARAM_PULL_REQUEST, "pr")
      .execute().getInput())
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("Either 'branch' or 'pullRequest' can be provided, not both");
  }

  @Test
  public void fail_if_no_parameter_provided() {
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> ws.newRequest()
      .execute().getInput())
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("Either 'analysisId', 'projectId' or 'projectKey' must be provided");
  }

  @Test
  public void fail_when_using_branch_uuid() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    SnapshotDto snapshot = db.components().insertSnapshot(branch);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("projectId", branch.uuid())
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining(format("Project '%s' not found", branch.uuid()));
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private MetricDto insertGateDetailMetric() {
    return dbClient.metricDao().insert(dbSession, newMetricDto()
      .setEnabled(true)
      .setKey(CoreMetrics.QUALITY_GATE_DETAILS_KEY));
  }

}
