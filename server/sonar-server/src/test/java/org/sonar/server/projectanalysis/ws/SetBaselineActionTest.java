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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_ANALYSIS;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_BRANCH;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_PROJECT;
import static org.sonar.test.Matchers.regexMatcher;
import static org.sonarqube.ws.client.WsRequest.Method.POST;

@RunWith(DataProviderRunner.class)
public class SetBaselineActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private WsActionTester ws = new WsActionTester(new SetBaselineAction(dbClient, userSession, TestComponentFinder.from(db)));

  @Test
  @UseDataProvider("nullOrEmpty")
  public void set_baseline_on_main_branch(@Nullable String branchName) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    BranchDto branch = new BranchDto()
      .setBranchType(BranchType.LONG)
      .setProjectUuid(project.uuid())
      .setUuid(project.uuid())
      .setKey("master");
    db.components().insertComponent(project);
    db.getDbClient().branchDao().insert(dbSession, branch);
    SnapshotDto analysis = db.components().insertSnapshot(project);
    logInAsProjectAdministrator(project);

    call(project.getKey(), branchName, analysis.getUuid());

    BranchDto loaded = dbClient.branchDao().selectByUuid(dbSession, branch.getUuid()).get();
    assertThat(loaded.getManualBaseline()).isEqualTo(analysis.getUuid());
  }

  @DataProvider
  public static Object[][] nullOrEmpty() {
    return new Object[][] {
      {null},
      {""},
      {"     "},
    };
  }

  @Test
  public void set_baseline_on_long_living_branch() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    BranchDto branch = ComponentTesting.newBranchDto(project.projectUuid(), BranchType.LONG);
    db.components().insertProjectBranch(project, branch);
    ComponentDto branchComponentDto = ComponentTesting.newProjectBranch(project, branch);
    SnapshotDto analysis = db.components().insertSnapshot(branchComponentDto);
    logInAsProjectAdministrator(project);

    call(project.getKey(), branch.getKey(), analysis.getUuid());

    BranchDto loaded = dbClient.branchDao().selectByUuid(dbSession, branch.getUuid()).get();
    assertThat(loaded.getManualBaseline()).isEqualTo(analysis.getUuid());
  }

  @Test
  public void fail_when_user_is_not_admin() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    BranchDto branch = ComponentTesting.newBranchDto(project.projectUuid(), BranchType.LONG);
    db.components().insertProjectBranch(project, branch);
    ComponentDto branchComponentDto = ComponentTesting.newProjectBranch(project, branch);
    SnapshotDto analysis = db.components().insertSnapshot(branchComponentDto);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    call(project.getKey(), branch.getKey(), analysis.getUuid());
  }

  @Test
  @UseDataProvider("missingOrEmptyParamsAndFailureMessage")
  public void fail_with_IAE_when_required_param_missing_or_empty(Map<String, String> params, String message) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(message);

    call(params);
  }

  @DataProvider
  public static Object[][] missingOrEmptyParamsAndFailureMessage() {
    MapBuilder builder = new MapBuilder()
      .put(PARAM_PROJECT, "project key")
      .put(PARAM_BRANCH, "branch key")
      .put(PARAM_ANALYSIS, "analysis uuid");

    return new Object[][] {
      {builder.put(PARAM_PROJECT, null).map, "The 'project' parameter is missing"},
      {builder.put(PARAM_PROJECT, "").map, "The 'project' parameter is missing"},
      {builder.put(PARAM_ANALYSIS, null).map, "The 'analysis' parameter is missing"},
      {builder.put(PARAM_ANALYSIS, "").map, "The 'analysis' parameter is missing"},
    };
  }

  @Test
  @UseDataProvider("nonexistentParamsAndFailureMessage")
  public void fail_with_IAE_when_required_param_nonexistent(Map<String, String> nonexistentParams, String regex) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    BranchDto branch = ComponentTesting.newBranchDto(project.projectUuid(), BranchType.LONG);
    db.components().insertProjectBranch(project, branch);
    ComponentDto branchComponentDto = ComponentTesting.newProjectBranch(project, branch);
    SnapshotDto analysis = db.components().insertSnapshot(branchComponentDto);
    logInAsProjectAdministrator(project);

    Map<String, String> params = new HashMap<>();
    params.put(PARAM_PROJECT, project.getKey());
    params.put(PARAM_BRANCH, branch.getKey());
    params.put(PARAM_ANALYSIS, analysis.getUuid());
    params.putAll(nonexistentParams);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(regexMatcher(regex));

    call(params);
  }

  @DataProvider
  public static Object[][] nonexistentParamsAndFailureMessage() {
    MapBuilder builder = new MapBuilder();

    return new Object[][] {
      {builder.put(PARAM_PROJECT, "nonexistent").map, "Component 'nonexistent' on branch .* not found"},
      {builder.put(PARAM_BRANCH, "nonexistent").map, "Component .* on branch 'nonexistent' not found"},
      {builder.put(PARAM_ANALYSIS, "nonexistent").map, "Analysis 'nonexistent' is not found"},
    };
  }

  @Test
  public void fail_when_branch_does_not_belong_to_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    BranchDto branch = ComponentTesting.newBranchDto(project.projectUuid(), BranchType.LONG);
    db.components().insertProjectBranch(project, branch);
    ComponentDto branchComponentDto = ComponentTesting.newProjectBranch(project, branch);
    SnapshotDto analysis = db.components().insertSnapshot(branchComponentDto);
    logInAsProjectAdministrator(project);

    ComponentDto otherProject = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    BranchDto otherBranch = ComponentTesting.newBranchDto(otherProject.projectUuid(), BranchType.LONG);
    db.components().insertProjectBranch(otherProject, otherBranch);
    ComponentTesting.newProjectBranch(otherProject, otherBranch);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(String.format("Component '%s' on branch '%s' not found", project.getKey(), otherBranch.getKey()));

    call(project.getKey(), otherBranch.getKey(), analysis.getUuid());
  }

  @Test
  public void fail_when_analysis_does_not_belong_to_main_branch_of_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    BranchDto branch = new BranchDto()
      .setBranchType(BranchType.LONG)
      .setProjectUuid(project.uuid())
      .setUuid(project.uuid())
      .setKey("master");
    db.components().insertComponent(project);
    db.getDbClient().branchDao().insert(dbSession, branch);
    logInAsProjectAdministrator(project);

    ComponentDto otherProject = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    SnapshotDto otherAnalysis = db.components().insertSnapshot(otherProject);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(String.format("Analysis '%s' does not belong to project '%s'",
      otherAnalysis.getUuid(), project.getKey()));

    call(ImmutableMap.of(PARAM_PROJECT, project.getKey(), PARAM_ANALYSIS, otherAnalysis.getUuid()));
  }

  @Test
  public void fail_when_analysis_does_not_belong_to_non_main_branch_of_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    BranchDto branch = ComponentTesting.newBranchDto(project.projectUuid(), BranchType.LONG);
    db.components().insertProjectBranch(project, branch);
    logInAsProjectAdministrator(project);

    ComponentDto otherProject = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    SnapshotDto otherAnalysis = db.components().insertProjectAndSnapshot(otherProject);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(String.format("Analysis '%s' does not belong to branch '%s' of project '%s'",
      otherAnalysis.getUuid(), branch.getKey(), project.getKey()));

    call(project.getKey(), branch.getKey(), otherAnalysis.getUuid());
  }

  @Test
  public void fail_when_branch_is_not_long() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    BranchDto branch = ComponentTesting.newBranchDto(project.projectUuid(), BranchType.SHORT);
    db.components().insertProjectBranch(project, branch);
    ComponentDto branchComponentDto = ComponentTesting.newProjectBranch(project, branch);
    SnapshotDto analysis = db.components().insertSnapshot(branchComponentDto);
    logInAsProjectAdministrator(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(String.format("Not a long-living branch: '%s'", branch.getKey()));

    call(project.getKey(), branch.getKey(), analysis.getUuid());
  }

  @Test
  public void ws_parameters() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.isPost()).isTrue();
    assertThat(definition.key()).isEqualTo("set_baseline");
    assertThat(definition.since()).isEqualTo("7.7");
    assertThat(definition.isInternal()).isFalse();
  }

  private void logInAsProjectAdministrator(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }

  private void call(Map<String, String> params) {
    TestRequest httpRequest = ws.newRequest().setMethod(POST.name());

    for (Map.Entry<String, String> param : params.entrySet()) {
      httpRequest.setParam(param.getKey(), param.getValue());
    }

    httpRequest.execute();
  }

  private void call(String projectKey, @Nullable String branchKey, String analysisUuid) {
    if (branchKey == null) {
      call(ImmutableMap.of(
        PARAM_PROJECT, projectKey,
        PARAM_ANALYSIS, analysisUuid));
    } else {
      call(ImmutableMap.of(
        PARAM_PROJECT, projectKey,
        PARAM_BRANCH, branchKey,
        PARAM_ANALYSIS, analysisUuid));
    }
  }

  private static class MapBuilder {
    private final Map<String, String> map;

    private MapBuilder() {
      this.map = Collections.emptyMap();
    }

    private MapBuilder(Map<String, String> map) {
      this.map = map;
    }

    public MapBuilder put(String key, @Nullable String value) {
      Map<String, String> copy = new HashMap<>(map);
      if (value == null) {
        copy.remove(key);
      } else {
        copy.put(key, value);
      }
      return new MapBuilder(copy);
    }
  }
}
