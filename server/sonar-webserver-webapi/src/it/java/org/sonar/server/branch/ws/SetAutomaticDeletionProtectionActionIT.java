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
package org.sonar.server.branch.ws;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.component.ComponentTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;

public class SetAutomaticDeletionProtectionActionIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private ComponentTypes componentTypes = new ComponentTypesRule().setRootQualifiers(PROJECT);
  private ComponentFinder componentFinder = new ComponentFinder(db.getDbClient(), componentTypes);
  private WsActionTester tester = new WsActionTester(new SetAutomaticDeletionProtectionAction(db.getDbClient(), userSession, componentFinder));

  @Test
  public void test_definition() {
    WebService.Action definition = tester.getDef();
    assertThat(definition.key()).isEqualTo("set_automatic_deletion_protection");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("project", "branch", "value");
    assertThat(definition.since()).isEqualTo("8.1");
  }

  @Test
  public void fail_if_missing_project_parameter() {
    userSession.logIn();

    assertThatThrownBy(() -> tester.newRequest().execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The 'project' parameter is missing");
  }

  @Test
  public void fail_if_missing_branch_parameter() {
    userSession.logIn();

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("project", "projectName")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The 'branch' parameter is missing");
  }

  @Test
  public void fail_if_missing_value_parameter() {
    userSession.logIn();

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("project", "projectName")
      .setParam("branch", "foobar")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The 'value' parameter is missing");
  }

  @Test
  public void fail_if_not_logged_in() {
    assertThatThrownBy(() -> tester.newRequest().execute())
      .isInstanceOf(UnauthorizedException.class)
      .hasMessageContaining("Authentication is required");
  }

  @Test
  public void fail_if_no_administer_permission() {
    userSession.logIn();
    ProjectDto project = db.components().insertPublicProject().getProjectDto();

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch1")
      .setParam("value", "true")
      .execute())
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void fail_when_attempting_to_set_main_branch_as_included_in_purge() {
    userSession.logIn();
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch1").setExcludeFromPurge(false));
    userSession.addProjectPermission(ProjectPermission.ADMIN, project);

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", DEFAULT_MAIN_BRANCH_NAME)
      .setParam("value", "false")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Main branch of the project is always excluded from automatic deletion.");
  }

  @Test
  public void set_purge_exclusion() {
    userSession.logIn();
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch1").setExcludeFromPurge(false));
    userSession.addProjectPermission(ProjectPermission.ADMIN, project);

    tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch1")
      .setParam("value", "true")
      .execute();

    assertThat(db.countRowsOfTable("project_branches")).isEqualTo(2);
    Optional<BranchDto> mainBranch = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), project.getUuid());
    assertThat(mainBranch.get().getKey()).isEqualTo(DEFAULT_MAIN_BRANCH_NAME);
    assertThat(mainBranch.get().isExcludeFromPurge()).isTrue();

    Optional<BranchDto> branchDto = db.getDbClient().branchDao().selectByUuid(db.getSession(), branch.getUuid());
    assertThat(branchDto.get().getKey()).isEqualTo("branch1");
    assertThat(branchDto.get().isExcludeFromPurge()).isTrue();
  }

  @Test
  public void fail_on_non_boolean_value_parameter() {
    userSession.logIn();
    ProjectDto project = db.components().insertPublicProject().getProjectDto();

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch1")
      .setParam("value", "foobar")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Value of parameter 'value' (foobar) must be one of: [true, false, yes, no]");
  }

  @Test
  public void fail_if_project_does_not_exist() {
    userSession.logIn();

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("project", "foo")
      .setParam("branch", "branch1")
      .setParam("value", "true")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Project 'foo' not found");
  }

  @Test
  public void fail_if_branch_does_not_exist() {
    userSession.logIn();
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    userSession.addProjectPermission(ProjectPermission.ADMIN, project);

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch1")
      .setParam("value", "true")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Branch 'branch1' not found for project '" + project.getKey() + "'");
  }
}
