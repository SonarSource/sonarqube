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
package org.sonar.server.projectanalysis.ws;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newProjectBranch;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_BRANCH;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.WsRequest.Method.POST;

@RunWith(DataProviderRunner.class)
public class UnsetBaselineActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private WsActionTester ws = new WsActionTester(new UnsetBaselineAction(dbClient, userSession, TestComponentFinder.from(db)));

  @Test
  public void does_not_fail_and_has_no_effect_when_there_is_no_baseline_on_main_branch() {
    ComponentDto project = db.components().insertMainBranch(db.organizations().insert());
    ComponentDto branch = db.components().insertProjectBranch(project);
    SnapshotDto analysis = db.components().insertSnapshot(project);
    logInAsProjectAdministrator(project);

    call(project.getKey(), null);

    verifyManualBaseline(project, null);
  }

  @Test
  public void does_not_fail_and_has_no_effect_when_there_is_no_baseline_on_long_living_branch() {
    ComponentDto project = db.components().insertMainBranch(db.organizations().insert());
    ComponentDto branch = db.components().insertProjectBranch(project);
    SnapshotDto analysis = db.components().insertSnapshot(project);
    logInAsProjectAdministrator(project);

    call(project.getKey(), branch.getBranch());

    verifyManualBaseline(branch, null);
  }

  @Test
  public void unset_baseline_when_it_is_set_on_main_branch() {
    ComponentDto project = db.components().insertMainBranch(db.organizations().insert());
    ComponentDto branch = db.components().insertProjectBranch(project);
    SnapshotDto projectAnalysis = db.components().insertSnapshot(project);
    SnapshotDto branchAnalysis = db.components().insertSnapshot(project);
    db.components().setManualBaseline(project, projectAnalysis);
    logInAsProjectAdministrator(project);

    call(project.getKey(), null);

    verifyManualBaseline(project, null);
  }

  @Test
  public void unset_baseline_when_it_is_set_long_living_branch() {
    ComponentDto project = db.components().insertMainBranch(db.organizations().insert());
    ComponentDto branch = db.components().insertProjectBranch(project);
    SnapshotDto projectAnalysis = db.components().insertSnapshot(branch);
    SnapshotDto branchAnalysis = db.components().insertSnapshot(project);
    db.components().setManualBaseline(branch, branchAnalysis);
    logInAsProjectAdministrator(project);

    call(project.getKey(), branch.getBranch());

    verifyManualBaseline(branch, null);
  }

  @Test
  public void fail_when_user_is_not_admin_on_project() {
    ComponentDto project = db.components().insertMainBranch(db.organizations().insert());
    db.components().insertProjectBranch(project);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    call(project.getKey(), null);
  }

  @Test
  public void fail_when_user_is_not_admin_on_project_of_branch() {
    ComponentDto project = db.components().insertMainBranch(db.organizations().insert());
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    call(project.getKey(), branch.getBranch());
  }

  @Test
  @UseDataProvider("nullOrEmptyOrValue")
  public void fail_with_IAE_when_missing_project_parameter(@Nullable String branchParam) {
    ComponentDto project = db.components().insertMainBranch(db.organizations().insert());
    db.components().insertProjectBranch(project);
    logInAsProjectAdministrator(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'project' parameter is missing");

    call(null, branchParam);
  }

  @Test
  @UseDataProvider("nullOrEmptyOrValue")
  public void fail_with_IAE_when_project_parameter_empty(@Nullable String branchParam) {
    ComponentDto project = db.components().insertMainBranch(db.organizations().insert());
    db.components().insertProjectBranch(project);
    logInAsProjectAdministrator(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'project' parameter is missing");

    call("", branchParam);
  }

  @DataProvider
  public static Object[][] nullOrEmptyOrValue() {
    return new Object[][] {
      {null},
      {""},
      {randomAlphabetic(10)},
    };
  }

  @Test
  @UseDataProvider("nullOrEmpty")
  public void does_not_fail_with_IAE_when_missing_branch_parameter(@Nullable String branchParam) {
    ComponentDto project = db.components().insertMainBranch(db.organizations().insert());
    db.components().insertProjectBranch(project);
    logInAsProjectAdministrator(project);

    call(project.getKey(), branchParam);
  }

  @DataProvider
  public static Object[][] nullOrEmpty() {
    return new Object[][] {
      {null},
      {""},
    };
  }

  @DataProvider
  public static Object[][] nonexistentParamsAndFailureMessage() {
    return new Object[][] {
      {ImmutableMap.of(PARAM_PROJECT, "nonexistent"), "Component 'nonexistent' on branch .* not found"},
      {ImmutableMap.of(PARAM_BRANCH, "nonexistent"), "Component .* on branch 'nonexistent' not found"}
    };
  }

  @Test
  public void fail_when_branch_does_not_belong_to_project() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto otherProject = db.components().insertMainBranch(organization);
    ComponentDto otherBranch = db.components().insertProjectBranch(otherProject);
    logInAsProjectAdministrator(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(String.format("Component '%s' on branch '%s' not found", project.getKey(), otherBranch.getKey()));

    call(project.getKey(), otherBranch.getKey());
  }

  @Test
  public void fail_with_IAE_when_branch_is_short() {
    ComponentDto project = newPrivateProjectDto(db.organizations().insert());
    BranchDto branch = newBranchDto(project.projectUuid(), BranchType.SHORT);
    db.components().insertProjectBranch(project, branch);
    logInAsProjectAdministrator(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(String.format("Not a long-living branch: '%s'", branch.getKey()));

    call(project.getKey(), branch.getKey());
  }

  @Test
  public void fail_with_NotFoundException_when_branch_is_pull_request() {
    ComponentDto project = newPrivateProjectDto(db.organizations().insert());
    BranchDto branch = newBranchDto(project.projectUuid(), BranchType.LONG);
    db.components().insertProjectBranch(project, branch);
    ComponentDto pullRequest = newProjectBranch(project, branch);
    logInAsProjectAdministrator(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(String.format("Component '%s' on branch '%s' not found", project.getKey(), pullRequest.getKey()));

    call(project.getKey(), pullRequest.getKey());
  }

  @Test
  public void verify_ws_parameters() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.isPost()).isTrue();
    assertThat(definition.key()).isEqualTo("unset_baseline");
    assertThat(definition.since()).isEqualTo("7.7");
    assertThat(definition.isInternal()).isFalse();
  }

  private void logInAsProjectAdministrator(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }

  private void call(@Nullable String project, @Nullable String branchName) {
    TestRequest httpRequest = ws.newRequest().setMethod(POST.name());
    ofNullable(project).ifPresent(t -> httpRequest.setParam(PARAM_PROJECT, t));
    ofNullable(branchName).ifPresent(t -> httpRequest.setParam(PARAM_BRANCH, t));

    httpRequest.execute();
  }

  private void verifyManualBaseline(ComponentDto project, @Nullable SnapshotDto projectAnalysis) {
    BranchDto branchDto = db.getDbClient().branchDao().selectByUuid(dbSession, project.uuid()).get();
    if (projectAnalysis == null) {
      assertThat(branchDto.getManualBaseline()).isNull();
    } else {
      assertThat(branchDto.getManualBaseline()).isEqualTo(projectAnalysis.getUuid());
    }
  }

}
