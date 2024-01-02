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
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.SPECIFIC_ANALYSIS;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_ANALYSIS;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_BRANCH;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.WsRequest.Method.POST;

@RunWith(DataProviderRunner.class)
public class SetBaselineActionTest {


  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private BranchDao branchDao = db.getDbClient().branchDao();
  private ComponentDbTester tester = new ComponentDbTester(db);
  private WsActionTester ws = new WsActionTester(new SetBaselineAction(dbClient, userSession, TestComponentFinder.from(db), branchDao));

  @Test
  @UseDataProvider("nullOrEmpty")
  public void set_baseline_on_main_branch(@Nullable String branchName) {
    ComponentDto project = tester.insertPrivateProject();
    SnapshotDto analysis = db.components().insertSnapshot(project);
    logInAsProjectAdministrator(project);

    call(project.getKey(), branchName, analysis.getUuid());

    NewCodePeriodDto loaded = dbClient.newCodePeriodDao().selectByBranch(dbSession, project.uuid(), project.uuid()).get();
    assertThat(loaded.getValue()).isEqualTo(analysis.getUuid());
    assertThat(loaded.getType()).isEqualTo(SPECIFIC_ANALYSIS);
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
  public void set_baseline_on_non_main_branch() {
    ComponentDto project = tester.insertPrivateProject();
    ComponentDto branchComponent = tester.insertProjectBranch(project);
    SnapshotDto analysis = db.components().insertSnapshot(branchComponent);
    BranchDto branch = branchDao.selectByUuid(dbSession, branchComponent.uuid()).get();
    logInAsProjectAdministrator(project);

    call(project.getKey(), branch.getKey(), analysis.getUuid());

    NewCodePeriodDto loaded = dbClient.newCodePeriodDao().selectByBranch(dbSession, project.uuid(), branch.getUuid()).get();
    assertThat(loaded.getValue()).isEqualTo(analysis.getUuid());
    assertThat(loaded.getType()).isEqualTo(SPECIFIC_ANALYSIS);
  }

  @Test
  public void fail_when_user_is_not_admin() {
    ComponentDto project = tester.insertPrivateProject();
    SnapshotDto analysis = db.components().insertSnapshot(project);

    assertThatThrownBy(() -> call(project.getKey(), DEFAULT_MAIN_BRANCH_NAME, analysis.getUuid()))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  @UseDataProvider("missingOrEmptyParamsAndFailureMessage")
  public void fail_with_IAE_when_required_param_missing_or_empty(Map<String, String> params, String message) {
    assertThatThrownBy(() -> call(params))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(message);
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
    ComponentDto project = tester.insertPrivateProject();
    SnapshotDto analysis = db.components().insertSnapshot(project);
    logInAsProjectAdministrator(project);

    Map<String, String> params = new HashMap<>();
    params.put(PARAM_PROJECT, project.getKey());
    params.put(PARAM_BRANCH, "master");
    params.put(PARAM_ANALYSIS, analysis.getUuid());
    params.putAll(nonexistentParams);

    assertThatThrownBy(() -> call(params))
      .isInstanceOf(NotFoundException.class);
  }

  @DataProvider
  public static Object[][] nonexistentParamsAndFailureMessage() {
    MapBuilder builder = new MapBuilder();

    return new Object[][] {
      {builder.put(PARAM_PROJECT, "nonexistent").map, "Project 'nonexistent' not found"},
      {builder.put(PARAM_BRANCH, "nonexistent").map, "Branch 'nonexistent' in project .* not found"},
      {builder.put(PARAM_ANALYSIS, "nonexistent").map, "Analysis 'nonexistent' is not found"},
    };
  }

  @Test
  public void fail_when_branch_does_not_belong_to_project() {
    ComponentDto project = tester.insertPrivateProject();
    SnapshotDto analysis = db.components().insertSnapshot(project);
    logInAsProjectAdministrator(project);

    ComponentDto otherProject = tester.insertPrivateProjectWithCustomBranch("develop");
    BranchDto branchOfOtherProject = branchDao.selectByUuid(dbSession, otherProject.uuid()).get();

    assertThatThrownBy(() -> call(project.getKey(), branchOfOtherProject.getKey(), analysis.getUuid()))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(String.format("Branch '%s' in project '%s' not found", branchOfOtherProject.getKey(), project.getKey()));
  }

  @Test
  public void fail_when_analysis_does_not_belong_to_main_branch_of_project() {
    ComponentDto project = tester.insertPrivateProjectWithCustomBranch("branch1");
    logInAsProjectAdministrator(project);

    ComponentDto otherProject = ComponentTesting.newPrivateProjectDto();
    SnapshotDto otherAnalysis = db.components().insertProjectAndSnapshot(otherProject);

    assertThatThrownBy(() ->  call(project.getKey(), "branch1", otherAnalysis.getUuid()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("Analysis '%s' does not belong to branch '%s' of project '%s'",
        otherAnalysis.getUuid(), "branch1", project.getKey()));
  }

  @Test
  public void fail_when_analysis_does_not_belong_to_non_main_branch_of_project() {
    ComponentDto project = tester.insertPrivateProject();
    tester.insertProjectBranch(project, b -> b.setKey("branch1"));
    logInAsProjectAdministrator(project);

    ComponentDto otherProject = ComponentTesting.newPrivateProjectDto();
    SnapshotDto otherAnalysis = db.components().insertProjectAndSnapshot(otherProject);

    assertThatThrownBy(() -> call(project.getKey(), "branch1", otherAnalysis.getUuid()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("Analysis '%s' does not belong to branch '%s' of project '%s'",
        otherAnalysis.getUuid(), "branch1", project.getKey()));
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
