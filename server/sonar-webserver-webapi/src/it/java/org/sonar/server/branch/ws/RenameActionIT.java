/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
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

public class RenameActionIT {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private ComponentTypes componentTypes = new ComponentTypesRule().setRootQualifiers(PROJECT);
  private ComponentFinder componentFinder = new ComponentFinder(db.getDbClient(), componentTypes);
  private WsActionTester tester = new WsActionTester(new RenameAction(db.getDbClient(), componentFinder, userSession));

  @Test
  public void test_definition() {
    WebService.Action definition = tester.getDef();
    assertThat(definition.key()).isEqualTo("rename");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("project", "name");
    assertThat(definition.since()).isEqualTo("6.6");
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

    assertThatThrownBy(() -> tester.newRequest().setParam("project", "projectName").execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The 'name' parameter is missing");
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
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("name", "branch1")
      .execute())
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void rename() {
    userSession.logIn();
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));
    userSession.addProjectPermission(ProjectPermission.ADMIN, project);

    tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("name", "master")
      .execute();

    assertThat(db.countRowsOfTable("project_branches")).isEqualTo(2);
    Optional<BranchDto> mainBranch = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), project.getUuid());
    assertThat(mainBranch.get().getKey()).isEqualTo("master");

    Optional<BranchDto> unchangedBranch = db.getDbClient().branchDao().selectByUuid(db.getSession(), branch.getUuid());
    assertThat(unchangedBranch.get().getKey()).isEqualTo("branch");
  }

  @Test
  public void whenRenameIsTriggered_eventualBranchReferencesPeriods_areUpdatedToo() {
    userSession.logIn();
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    db.newCodePeriods().insert(getBranchReferenceNewCodePeriod(project));
    userSession.addProjectPermission(ProjectPermission.ADMIN, project);

    tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("name", "newBranchName")
      .execute();

    assertThat(db.countRowsOfTable("project_branches")).isEqualTo(1);
    Optional<BranchDto> mainBranch = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), project.getUuid());
    assertThat(mainBranch.get().getKey()).isEqualTo("newBranchName");

    Optional<NewCodePeriodDto> updatedCodeReference = db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), project.getUuid());
    assertThat(updatedCodeReference.get().getValue()).isEqualTo("newBranchName");
  }

  private NewCodePeriodDto getBranchReferenceNewCodePeriod(ProjectDto project) {
    Optional<BranchDto> mainBranch = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), project.getUuid());
    return new NewCodePeriodDto()
      .setProjectUuid(project.getUuid())
      .setValue(mainBranch.get().getBranchKey())
      .setType(NewCodePeriodType.REFERENCE_BRANCH);
  }

  @Test
  public void rename_with_same_name() {
    userSession.logIn();
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));
    userSession.addProjectPermission(ProjectPermission.ADMIN, project);

    tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("name", "master")
      .execute();

    tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("name", "master")
      .execute();

    assertThat(db.countRowsOfTable("project_branches")).isEqualTo(2);
    Optional<BranchDto> mainBranch = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), project.getUuid());
    assertThat(mainBranch.get().getKey()).isEqualTo("master");

    Optional<BranchDto> unchangedBranch = db.getDbClient().branchDao().selectByUuid(db.getSession(), branch.getUuid());
    assertThat(unchangedBranch.get().getKey()).isEqualTo("branch");
  }

  @Test
  public void fail_if_name_already_used() {
    userSession.logIn();
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    userSession.addProjectPermission(ProjectPermission.ADMIN, project);
    db.components().insertProjectBranch(project, b -> b.setKey("branch"));

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("name", "branch")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Impossible to update branch name: a branch with name \"branch\" already exists");
  }

  @Test
  public void fail_if_project_does_not_exist() {
    userSession.logIn();

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("project", "foo")
      .setParam("name", "branch1")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Project 'foo' not found");
  }
}
