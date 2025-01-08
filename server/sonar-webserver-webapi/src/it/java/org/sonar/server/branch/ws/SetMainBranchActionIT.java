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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.Indexers;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.project.Project;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.ACTION_SET_MAIN_BRANCH;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_BRANCH;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_PROJECT;

public class SetMainBranchActionIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public LogTester logTester = new LogTester().setLevel(Level.INFO);
  ProjectLifeCycleListeners projectLifeCycleListeners = mock(ProjectLifeCycleListeners.class);

  private final Indexers indexers = mock(Indexers.class);
  private WsActionTester tester = new WsActionTester(new SetMainBranchAction(db.getDbClient(), userSession, projectLifeCycleListeners, indexers));

  @Test
  public void testDefinition() {
    WebService.Action definition = tester.getDef();
    assertThat(definition.key()).isEqualTo(ACTION_SET_MAIN_BRANCH);
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("project", "branch");
    assertThat(definition.since()).isEqualTo("10.2");
  }

  @Test
  public void fail_whenProjectParameterIsMissing_shouldThrowIllegalArgumentException() {
    userSession.logIn();

    assertThatThrownBy(tester.newRequest()::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The 'project' parameter is missing");
  }

  @Test
  public void fail_whenBranchParameterIsMissing_shouldIllegalArgumentException() {
    userSession.logIn();

    TestRequest request = tester.newRequest()
      .setParam(PARAM_PROJECT, "projectKey");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The 'branch' parameter is missing");
  }

  @Test
  public void fail_whenNotLoggedIn_shouldThrowUnauthorizedException() {
    TestRequest request = tester.newRequest()
      .setParam(PARAM_PROJECT, "project")
      .setParam(PARAM_BRANCH, "anotherBranch");

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class)
      .hasMessageContaining("Authentication is required");
  }

  @Test
  public void fail_whenNoAdministerPermission_shouldThrowForbiddenException() {
    userSession.logIn();
    ProjectDto projectDto = db.components().insertPublicProject().getProjectDto();

    TestRequest request = tester.newRequest()
      .setParam(PARAM_PROJECT, projectDto.getKey())
      .setParam(PARAM_BRANCH, "anotherBranch");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void fail_whenProjectIsNotFound_shouldThrowNotFoundException() {
    userSession.logIn();

    TestRequest request = tester.newRequest()
      .setParam(PARAM_PROJECT, "noExistingProjectKey")
      .setParam(PARAM_BRANCH, "aBranch");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Project 'noExistingProjectKey' not found.");
  }

  @Test
  public void fail_whenKeyPassedIsApplicationKey_shouldThrowIllegalArgumentException() {
    userSession.logIn();
    ProjectData application = db.components().insertPublicApplication();
    userSession.addProjectPermission(UserRole.ADMIN, application.getProjectDto());

    TestRequest request = tester.newRequest()
      .setParam(PARAM_PROJECT, application.projectKey())
      .setParam(PARAM_BRANCH, "aBranch");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Project '%s' not found.".formatted(application.projectKey()));
  }

  @Test
  public void fail_whenNewMainBranchIsNotFound_shouldThrowNotFoundException() {
    userSession.logIn();

    ProjectData projectData = db.components().insertPublicProject();
    userSession.addProjectPermission(UserRole.ADMIN, projectData.getProjectDto());

    String nonExistingBranch = "aNonExistingBranch";
    TestRequest request = tester.newRequest()
      .setParam(PARAM_PROJECT, projectData.projectKey())
      .setParam(PARAM_BRANCH, nonExistingBranch);

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Branch '%s' not found for project '%s'.".formatted(nonExistingBranch, projectData.projectKey()));
  }

  @Test
  public void fail_whenProjectHasNoMainBranch_shouldThrowIllegalStateException() {
    userSession.logIn();
    ProjectDto project = insertProjectWithoutMainBranch();
    userSession.addProjectPermission(UserRole.ADMIN, project);

    TestRequest request = tester.newRequest()
      .setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_BRANCH, "anotherBranch");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("No main branch for existing project '%s'".formatted(project.getKey()));
  }

  private ProjectDto insertProjectWithoutMainBranch() {
    ProjectDto project = ComponentTesting.newProjectDto();
    db.getDbClient().projectDao().insert(db.getSession(), project);
    db.commit();
    return project;
  }

  @Test
  public void log_whenOldBranchAndNewBranchAreSame_shouldThrowServerException() {
    userSession.logIn();

    ProjectData projectData = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.ADMIN, projectData.getProjectDto());

    TestRequest request = tester.newRequest()
      .setParam(PARAM_PROJECT, projectData.projectKey())
      .setParam(PARAM_BRANCH, projectData.getMainBranchDto().getKey());

    request.execute();

    assertThat(logTester.logs(Level.INFO))
      .containsOnly("Branch '%s' is already the main branch.".formatted(projectData.getMainBranchDto().getKey()));
  }

  @Test
  public void setNewMainBranch_shouldConfigureNewBranchAsMainBranchAndKeepThePreviousExcludeFromPurge() {
    userSession.logIn();

    ProjectData projectData = db.components().insertPrivateProject();
    BranchDto newMainBranch = db.components().insertProjectBranch(projectData.getProjectDto(), branchDto -> branchDto.setKey("newMain"));
    userSession.addProjectPermission(UserRole.ADMIN, projectData.getProjectDto());

    tester.newRequest()
      .setParam(PARAM_PROJECT, projectData.projectKey())
      .setParam(PARAM_BRANCH, newMainBranch.getKey()).execute();

    checkCallToProjectLifeCycleListenersOnProjectBranchesChanges(projectData.getProjectDto(), projectData.getMainBranchDto().getUuid());
    verify(indexers).commitAndIndexBranches(any(), eq(List.of(projectData.getMainBranchDto(), newMainBranch)), eq(Indexers.BranchEvent.SWITCH_OF_MAIN_BRANCH));
    checkNewMainBranch(projectData.projectUuid(), newMainBranch.getUuid());
    checkPreviousMainBranch(projectData);
    assertThat(logTester.logs(Level.INFO))
      .containsOnly("The new main branch of project '%s' is '%s' (Previous one : '%s')"
        .formatted(projectData.projectKey(), newMainBranch.getKey(), projectData.getMainBranchDto().getKey()));
  }

  private void checkCallToProjectLifeCycleListenersOnProjectBranchesChanges(ProjectDto projectDto, String oldMainBranchUuid) {
    Project project = Project.from(projectDto);
    verify(projectLifeCycleListeners).onProjectBranchesChanged(Set.of(project), Set.of(oldMainBranchUuid) );
  }

  private void checkNewMainBranch(String projectUuid, String newBranchUuid) {
    Optional<BranchDto> branchDto = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), projectUuid);
    assertThat(branchDto).isPresent();
    assertThat(branchDto.get().getUuid()).isEqualTo(newBranchUuid);
    assertThat(branchDto.get().isExcludeFromPurge()).isTrue();
  }

  private void checkPreviousMainBranch(ProjectData projectData) {
    Optional<BranchDto> branchDto1 = db.getDbClient().branchDao().selectByUuid(db.getSession(), projectData.getMainBranchDto().getUuid());
    assertThat(branchDto1).isPresent();
    BranchDto oldBranchAfterSetting = branchDto1.get();
    assertThat(oldBranchAfterSetting.isMain()).isFalse();
    assertThat(oldBranchAfterSetting.isExcludeFromPurge()).isTrue();
  }

}
