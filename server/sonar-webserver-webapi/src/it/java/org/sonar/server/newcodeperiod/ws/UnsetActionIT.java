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

import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnsetActionIT {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ComponentFinder componentFinder = TestComponentFinder.from(db);
  private NewCodePeriodDao dao = new NewCodePeriodDao(System2.INSTANCE, UuidFactoryFast.getInstance());
  private PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private DocumentationLinkGenerator documentationLinkGenerator = mock(DocumentationLinkGenerator.class);
  private WsActionTester ws;

  @Before
  public void setup() {
    when(documentationLinkGenerator.getDocumentationLink(any())).thenReturn("https://docs.sonarsource.com/someddoc");
    ws = new WsActionTester(new UnsetAction(dbClient, userSession, componentFinder, editionProvider, dao, documentationLinkGenerator));
  }

  @Test
  public void test_definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.description()).contains("https://docs.sonarsource.com/someddoc");

    assertThat(definition.key()).isEqualTo("unset");
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.since()).isEqualTo("8.0");
    assertThat(definition.isPost()).isTrue();

    assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("project", "branch");
    assertThat(definition.param("project").isRequired()).isFalse();
    assertThat(definition.param("branch").isRequired()).isFalse();
  }

  // validation of project/branch
  @Test
  public void throw_IAE_if_branch_is_specified_without_project() {

    TestRequest request = ws.newRequest()
      .setParam("branch", "branch");
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("If branch key is specified, project key needs to be specified too");
  }

  @Test
  public void throw_NFE_if_project_not_found() {
    assertThatThrownBy(() -> ws.newRequest()
      .setParam("type", "previous_version")
      .setParam("project", "unknown")
      .execute())
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Project 'unknown' not found");
  }

  @Test
  public void throw_NFE_if_branch_not_found() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    logInAsProjectAdministrator(project);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "previous_version")
      .setParam("branch", "unknown")
      .execute())
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Branch 'unknown' in project '" + project.getKey() + "' not found");
  }

  // permission
  @Test
  public void throw_NFE_if_no_project_permission() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "previous_version")
      .execute())
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void throw_NFE_if_no_system_permission() {
    assertThatThrownBy(() -> ws.newRequest()
      .setParam("type", "previous_version")
      .execute())
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("Insufficient privileges");
  }

  // success cases
  @Test
  public void delete_global_period() {
    logInAsSystemAdministrator();
    ws.newRequest()
      .execute();

    assertTableEmpty();
  }

  @Test
  public void delete_project_period() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    logInAsProjectAdministrator(project);
    ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    assertTableEmpty();
  }

  @Test
  public void delete_project_period_twice() {
    ProjectDto project1 = db.components().insertPublicProject().getProjectDto();
    ProjectDto project2 = db.components().insertPublicProject().getProjectDto();
    db.newCodePeriods().insert(project1.getUuid(), null, NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid1");
    db.newCodePeriods().insert(project2.getUuid(), null, NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid2");

    logInAsProjectAdministrator(project1);
    ws.newRequest()
      .setParam("project", project1.getKey())
      .execute();
    assertTableContainsOnly(project2.getUuid(), null, NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid2");

    ws.newRequest()
      .setParam("project", project1.getKey())
      .execute();

    assertTableContainsOnly(project2.getUuid(), null, NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid2");
  }

  @Test
  public void delete_branch_period() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));

    db.newCodePeriods().insert(project.getUuid(), null, NewCodePeriodType.NUMBER_OF_DAYS, "20");
    db.newCodePeriods().insert(project.getUuid(), branch.getUuid(), NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid2");

    logInAsProjectAdministrator(project);

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .execute();

    assertTableContainsOnly(project.getUuid(), null, NewCodePeriodType.NUMBER_OF_DAYS, "20");
  }

  @Test
  public void delete_branch_and_project_period_in_community_edition() {
    ProjectData projectData = db.components().insertPublicProject();
    ProjectDto project = projectData.getProjectDto();

    db.newCodePeriods().insert(project.getUuid(), null, NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid1");
    db.newCodePeriods().insert(project.getUuid(), projectData.getMainBranchComponent().uuid(), NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid2");

    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));

    logInAsProjectAdministrator(project);

    ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    assertTableEmpty();
  }

  @Test
  public void throw_IAE_if_unset_branch_NCD_and_project_NCD_not_compliant() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));
    db.newCodePeriods().insert(project.getUuid(), null, NewCodePeriodType.NUMBER_OF_DAYS, "97");
    db.newCodePeriods().insert(project.getUuid(), branch.getUuid(), NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid");

    TestRequest request = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch");

    logInAsProjectAdministrator(project);
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Failed to unset the New Code Definition. Your project " +
        "New Code Definition is not compatible with the Clean as You Code methodology. Please update your project New Code Definition");

  }

  @Test
  public void throw_IAE_if_unset_branch_NCD_and_no_project_NCD_and_instance_NCD_not_compliant() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));
    db.newCodePeriods().insert(null, null, NewCodePeriodType.NUMBER_OF_DAYS, "97");
    db.newCodePeriods().insert(project.getUuid(), branch.getUuid(), NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid");

    TestRequest request = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch");

    logInAsProjectAdministrator(project);
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Failed to unset the New Code Definition. Your instance " +
        "New Code Definition is not compatible with the Clean as You Code methodology. Please update your instance New Code Definition");
  }

  @Test
  public void throw_IAE_if_unset_project_NCD_and_instance_NCD_not_compliant() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    db.newCodePeriods().insert(null, null, NewCodePeriodType.NUMBER_OF_DAYS, "97");
    db.newCodePeriods().insert(project.getUuid(), null, NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid");

    logInAsProjectAdministrator(project);

    TestRequest request = ws.newRequest()
      .setParam("project", project.getKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Failed to unset the New Code Definition. Your instance " +
        "New Code Definition is not compatible with the Clean as You Code methodology. Please update your instance New Code Definition");
  }

  @Test
  public void do_not_throw_IAE_if_unset_project_NCD_and_no_instance_NCD() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    db.newCodePeriods().insert(project.getUuid(), null, NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid");

    logInAsProjectAdministrator(project);
    ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    assertTableEmpty();
  }

  @Test
  public void do_not_throw_IAE_if_unset_branch_NCD_and_project_NCD_compliant() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));
    db.newCodePeriods().insert(project.getUuid(), branch.getUuid(), NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid");
    db.newCodePeriods().insert(project.getUuid(), null, NewCodePeriodType.PREVIOUS_VERSION, null);

    logInAsProjectAdministrator(project);
    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .execute();

    assertTableContainsOnly(project.getUuid(), null, NewCodePeriodType.PREVIOUS_VERSION, null);
  }

  @Test
  public void do_not_throw_IAE_if_unset_branch_NCD_and_project_NCD_not_compliant_and_no_branch_NCD() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    db.components().insertProjectBranch(project, b -> b.setKey("branch"));
    db.newCodePeriods().insert(project.getUuid(), null, NewCodePeriodType.NUMBER_OF_DAYS, "93");

    logInAsProjectAdministrator(project);
    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .execute();

    assertTableContainsOnly(project.getUuid(), null, NewCodePeriodType.NUMBER_OF_DAYS, "93");
  }

  @Test
  public void do_not_throw_IAE_if_unset_branch_NCD_and_no_project_NCD_and_instance_NCD_compliant() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));
    db.newCodePeriods().insert(project.getUuid(), branch.getUuid(), NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid");
    db.newCodePeriods().insert(null, null, NewCodePeriodType.PREVIOUS_VERSION, null);

    logInAsProjectAdministrator(project);
    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .execute();

    assertTableContainsOnly(null, null, NewCodePeriodType.PREVIOUS_VERSION, null);
  }

  @Test
  public void do_not_throw_IAE_if_unset_branch_NCD_and_no_project_NCD_and_no_instance() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));
    db.newCodePeriods().insert(project.getUuid(), branch.getUuid(), NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid");

    logInAsProjectAdministrator(project);
    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .execute();

    assertTableEmpty();
  }

  @Test
  public void do_not_throw_IAE_if_unset_project_NCD_and_instance_NCD_compliant() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));
    db.newCodePeriods().insert(null, null, NewCodePeriodType.PREVIOUS_VERSION, null);
    db.newCodePeriods().insert(project.getUuid(), null, NewCodePeriodType.PREVIOUS_VERSION, null);

    logInAsProjectAdministrator(project);
    ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    assertTableContainsOnly(null, null, NewCodePeriodType.PREVIOUS_VERSION, null);
  }

  @Test
  public void do_not_throw_IAE_if_unset_project_NCD_and_instance_NCD_not_compliant_and_no_project_NCD() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));
    db.newCodePeriods().insert(null, null, NewCodePeriodType.NUMBER_OF_DAYS, "93");

    logInAsProjectAdministrator(project);
    ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    assertTableContainsOnly(null, null, NewCodePeriodType.NUMBER_OF_DAYS, "93");
  }

  private void assertTableEmpty() {
    assertThat(db.countRowsOfTable(dbSession, "new_code_periods")).isZero();
  }

  private void assertTableContainsOnly(@Nullable String projectUuid, @Nullable String branchUuid, NewCodePeriodType type, @Nullable String value) {
    assertThat(db.countRowsOfTable(dbSession, "new_code_periods")).isOne();
    assertThat(db.selectFirst(dbSession, "select project_uuid, branch_uuid, type, value from new_code_periods"))
      .containsOnly(entry("PROJECT_UUID", projectUuid), entry("BRANCH_UUID", branchUuid), entry("TYPE", type.name()), entry("VALUE", value));
  }

  private void logInAsProjectAdministrator(ProjectDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }
}
