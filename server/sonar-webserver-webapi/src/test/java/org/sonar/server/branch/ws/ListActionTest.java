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
package org.sonar.server.branch.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common.BranchType;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.ProjectBranches;
import org.sonarqube.ws.ProjectBranches.Branch;
import org.sonarqube.ws.ProjectBranches.ListWsResponse;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.utils.DateUtils.dateToLong;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.test.JsonAssert.assertJson;

public class ListActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(PROJECT);
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()));
  private PermissionIndexerTester permissionIndexerTester = new PermissionIndexerTester(es, issueIndexer);

  private MetricDto qualityGateStatus;

  public WsActionTester ws = new WsActionTester(new ListAction(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), resourceTypes)));

  @Before
  public void setUp() throws Exception {
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
    ComponentDto project = db.components().insertMainBranch(p -> p.setDbKey("sonarqube"));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(project).setLast(true).setCreatedAt(parseDateTime("2017-04-01T01:15:42+0100").getTime()));
    db.measures().insertLiveMeasure(project, qualityGateStatus, m -> m.setData("ERROR"));

    ComponentDto branch = db.components()
      .insertProjectBranch(project, b -> b.setKey("feature/foo").setBranchType(BRANCH));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(branch).setLast(true).setCreatedAt(parseDateTime("2017-04-03T13:37:00+0100").getTime()));
    db.measures().insertLiveMeasure(branch, qualityGateStatus, m -> m.setData("OK"));

    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, branch, branch, i -> i.setType(BUG).setResolution(null));

    issueIndexer.indexOnStartup(emptySet());

    userSession.logIn().addProjectPermission(USER, project);

    String json = ws.newRequest()
      .setParam("project", project.getDbKey())
      .execute()
      .getInput();

    assertJson(json).isSimilarTo(ws.getDef().responseExampleAsString());
    assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(json);
  }

  @Test
  public void test_with_SCAN_EXCUTION_permission() {
    ComponentDto project = db.components().insertMainBranch(p -> p.setDbKey("sonarqube"));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(project).setLast(true).setCreatedAt(parseDateTime("2017-04-01T01:15:42+0100").getTime()));
    db.measures().insertLiveMeasure(project, qualityGateStatus, m -> m.setData("ERROR"));

    ComponentDto branch = db.components()
      .insertProjectBranch(project, b -> b.setKey("feature/foo").setBranchType(BRANCH));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(branch).setLast(true).setCreatedAt(parseDateTime("2017-04-03T13:37:00+0100").getTime()));
    db.measures().insertLiveMeasure(branch, qualityGateStatus, m -> m.setData("OK"));

    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, branch, branch, i -> i.setType(BUG).setResolution(null));
    issueIndexer.indexOnStartup(emptySet());

    userSession.logIn().addProjectPermission(SCAN_EXECUTION, project);

    String json = ws.newRequest()
      .setParam("project", project.getDbKey())
      .execute()
      .getInput();

    assertJson(json).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void main_branch() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(USER, project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getDbKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(Branch::getName, Branch::getIsMain, Branch::getType)
      .containsExactlyInAnyOrder(tuple("master", true, BranchType.BRANCH));
  }

  @Test
  public void main_branch_with_specified_name() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization, "head");
    userSession.logIn().addProjectPermission(USER, project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getDbKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(Branch::getName, Branch::getIsMain, Branch::getType)
      .containsExactlyInAnyOrder(tuple("head", true, BranchType.BRANCH));
  }

  @Test
  public void test_project_with_zero_branches() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(USER, project);

    String json = ws.newRequest()
      .setParam("project", project.getDbKey())
      .setMediaType(MediaTypes.JSON)
      .execute()
      .getInput();
    assertJson(json).isSimilarTo("{\"branches\": []}");
  }

  @Test
  public void test_project_with_branches() {
    ComponentDto project = db.components().insertMainBranch();
    db.components().insertProjectBranch(project, b -> b.setKey("feature/bar"));
    db.components().insertProjectBranch(project, b -> b.setKey("feature/foo"));
    userSession.logIn().addProjectPermission(USER, project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getDbKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(Branch::getName, Branch::getType)
      .containsExactlyInAnyOrder(
        tuple("master", BranchType.BRANCH),
        tuple("feature/foo", BranchType.BRANCH),
        tuple("feature/bar", BranchType.BRANCH));
  }

  @Test
  public void status_on_branch() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(org.sonar.db.component.BranchType.BRANCH));
    db.measures().insertLiveMeasure(branch, qualityGateStatus, m -> m.setData("OK"));

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

    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(USER, project);
    ComponentDto branch2 = db.components().insertProjectBranch(project, b -> b.setBranchType(org.sonar.db.component.BranchType.BRANCH));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(branch2).setCreatedAt(lastAnalysisBranch));
    db.commit();
    issueIndexer.indexOnStartup(emptySet());
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

  @Test
  public void application_branches() {
    ComponentDto application = db.components().insertPrivateApplication(db.getDefaultOrganization());
    db.components().insertProjectBranch(application, b -> b.setKey("feature/bar"));
    db.components().insertProjectBranch(application, b -> b.setKey("feature/foo"));
    userSession.logIn().addProjectPermission(USER, application);

    ListWsResponse response = ws.newRequest()
      .setParam("project", application.getDbKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(Branch::getName, Branch::getType)
      .containsExactlyInAnyOrder(
        tuple("feature/foo", BranchType.BRANCH),
        tuple("feature/bar", BranchType.BRANCH));
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    ws.newRequest()
      .setParam("project", branch.getDbKey())
      .execute();
  }

  @Test
  public void fail_if_missing_project_parameter() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'project' parameter is missing");

    ws.newRequest().execute();
  }

  @Test
  public void fail_if_not_a_reference_on_project() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    userSession.logIn().addProjectPermission(USER, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid project");

    ws.newRequest()
      .setParam("project", file.getDbKey())
      .execute();
  }

  @Test
  public void fail_if_project_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'foo' not found");

    ws.newRequest()
      .setParam("project", "foo")
      .execute();
  }

}
