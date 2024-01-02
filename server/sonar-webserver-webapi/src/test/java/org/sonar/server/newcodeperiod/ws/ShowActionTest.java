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
package org.sonar.server.newcodeperiod.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.newcodeperiod.NewCodePeriodDbTester;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.NewCodePeriods;
import org.sonarqube.ws.NewCodePeriods.ShowWSResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ShowActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ComponentFinder componentFinder = TestComponentFinder.from(db);
  private NewCodePeriodDao dao = new NewCodePeriodDao(System2.INSTANCE, UuidFactoryFast.getInstance());
  private NewCodePeriodDbTester tester = new NewCodePeriodDbTester(db);
  private ShowAction underTest = new ShowAction(dbClient, userSession, componentFinder, dao);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("show");
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.since()).isEqualTo("8.0");
    assertThat(definition.isPost()).isFalse();

    assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("project", "branch");
    assertThat(definition.param("project").isRequired()).isFalse();
    assertThat(definition.param("branch").isRequired()).isFalse();
  }

  @Test
  public void throw_IAE_if_branch_is_specified_without_project() {
    assertThatThrownBy(() -> ws.newRequest()
      .setParam("branch", "branch")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("If branch key is specified, project key needs to be specified too");
  }

  @Test
  public void throw_FE_if_no_project_permission() {
    ComponentDto project = componentDb.insertPublicProject();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("project", project.getKey())
      .execute())
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void throw_FE_if_project_issue_admin() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectIssueAdmin(project);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("project", project.getKey())
      .execute())
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void show_global_setting() {
    tester.insert(new NewCodePeriodDto().setType(NewCodePeriodType.PREVIOUS_VERSION));

    ShowWSResponse response = ws.newRequest()
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, "", "", NewCodePeriods.NewCodePeriodType.PREVIOUS_VERSION, "", false);
  }

  @Test
  public void show_project_setting() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);

    tester.insert(new NewCodePeriodDto()
      .setProjectUuid(project.uuid())
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("4"));

    ShowWSResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, project.getKey(), "", NewCodePeriods.NewCodePeriodType.NUMBER_OF_DAYS, "4", false);
  }

  @Test
  public void show_branch_setting() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);

    ComponentDto branch = componentDb.insertProjectBranch(project, b -> b.setKey("branch"));

    tester.insert(new NewCodePeriodDto()
      .setProjectUuid(project.uuid())
      .setBranchUuid(branch.uuid())
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("1"));

    ShowWSResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, project.getKey(), "branch", NewCodePeriods.NewCodePeriodType.NUMBER_OF_DAYS, "1", false);
  }

  @Test
  public void show_inherited_project_setting() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);
    tester.insert(new NewCodePeriodDto().setType(NewCodePeriodType.PREVIOUS_VERSION));

    ShowWSResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, project.getKey(), "", NewCodePeriods.NewCodePeriodType.PREVIOUS_VERSION, "", true);
  }

  @Test
  public void show_inherited_branch_setting_from_project() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);

    ComponentDto branch = componentDb.insertProjectBranch(project, b -> b.setKey("branch"));

    tester.insert(new NewCodePeriodDto()
      .setProjectUuid(project.uuid())
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("1"));

    ShowWSResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, project.getKey(), "branch", NewCodePeriods.NewCodePeriodType.NUMBER_OF_DAYS, "1", true);
  }

  @Test
  public void show_inherited_branch_setting_from_global() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);
    ComponentDto branch = componentDb.insertProjectBranch(project, b -> b.setKey("branch"));
    tester.insert(new NewCodePeriodDto().setType(NewCodePeriodType.NUMBER_OF_DAYS).setValue("3"));

    ShowWSResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, project.getKey(), "branch", NewCodePeriods.NewCodePeriodType.NUMBER_OF_DAYS, "3", true);
  }

  @Test
  public void show_inherited_if_project_not_found() {
    tester.insert(new NewCodePeriodDto().setType(NewCodePeriodType.NUMBER_OF_DAYS).setValue("3"));

    ShowWSResponse response = ws.newRequest()
      .setParam("project", "unknown")
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, "", "", NewCodePeriods.NewCodePeriodType.NUMBER_OF_DAYS, "3", true);
  }

  @Test
  public void show_inherited_if_branch_not_found() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectScan(project);

    tester.insert(project.branchUuid(), NewCodePeriodType.NUMBER_OF_DAYS, "3");

    ShowWSResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "unknown")
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, project.getKey(), "", NewCodePeriods.NewCodePeriodType.NUMBER_OF_DAYS, "3", true);
  }

  private void assertResponse(ShowWSResponse response, String projectKey, String branchKey, NewCodePeriods.NewCodePeriodType type, String value, boolean inherited) {
    assertThat(response.getBranchKey()).isEqualTo(branchKey);
    assertThat(response.getProjectKey()).isEqualTo(projectKey);
    assertThat(response.getInherited()).isEqualTo(inherited);
    assertThat(response.getValue()).isEqualTo(value);
    assertThat(response.getType()).isEqualTo(type);
  }

  private void logInAsProjectAdministrator(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }

  private void logInAsProjectScan(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.SCAN, project);
  }

  private void logInAsProjectIssueAdmin(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.ISSUE_ADMIN, project);
  }

}
