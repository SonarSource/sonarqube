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
package org.sonar.server.branch.ws;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.project.Project;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DeleteActionIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);
  private final ComponentFinder componentFinder = TestComponentFinder.from(db);
  private final ProjectLifeCycleListeners projectLifeCycleListeners = mock(ProjectLifeCycleListeners.class);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  public WsActionTester tester = new WsActionTester(new DeleteAction(db.getDbClient(), componentFinder, userSession, componentCleanerService, projectLifeCycleListeners));

  @Test
  public void delete_branch() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.components().insertProjectBranch(project, b -> b.setKey("branch1"));
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch1")
      .execute();

    verifyDeletedKey("branch1");
    verify(projectLifeCycleListeners).onProjectBranchesChanged(singleton(Project.fromProjectDtoWithTags(project)), emptySet());
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
      .hasMessageContaining("The 'branch' parameter is missing");
  }

  @Test
  public void fail_if_not_logged_in() {
    assertThatThrownBy(() -> tester.newRequest().execute())
      .isInstanceOf(UnauthorizedException.class)
      .hasMessageContaining("Authentication is required");
  }

  @Test
  public void fail_if_branch_does_not_exist() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch1")
      .execute())
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Branch 'branch1' not found");
  }

  @Test
  public void fail_if_project_does_not_exist() {
    userSession.logIn();

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("project", "foo")
      .setParam("branch", "branch1")
      .execute())
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Project 'foo' not found");
  }

  @Test
  public void definition() {
    WebService.Action definition = tester.getDef();
    assertThat(definition.key()).isEqualTo("delete");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("project", "branch");
    assertThat(definition.since()).isEqualTo("6.6");
  }

  private void verifyDeletedKey(String key) {
    ArgumentCaptor<BranchDto> argument = ArgumentCaptor.forClass(BranchDto.class);
    verify(componentCleanerService).deleteBranch(any(DbSession.class), argument.capture());
    assertThat(argument.getValue().getKey()).isEqualTo(key);
  }

}
