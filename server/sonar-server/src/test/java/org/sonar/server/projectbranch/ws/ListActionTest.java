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
package org.sonar.server.projectbranch.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
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
import org.sonarqube.ws.Common;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsBranches;
import org.sonarqube.ws.WsBranches.Branch;
import org.sonarqube.ws.WsBranches.ListWsResponse;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.WsBranches.Branch.Status;

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

  private MetricDto qualityGateStatus;

  public WsActionTester ws = new WsActionTester(new ListAction(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), resourceTypes), issueIndex));

  @Before
  public void setUp() throws Exception {
    qualityGateStatus = db.measures().insertMetric(m -> m.setKey(ALERT_STATUS_KEY));
  }

  @Test
  public void test_definition() {
    WebService.Action definition = ws.getDef();
    assertThat(definition.key()).isEqualTo("list");
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("project");
    assertThat(definition.since()).isEqualTo("6.6");
  }

  @Test
  public void test_example() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setDbKey("sonarqube"));
    ComponentDto longLivingBranch = db.components().insertProjectBranch(project, b -> b.setKey("feature/bar").setBranchType(BranchType.LONG));
    db.components().insertProjectBranch(project, b -> b.setKey("feature/foo").setBranchType(BranchType.SHORT).setMergeBranchUuid(longLivingBranch.uuid()));
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    String json = ws.newRequest()
      .setParam("project", project.getDbKey())
      .execute()
      .getInput();

    assertJson(json).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void main_branch() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getDbKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(Branch::getName, Branch::getIsMain, Branch::getType)
      .containsExactlyInAnyOrder(tuple("master", true, Common.BranchType.LONG));
  }

  @Test
  public void main_branch_with_specified_name() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization, "head");
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getDbKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(Branch::getName, Branch::getIsMain, Branch::getType)
      .containsExactlyInAnyOrder(tuple("head", true, Common.BranchType.LONG));
  }

  @Test
  public void test_project_with_zero_branches() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(UserRole.USER, project);

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
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getDbKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(Branch::getName, Branch::getType)
      .containsExactlyInAnyOrder(
        tuple("master", Common.BranchType.LONG),
        tuple("feature/foo", Common.BranchType.LONG),
        tuple("feature/bar", Common.BranchType.LONG));
  }

  @Test
  public void short_living_branches() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto longLivingBranch = db.components().insertProjectBranch(project,
      b -> b.setKey("long").setBranchType(BranchType.LONG));
    ComponentDto shortLivingBranch = db.components().insertProjectBranch(project,
      b -> b.setKey("short").setBranchType(BranchType.SHORT).setMergeBranchUuid(longLivingBranch.uuid()));
    ComponentDto shortLivingBranchOnMaster = db.components().insertProjectBranch(project,
      b -> b.setKey("short_on_master").setBranchType(BranchType.SHORT).setMergeBranchUuid(project.uuid()));

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(Branch::getName, Branch::getType, Branch::getMergeBranch)
      .containsExactlyInAnyOrder(
        tuple("master", Common.BranchType.LONG, ""),
        tuple(longLivingBranch.getBranch(), Common.BranchType.LONG, ""),
        tuple(shortLivingBranch.getBranch(), Common.BranchType.SHORT, longLivingBranch.getBranch()),
        tuple(shortLivingBranchOnMaster.getBranch(), Common.BranchType.SHORT, "master"));
  }

  @Test
  public void mergeBranch_is_using_default_main_name_when_main_branch_has_no_name() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto shortLivingBranch = db.components().insertProjectBranch(project,
      b -> b.setKey("short").setBranchType(BranchType.SHORT).setMergeBranchUuid(project.uuid()));

    WsBranches.ShowWsResponse response = ws.newRequest()
      .setParam("project", shortLivingBranch.getKey())
      .executeProtobuf(WsBranches.ShowWsResponse.class);

    assertThat(response.getBranch())
      .extracting(Branch::getName, Branch::getType, Branch::getMergeBranch)
      .containsExactlyInAnyOrder(shortLivingBranch.getBranch(), Common.BranchType.SHORT, "master");
  }

  @Test
  public void short_living_branch_on_removed_branch() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto shortLivingBranch = db.components().insertProjectBranch(project,
      b -> b.setKey("short").setBranchType(BranchType.SHORT).setMergeBranchUuid("unknown"));

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(Branch::getName, Branch::getType, Branch::hasMergeBranch, Branch::getIsOrphan)
      .containsExactlyInAnyOrder(
        tuple("master", Common.BranchType.LONG, false, false),
        tuple(shortLivingBranch.getBranch(), Common.BranchType.SHORT, false, true));
  }

  @Test
  public void quality_gate_status_on_long_living_branch() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    SnapshotDto branchAnalysis = db.components().insertSnapshot(branch);
    db.measures().insertMeasure(branch, branchAnalysis, qualityGateStatus, m -> m.setData("OK"));

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList())
      .extracting(b -> b.getStatus().hasQualityGateStatus(), b -> b.getStatus().getQualityGateStatus())
      .containsExactlyInAnyOrder(tuple(false, ""), tuple(true, "OK"));
  }

  @Test
  public void bugs_vulnerabilities_and_code_smells_on_short_living_branch() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto longLivingBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    ComponentDto shortLivingBranch = db.components().insertProjectBranch(project,
      b -> b.setBranchType(BranchType.SHORT).setMergeBranchUuid(longLivingBranch.uuid()));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, shortLivingBranch, shortLivingBranch, i -> i.setType(BUG));
    db.issues().insert(rule, shortLivingBranch, shortLivingBranch, i -> i.setType(VULNERABILITY));
    db.issues().insert(rule, shortLivingBranch, shortLivingBranch, i -> i.setType(VULNERABILITY));
    db.issues().insert(rule, shortLivingBranch, shortLivingBranch, i -> i.setType(CODE_SMELL));
    db.issues().insert(rule, shortLivingBranch, shortLivingBranch, i -> i.setType(CODE_SMELL));
    db.issues().insert(rule, shortLivingBranch, shortLivingBranch, i -> i.setType(CODE_SMELL));
    issueIndexer.indexOnStartup(emptySet());
    permissionIndexerTester.allowOnlyAnyone(project);

    ListWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getBranchesList().stream().map(WsBranches.Branch::getStatus))
      .extracting(Status::hasBugs, Status::getBugs, Status::hasVulnerabilities, Status::getVulnerabilities, Status::hasCodeSmells, Status::getCodeSmells)
      .containsExactlyInAnyOrder(
        tuple(false, 0, false, 0, false, 0),
        tuple(false, 0, false, 0, false, 0),
        tuple(true, 1, true, 2, true, 3));
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
