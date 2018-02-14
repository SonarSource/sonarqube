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
package org.sonar.server.branch.pr.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.ProjectPullRequests.ListWsResponse;
import org.sonarqube.ws.ProjectPullRequests.PullRequest;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.api.utils.DateUtils.dateToLong;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.ProjectPullRequests.Status;

public class ListActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = new EsTester(new IssueIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(PROJECT);
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()));
  private IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new AuthorizationTypeSupport(userSession));
  private PermissionIndexerTester permissionIndexerTester = new PermissionIndexerTester(es, issueIndexer);

  public WsActionTester ws = new WsActionTester(new ListAction(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), resourceTypes), issueIndex));

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();
    assertThat(definition.key()).isEqualTo("list");
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("project");
    assertThat(definition.since()).isEqualTo("7.1");
  }

  @Test
  public void json_example() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setDbKey("sonarqube"));
    ComponentDto longLivingBranch = db.components().insertProjectBranch(project, b -> b.setKey("feature/foo").setBranchType(LONG));
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setKey("feature/bar").setMergeBranchUuid(longLivingBranch.uuid()).setBranchType(PULL_REQUEST));
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    db.getDbClient().snapshotDao().insert(db.getSession(),
      SnapshotTesting.newAnalysis(pullRequest).setLast(true).setCreatedAt(DateUtils.parseDateTime("2017-04-01T01:15:42+0100").getTime()));
    db.commit();

    String json = ws.newRequest()
      .setParam("project", project.getDbKey())
      .execute()
      .getInput();

    assertJson(json).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void pull_request() {
    ComponentDto project = db.components().insertMainBranch();
    db.components().insertProjectBranch(project, b -> b.setBranchType(PULL_REQUEST).setKey("feature/bar").setMergeBranchUuid(project.uuid()));
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getDbKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getPullRequestsList())
      .extracting(PullRequest::getId, PullRequest::getBranch, PullRequest::getIsOrphan)
      .containsExactlyInAnyOrder(tuple("feature/bar", "feature/bar", false));
  }

  @Test
  public void project_with_zero_branches() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    String json = ws.newRequest()
      .setParam("project", project.getDbKey())
      .setMediaType(MediaTypes.JSON)
      .execute()
      .getInput();

    assertJson(json).isSimilarTo("{\"pullRequests\": []}");
  }

  @Test
  public void pull_requests() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto longLivingBranch = db.components().insertProjectBranch(project,
      b -> b.setKey("long").setBranchType(BranchType.LONG));
    ComponentDto pullRequestOnLong = db.components().insertProjectBranch(project,
      b -> b.setKey("pull_request_on_long").setBranchType(BranchType.PULL_REQUEST).setMergeBranchUuid(longLivingBranch.uuid()));
    ComponentDto pullRequestOnMaster = db.components().insertProjectBranch(project,
      b -> b.setKey("pull_request_on_master").setBranchType(BranchType.PULL_REQUEST).setMergeBranchUuid(project.uuid()));

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getPullRequestsList())
      .extracting(PullRequest::getId, PullRequest::getBase)
      .containsExactlyInAnyOrder(
        tuple(pullRequestOnLong.getBranch(), longLivingBranch.getBranch()),
        tuple(pullRequestOnMaster.getBranch(), "master"));
  }

  @Test
  public void base_branch_is_using_default_main_name_when_main_branch_has_no_name() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto pullRequest = db.components().insertProjectBranch(project,
      b -> b.setKey("pr-123").setBranchType(PULL_REQUEST).setMergeBranchUuid(project.uuid()));

    ListWsResponse response = ws.newRequest()
      .setParam("project", pullRequest.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getPullRequests(0))
      .extracting(PullRequest::getId, PullRequest::getBase)
      .containsExactlyInAnyOrder(pullRequest.getBranch(), "master");
  }

  @Test
  public void pull_request_on_removed_branch() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto pullRequest = db.components().insertProjectBranch(project,
      b -> b.setKey("pr-123").setBranchType(BranchType.PULL_REQUEST).setMergeBranchUuid("unknown"));

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getPullRequestsList())
      .extracting(PullRequest::getId, PullRequest::hasBase, PullRequest::getIsOrphan)
      .containsExactlyInAnyOrder(
        tuple(pullRequest.getBranch(), false, true));
  }

  @Test
  public void status_on_pull_requests() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto longLivingBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    ComponentDto shortLivingBranch = db.components().insertProjectBranch(project,
      b -> b.setBranchType(BranchType.PULL_REQUEST).setMergeBranchUuid(longLivingBranch.uuid()));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, shortLivingBranch, shortLivingBranch, i -> i.setType(BUG).setResolution(null));
    db.issues().insert(rule, shortLivingBranch, shortLivingBranch, i -> i.setType(BUG).setResolution(RESOLUTION_FIXED));
    db.issues().insert(rule, shortLivingBranch, shortLivingBranch, i -> i.setType(VULNERABILITY).setResolution(null));
    db.issues().insert(rule, shortLivingBranch, shortLivingBranch, i -> i.setType(VULNERABILITY).setResolution(null));
    db.issues().insert(rule, shortLivingBranch, shortLivingBranch, i -> i.setType(CODE_SMELL).setResolution(null));
    db.issues().insert(rule, shortLivingBranch, shortLivingBranch, i -> i.setType(CODE_SMELL).setResolution(null));
    db.issues().insert(rule, shortLivingBranch, shortLivingBranch, i -> i.setType(CODE_SMELL).setResolution(null));
    db.issues().insert(rule, shortLivingBranch, shortLivingBranch, i -> i.setType(CODE_SMELL).setResolution(RESOLUTION_FALSE_POSITIVE));
    issueIndexer.indexOnStartup(emptySet());
    permissionIndexerTester.allowOnlyAnyone(project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getPullRequestsList().stream().map(PullRequest::getStatus))
      .extracting(Status::hasBugs, Status::getBugs, Status::hasVulnerabilities, Status::getVulnerabilities, Status::hasCodeSmells, Status::getCodeSmells)
      .containsExactlyInAnyOrder(tuple(true, 1L, true, 2L, true, 3L));
  }

  @Test
  public void status_on_pull_request_with_no_issue() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto longLivingBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setMergeBranchUuid(longLivingBranch.uuid()));
    issueIndexer.indexOnStartup(emptySet());
    permissionIndexerTester.allowOnlyAnyone(project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getPullRequestsList().stream().map(PullRequest::getStatus))
      .extracting(Status::getBugs, Status::getVulnerabilities, Status::getCodeSmells)
      .containsExactlyInAnyOrder(tuple(0L, 0L, 0L));
  }

  @Test
  public void response_contains_date_of_last_analysis() {
    Long lastAnalysisLongLivingBranch = dateToLong(parseDateTime("2017-04-01T00:00:00+0100"));
    Long previousAnalysisPullRequest = dateToLong(parseDateTime("2017-04-02T00:00:00+0100"));
    Long lastAnalysisPullRequest = dateToLong(parseDateTime("2017-04-03T00:00:00+0100"));

    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto pullRequest1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setMergeBranchUuid(project.uuid()));
    ComponentDto longLivingBranch2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    ComponentDto pullRequest2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setMergeBranchUuid(longLivingBranch2.uuid()));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      SnapshotTesting.newAnalysis(longLivingBranch2).setCreatedAt(lastAnalysisLongLivingBranch));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      SnapshotTesting.newAnalysis(pullRequest2).setCreatedAt(previousAnalysisPullRequest).setLast(false));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      SnapshotTesting.newAnalysis(pullRequest2).setCreatedAt(lastAnalysisPullRequest));
    db.commit();
    issueIndexer.indexOnStartup(emptySet());
    permissionIndexerTester.allowOnlyAnyone(project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getPullRequestsList())
      .extracting(PullRequest::hasAnalysisDate, b -> "".equals(b.getAnalysisDate()) ? null : dateToLong(parseDateTime(b.getAnalysisDate())))
      .containsExactlyInAnyOrder(
        tuple(false, null),
        tuple(true, lastAnalysisPullRequest));
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(UserRole.USER, project);
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
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid project key");

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
