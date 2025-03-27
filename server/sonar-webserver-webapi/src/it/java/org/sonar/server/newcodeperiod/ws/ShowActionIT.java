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
package org.sonar.server.newcodeperiod.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.newcodeperiod.NewCodePeriodDbTester;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.NewCodePeriods;
import org.sonarqube.ws.NewCodePeriods.ShowWSResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.newcodeperiod.ws.NewCodePeriodsWsUtils.DOCUMENTATION_LINK;
import static org.sonarqube.ws.NewCodePeriods.NewCodePeriodType.NUMBER_OF_DAYS;
import static org.sonarqube.ws.NewCodePeriods.NewCodePeriodType.PREVIOUS_VERSION;

public class ShowActionIT {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = db.getDbClient();
  private ComponentFinder componentFinder = TestComponentFinder.from(db);
  private NewCodePeriodDao dao = new NewCodePeriodDao(System2.INSTANCE, UuidFactoryFast.getInstance());
  private NewCodePeriodDbTester tester = new NewCodePeriodDbTester(db);
  private DocumentationLinkGenerator documentationLinkGenerator = mock(DocumentationLinkGenerator.class);
  private WsActionTester ws;

  @Before
  public void setup() {
    when(documentationLinkGenerator.getDocumentationLink(any())).thenReturn("https://docs.sonarsource.com/someddoc" + DOCUMENTATION_LINK);
    ws = new WsActionTester(new ShowAction(dbClient, userSession, componentFinder, dao, documentationLinkGenerator));
  }

  @Test
  public void test_definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.description()).contains("https://docs.sonarsource.com/someddoc");

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
    ProjectDto project = db.components().insertPublicProject().getProjectDto();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("project", project.getKey())
      .execute())
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void throw_FE_if_project_issue_admin() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
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

    assertResponse(response, "", "", PREVIOUS_VERSION, "", false, null);
  }

  @Test
  public void show_project_setting() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    logInAsProjectAdministrator(project);

    var ncd = tester.insert(new NewCodePeriodDto()
      .setProjectUuid(project.getUuid())
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setPreviousNonCompliantValue("100")
      .setValue("90"));

    ShowWSResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, project.getKey(), "", NUMBER_OF_DAYS, "90", false, "100");
    assertUpdatedAt(response, ncd.getUpdatedAt());
  }

  @Test
  public void show_branch_setting() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    logInAsProjectAdministrator(project);

    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));

    tester.insert(new NewCodePeriodDto()
      .setProjectUuid(project.getUuid())
      .setBranchUuid(branch.getUuid())
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("1"));

    ShowWSResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, project.getKey(), "branch", NUMBER_OF_DAYS, "1", false, null);
  }

  @Test
  public void show_inherited_project_setting() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    logInAsProjectAdministrator(project);
    tester.insert(new NewCodePeriodDto().setType(NewCodePeriodType.PREVIOUS_VERSION));

    ShowWSResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, project.getKey(), "", PREVIOUS_VERSION, "", true, null);
  }

  @Test
  public void show_inherited_branch_setting_from_project() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    logInAsProjectAdministrator(project);

    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));

    tester.insert(new NewCodePeriodDto()
      .setProjectUuid(project.getUuid())
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("1"));

    ShowWSResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, project.getKey(), "branch", NUMBER_OF_DAYS, "1", true, null);
  }

  @Test
  public void show_inherited_branch_setting_from_global() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    logInAsProjectAdministrator(project);
    BranchDto branchDto = db.components().insertProjectBranch(project, b -> b.setKey("branch"));
    tester.insert(new NewCodePeriodDto().setType(NewCodePeriodType.NUMBER_OF_DAYS).setValue("3"));

    ShowWSResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, project.getKey(), "branch", NUMBER_OF_DAYS, "3", true, null);
  }

  @Test
  public void show_inherited_if_project_not_found() {
    tester.insert(new NewCodePeriodDto().setType(NewCodePeriodType.NUMBER_OF_DAYS).setValue("3"));

    ShowWSResponse response = ws.newRequest()
      .setParam("project", "unknown")
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, "", "", NUMBER_OF_DAYS, "3", true, null);
  }

  @Test
  public void show_inherited_if_branch_not_found() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    logInAsProjectScan(project);

    tester.insert(project.getUuid(), NewCodePeriodType.NUMBER_OF_DAYS, "3");

    ShowWSResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "unknown")
      .executeProtobuf(ShowWSResponse.class);

    assertResponse(response, project.getKey(), "", NUMBER_OF_DAYS, "3", true, null);
  }

  private void assertResponse(ShowWSResponse response, String projectKey, String branchKey, NewCodePeriods.NewCodePeriodType type, String value, boolean inherited,
    String previousNonCompliantValue) {
    assertThat(response.getBranchKey()).isEqualTo(branchKey);
    assertThat(response.getProjectKey()).isEqualTo(projectKey);
    assertThat(response.getInherited()).isEqualTo(inherited);
    assertThat(response.getValue()).isEqualTo(value);
    assertThat(response.getType()).isEqualTo(type);
    assertThat(response.getPreviousNonCompliantValue()).isEqualTo(previousNonCompliantValue == null ? "" : previousNonCompliantValue);
  }

  private static void assertUpdatedAt(ShowWSResponse response, long currentTime) {
    assertThat(response.getUpdatedAt()).isEqualTo(currentTime);
  }

  private void logInAsProjectAdministrator(ProjectDto project) {
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);
  }

  private void logInAsProjectScan(ProjectDto project) {
    userSession.logIn().addProjectPermission(ProjectPermission.SCAN, project);
  }

  private void logInAsProjectIssueAdmin(ProjectDto project) {
    userSession.logIn().addProjectPermission(ProjectPermission.ISSUE_ADMIN, project);
  }

}
