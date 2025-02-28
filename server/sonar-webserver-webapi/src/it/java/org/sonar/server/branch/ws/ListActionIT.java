/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.branch.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.component.ComponentTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.index.AsyncIssueIndexing;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common.BranchType;
import org.sonarqube.ws.ProjectBranches;
import org.sonarqube.ws.ProjectBranches.Branch;
import org.sonarqube.ws.ProjectBranches.ListWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.core.rule.RuleType.BUG;
import static org.sonar.api.utils.DateUtils.dateToLong;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.test.JsonAssert.assertJson;

public class ListActionIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final AsyncIssueIndexing asyncIssueIndexing = mock(AsyncIssueIndexing.class);
  private final ComponentTypes componentTypes = new ComponentTypesRule().setRootQualifiers(PROJECT);
  private final IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()), asyncIssueIndexing);
  private final PermissionIndexerTester permissionIndexerTester = new PermissionIndexerTester(es, issueIndexer);

  private MetricDto qualityGateStatus;

  public WsActionTester ws = new WsActionTester(new ListAction(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), componentTypes)));

  @Before
  public void setUp() {
    qualityGateStatus = db.measures().insertMetric(m -> m.setKey(ALERT_STATUS_KEY));
  }

  @Test
  public void test_definition() {
    WebService.Action definition = ws.getDef();
    assertThat(definition.key()).isEqualTo("list");
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("project");
    assertThat(definition.since()).isEqualTo("6.6");
  }

  @Test
  public void test_example() {
    String mainBranchUuid = "57f02458-65db-4e7f-a144-20122af12a4c]";
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("sonarqube"));
    ProjectDto project = projectData.getProjectDto();
    projectData.getMainBranchDto().setUuid(mainBranchUuid);
    projectData.getMainBranchComponent().setUuid(mainBranchUuid);
    db.executeUpdateSql("UPDATE project_branches SET uuid = ? where kee = 'main' and project_uuid = ?",
      mainBranchUuid, project.getUuid());

    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(projectData.getMainBranchDto()).setLast(true).setCreatedAt(parseDateTime("2017-04-01T01:15:42+0100").getTime()));
    db.measures().insertMeasure(projectData.getMainBranchComponent(), m -> m.addValue(qualityGateStatus.getKey(), "ERROR"));

    BranchDto branch = db.components()
      .insertProjectBranch(project, b -> b.setKey("feature/foo").setBranchType(BRANCH).setUuid("ac312cc6-26a2-4e2c-9eff-1072358f2017"));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(branch).setLast(true).setCreatedAt(parseDateTime("2017-04-03T13:37:00+0100").getTime()));
    db.measures().insertMeasure(branch, m -> m.addValue(qualityGateStatus.getKey(), "OK"));

    RuleDto rule = db.rules().insert();
    db.issues().insert(rule, branch, db.components().getComponentDto(branch), i -> i.setType(BUG).setResolution(null));

    indexIssues();

    userSession.logIn().addProjectPermission(USER, project);

    String json = ws.newRequest()
      .setParam("project", project.getKey())
      .execute()
      .getInput();

    assertJson(json).isSimilarTo(ws.getDef().responseExampleAsString());
    assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(json);
  }

  @Test
  public void test_with_SCAN_EXCUTION_permission() {
    String mainBranchUuid = "57f02458-65db-4e7f-a144-20122af12a4c]";
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("sonarqube"));
    ProjectDto project = projectData.getProjectDto();
    projectData.getMainBranchDto().setUuid(mainBranchUuid);
    projectData.getMainBranchComponent().setUuid(mainBranchUuid);
    db.executeUpdateSql("UPDATE project_branches SET uuid = ? where kee = 'main' and project_uuid = ?",
      mainBranchUuid, project.getUuid());

    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(projectData.getMainBranchDto()).setLast(true).setCreatedAt(parseDateTime("2017-04-01T01:15:42+0100").getTime()));
    db.measures().insertMeasure(projectData.getMainBranchDto(), m -> m.addValue(qualityGateStatus.getKey(), "ERROR"));

    BranchDto branch = db.components()
      .insertProjectBranch(project, b -> b.setKey("feature/foo").setBranchType(BRANCH).setUuid("ac312cc6-26a2-4e2c-9eff-1072358f2017"));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(branch).setLast(true).setCreatedAt(parseDateTime("2017-04-03T13:37:00+0100").getTime()));
    db.measures().insertMeasure(branch, m -> m.addValue(qualityGateStatus.getKey(), "OK"));

    RuleDto rule = db.rules().insert();
    db.issues().insert(rule, branch, db.components().getComponentDto(branch), i -> i.setType(BUG).setResolution(null));
    indexIssues();

    userSession.logIn().addProjectPermission(SCAN.getKey(), project);

    String json = ws.newRequest()
      .setParam("project", project.getKey())
      .execute()
      .getInput();

    assertJson(json).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void main_branch() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().addProjectPermission(USER, project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(Branch::getName, Branch::getIsMain, Branch::getType)
      .containsExactlyInAnyOrder(tuple(DEFAULT_MAIN_BRANCH_NAME, true, BranchType.BRANCH));
  }

  @Test
  public void main_branch_with_specified_name() {
    ProjectData projectData = db.components().insertPrivateProject();
    ProjectDto project = projectData.getProjectDto();
    db.getDbClient().branchDao().updateBranchName(db.getSession(), projectData.getMainBranchDto().getUuid(), "head");
    db.commit();
    userSession.logIn().addProjectPermission(USER, project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(Branch::getName, Branch::getIsMain, Branch::getType)
      .containsExactlyInAnyOrder(tuple("head", true, BranchType.BRANCH));
  }

  @Test
  public void test_project_with_branches() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.components().insertProjectBranch(project, b -> b.setKey("feature/bar"));
    db.components().insertProjectBranch(project, b -> b.setKey("feature/foo"));
    userSession.logIn().addProjectPermission(USER, project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(Branch::getName, Branch::getType)
      .containsExactlyInAnyOrder(
        tuple(DEFAULT_MAIN_BRANCH_NAME, BranchType.BRANCH),
        tuple("feature/foo", BranchType.BRANCH),
        tuple("feature/bar", BranchType.BRANCH));
  }

  @Test
  public void status_on_branch() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().addProjectPermission(USER, project);
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(org.sonar.db.component.BranchType.BRANCH));
    db.measures().insertMeasure(branch, m -> m.addValue(qualityGateStatus.getKey(), "OK"));

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(b -> b.getStatus().hasQualityGateStatus(), b -> b.getStatus().getQualityGateStatus())
      .containsExactlyInAnyOrder(tuple(false, ""), tuple(true, "OK"));
  }

  @Test
  public void response_contains_date_of_last_analysis() {
    Long lastAnalysisBranch = dateToLong(parseDateTime("2017-04-01T00:00:00+0100"));

    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().addProjectPermission(USER, project);
    BranchDto branch2 = db.components().insertProjectBranch(project, b -> b.setBranchType(org.sonar.db.component.BranchType.BRANCH));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(branch2).setCreatedAt(lastAnalysisBranch));
    db.commit();
    indexIssues();
    permissionIndexerTester.allowOnlyAnyone(project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(ProjectBranches.Branch::getType, ProjectBranches.Branch::hasAnalysisDate,
        b -> "".equals(b.getAnalysisDate()) ? null : dateToLong(parseDateTime(b.getAnalysisDate())))
      .containsExactlyInAnyOrder(
        tuple(BranchType.BRANCH, false, null),
        tuple(BranchType.BRANCH, true, lastAnalysisBranch));
  }

  private void indexIssues() {
    issueIndexer.indexAllIssues();
  }

  @Test
  public void application_branches() {
    ProjectDto application = db.components().insertPrivateApplication().getProjectDto();
    db.components().insertProjectBranch(application, b -> b.setKey("feature/bar"));
    db.components().insertProjectBranch(application, b -> b.setKey("feature/foo"));
    userSession.logIn().addProjectPermission(USER, application);

    ListWsResponse response = ws.newRequest()
      .setParam("project", application.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(Branch::getName, Branch::getType)
      .containsExactlyInAnyOrder(
        tuple("main", BranchType.BRANCH),
        tuple("feature/foo", BranchType.BRANCH),
        tuple("feature/bar", BranchType.BRANCH));
  }

  @Test
  public void fail_if_missing_project_parameter() {
    assertThatThrownBy(() -> ws.newRequest().execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The 'project' parameter is missing");
  }

  @Test
  public void fail_if_not_a_reference_on_project() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(mainBranch));
    userSession.logIn().addProjectPermission(USER, projectData.getProjectDto());
    TestRequest request = ws.newRequest().setParam("project", file.getKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Project '" + file.getKey() + "' not found");
  }

  @Test
  public void fail_if_project_does_not_exist() {
    assertThatThrownBy(() -> ws.newRequest().setParam("project", "foo").execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Project 'foo' not found");
  }

}
