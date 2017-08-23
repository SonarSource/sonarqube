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
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsBranches.ShowWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.WsBranches.Branch;

public class ShowActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(PROJECT);

  private MetricDto qualityGateStatus;
  private MetricDto bugs;
  private MetricDto vulnerabilities;
  private MetricDto codeSmells;

  public WsActionTester ws = new WsActionTester(new ShowAction(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), resourceTypes),
    mock(org.sonar.server.computation.task.projectanalysis.analysis.Branch.class)));

  @Before
  public void setUp() throws Exception {
    qualityGateStatus = db.measures().insertMetric(m -> m.setKey(ALERT_STATUS_KEY));
    bugs = db.measures().insertMetric(m -> m.setKey(BUGS_KEY));
    vulnerabilities = db.measures().insertMetric(m -> m.setKey(VULNERABILITIES_KEY));
    codeSmells = db.measures().insertMetric(m -> m.setKey(CODE_SMELLS_KEY));
  }

  @Test
  public void test_definition() {
    WebService.Action definition = ws.getDef();
    assertThat(definition.key()).isEqualTo("show");
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("component", "branch");
    assertThat(definition.since()).isEqualTo("6.6");
  }

  @Test
  public void main_branch_when_no_branch_parameter() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    ShowWsResponse response = ws.newRequest()
      .setParam("component", project.getKey())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getBranch())
      .extracting(Branch::getName, Branch::getType, Branch::getIsMain, Branch::hasMergeBranch)
      .containsExactlyInAnyOrder("master", Common.BranchType.LONG, true, false);
  }

  @Test
  public void main_branch_with_name() {
    OrganizationDto organizationD = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organizationD, "head");

    userSession.logIn().addProjectPermission(UserRole.USER, project);

    ShowWsResponse response = ws.newRequest()
      .setParam("component", project.getKey())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getBranch())
      .extracting(Branch::getName, Branch::getType, Branch::getIsMain)
      .containsExactlyInAnyOrder("head", Common.BranchType.LONG, true);
  }

  @Test
  public void long_living_branch() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto longLivingBranch = db.components().insertProjectBranch(project,
      b -> b.setKey("long").setBranchType(BranchType.LONG));

    ShowWsResponse response = ws.newRequest()
      .setParam("component", longLivingBranch.getKey())
      .setParam("branch", longLivingBranch.getBranch())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getBranch())
      .extracting(Branch::getName, Branch::getType, Branch::hasMergeBranch)
      .containsExactlyInAnyOrder(longLivingBranch.getBranch(), Common.BranchType.LONG, false);
  }

  @Test
  public void short_living_branches() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto longLivingBranch = db.components().insertProjectBranch(project,
      b -> b.setKey("long").setBranchType(BranchType.LONG));
    ComponentDto shortLivingBranch = db.components().insertProjectBranch(project,
      b -> b.setKey("short").setBranchType(BranchType.SHORT).setMergeBranchUuid(longLivingBranch.uuid()));

    ShowWsResponse response = ws.newRequest()
      .setParam("component", shortLivingBranch.getKey())
      .setParam("branch", shortLivingBranch.getBranch())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getBranch())
      .extracting(Branch::getName, Branch::getType, Branch::getMergeBranch)
      .containsExactlyInAnyOrder(shortLivingBranch.getBranch(), Common.BranchType.SHORT, longLivingBranch.getBranch());
  }

  @Test
  public void mergeBranch_is_using_default_main_name_when_main_branch_has_no_name() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto shortLivingBranch = db.components().insertProjectBranch(project,
      b -> b.setKey("short").setBranchType(BranchType.SHORT).setMergeBranchUuid(project.uuid()));

    ShowWsResponse response = ws.newRequest()
      .setParam("component", shortLivingBranch.getKey())
      .setParam("branch", shortLivingBranch.getBranch())
      .executeProtobuf(ShowWsResponse.class);

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

    ShowWsResponse response = ws.newRequest()
      .setParam("component", shortLivingBranch.getKey())
      .setParam("branch", shortLivingBranch.getBranch())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getBranch())
      .extracting(Branch::getIsOrphan)
      .containsExactlyInAnyOrder(true);
  }

  @Test
  public void quality_gate_status_on_long_living_branch() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    SnapshotDto branchAnalysis = db.components().insertSnapshot(branch);
    db.measures().insertMeasure(branch, branchAnalysis, qualityGateStatus, m -> m.setData("OK"));

    ShowWsResponse response = ws.newRequest()
      .setParam("component", branch.getKey())
      .setParam("branch", branch.getBranch())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getBranch())
      .extracting(b -> b.getStatus().hasQualityGateStatus(), b -> b.getStatus().getQualityGateStatus())
      .containsExactlyInAnyOrder(true, "OK");
  }

  @Test
  public void bugs_vulnerabilities_and_code_smells_on_short_living_branch() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto longLivingBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    ComponentDto shortLivingBranch = db.components().insertProjectBranch(project,
      b -> b.setBranchType(BranchType.SHORT).setMergeBranchUuid(longLivingBranch.uuid()));
    SnapshotDto branchAnalysis = db.components().insertSnapshot(shortLivingBranch);
    db.measures().insertMeasure(shortLivingBranch, branchAnalysis, bugs, m -> m.setValue(1d));
    db.measures().insertMeasure(shortLivingBranch, branchAnalysis, vulnerabilities, m -> m.setValue(2d));
    db.measures().insertMeasure(shortLivingBranch, branchAnalysis, codeSmells, m -> m.setValue(3d));

    ShowWsResponse response = ws.newRequest()
      .setParam("component", shortLivingBranch.getKey())
      .setParam("branch", shortLivingBranch.getBranch())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getBranch().getStatus())
      .extracting(Branch.Status::hasBugs, Branch.Status::getBugs, Branch.Status::hasVulnerabilities, Branch.Status::getVulnerabilities, Branch.Status::hasCodeSmells,
        Branch.Status::getCodeSmells)
      .containsExactlyInAnyOrder(true, 1, true, 2, true, 3);
  }

  @Test
  public void file() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto longLivingBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    ComponentDto file = db.components().insertComponent(newFileDto(longLivingBranch));

    ShowWsResponse response = ws.newRequest()
      .setParam("component", file.getKey())
      .setParam("branch", file.getBranch())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getBranch())
      .extracting(Branch::getName, Branch::getType, Branch::hasMergeBranch)
      .containsExactlyInAnyOrder(file.getBranch(), Common.BranchType.LONG, false);
  }

  @Test
  public void fail_if_missing_component_parameter() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'component' parameter is missing");

    ws.newRequest()
      .setParam("branch", "my_branch")
      .execute();
  }

  @Test
  public void fail_if_branch_does_not_exist() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    userSession.addProjectPermission(UserRole.USER, project);
    db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(String.format("Component '%s' on branch '%s' not found", file.getKey(), "another_branch"));

    ws.newRequest()
      .setParam("component", file.getKey())
      .setParam("branch", "another_branch")
      .execute();
  }

  @Test
  public void fail_if_branch_exists_in_projects_table_but_not_in_project_branches_table() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    db.executeDdl("delete from project_branches where uuid = '" + branch.uuid() + "'");
    db.commit();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Branch uuid '" + branch.uuid() + "' not found");

    ws.newRequest()
      .setParam("component", branch.getKey())
      .setParam("branch", branch.getBranch())
      .execute();
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(String.format("Component '%s' on branch '%s' not found", branch.getDbKey(), branch.getBranch()));

    ws.newRequest()
      .setParam("component", branch.getDbKey())
      .setParam("branch", branch.getBranch())
      .execute();
  }

  @Test
  public void test_example() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setDbKey("sonarqube"));
    ComponentDto longLivingBranch = db.components().insertProjectBranch(project, b -> b.setKey("feature/bar").setBranchType(BranchType.LONG));
    db.components().insertProjectBranch(project, b -> b.setKey("feature/foo").setBranchType(BranchType.SHORT).setMergeBranchUuid(longLivingBranch.uuid()));
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    String json = ws.newRequest()
      .setParam("component", longLivingBranch.getKey())
      .setParam("branch", longLivingBranch.getBranch())
      .execute()
      .getInput();

    assertJson(json).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void empty_response_when_branch_feature_not_supported() {
    ws = new WsActionTester(new ShowAction(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), resourceTypes)));
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    String response = ws.newRequest()
      .setParam("component", branch.getKey())
      .setParam("branch", branch.getBranch())
      .execute().getInput();

    assertThat(response).isEqualTo("{}");
  }
}
