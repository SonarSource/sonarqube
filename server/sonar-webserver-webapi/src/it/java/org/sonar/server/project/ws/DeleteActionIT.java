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
package org.sonar.server.project.ws;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.webhook.WebhookDbTester;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.es.TestIndexers;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.project.DeletedProject;
import org.sonar.server.project.Project;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.component.TestComponentFinder.from;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;

public class DeleteActionIT {
  private final System2 system2 = System2.INSTANCE;

  @Rule
  public final DbTester db = DbTester.create(system2);
  @Rule
  public final UserSessionRule userSessionRule = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final WebhookDbTester webhookDbTester = db.webhooks();
  private final ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);
  private final ProjectLifeCycleListeners projectLifeCycleListeners = mock(ProjectLifeCycleListeners.class);

  private final DeleteAction underTest = new DeleteAction(
    componentCleanerService,
    from(db),
    dbClient,
    userSessionRule, projectLifeCycleListeners);
  private final WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void global_administrator_deletes_project_by_key() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto rootComponent = projectData.getMainBranchComponent();
    userSessionRule.logIn().addPermission(GlobalPermission.ADMINISTER);

    call(tester.newRequest().setParam(PARAM_PROJECT, rootComponent.getKey()));

    assertThat(verifyDeletedKey()).isEqualTo(rootComponent.getKey());
    Project from = Project.from(projectData.getProjectDto());
    String mainBranchUuid = projectData.getMainBranchDto().getUuid();
    verify(projectLifeCycleListeners).onProjectsDeleted(singleton(new DeletedProject(from, mainBranchUuid)));
  }

  @Test
  public void project_administrator_deletes_the_project_by_key() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    userSessionRule.logIn().addProjectPermission(UserRole.ADMIN, projectData.getProjectDto());

    call(tester.newRequest().setParam(PARAM_PROJECT, project.getKey()));

    assertThat(verifyDeletedKey()).isEqualTo(project.getKey());
    verify(projectLifeCycleListeners).onProjectsDeleted(singleton(new DeletedProject(Project.from(projectData.getProjectDto()),projectData.getMainBranchDto().getUuid())));
  }

  @Test
  public void project_deletion_also_ensure_that_homepage_on_this_project_if_it_exists_is_cleared() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    UserDto insert = dbClient.userDao().insert(dbSession,
      newUserDto().setHomepageType("PROJECT").setHomepageParameter(projectData.projectUuid()));
    dbSession.commit();
    userSessionRule.logIn().addProjectPermission(UserRole.ADMIN, projectData.getProjectDto());
    DeleteAction underTest = new DeleteAction(
      new ComponentCleanerService(dbClient, new TestIndexers()),
      from(db), dbClient, userSessionRule, projectLifeCycleListeners);

    new WsActionTester(underTest)
      .newRequest()
      .setParam(PARAM_PROJECT, project.getKey())
      .execute();

    UserDto userReloaded = dbClient.userDao().selectByUuid(dbSession, insert.getUuid());
    assertThat(userReloaded.getHomepageType()).isNull();
    assertThat(userReloaded.getHomepageParameter()).isNull();
  }

  @Test
  public void project_deletion_also_ensure_that_webhooks_on_this_project_if_they_exists_are_deleted() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    webhookDbTester.insertWebhook(project);
    webhookDbTester.insertWebhook(project);
    webhookDbTester.insertWebhook(project);
    webhookDbTester.insertWebhook(project);

    userSessionRule.logIn().addProjectPermission(UserRole.ADMIN, project);
    DeleteAction underTest = new DeleteAction(
      new ComponentCleanerService(dbClient, new TestIndexers()),
      from(db), dbClient, userSessionRule, projectLifeCycleListeners);

    new WsActionTester(underTest)
      .newRequest()
      .setParam(PARAM_PROJECT, project.getKey())
      .execute();

    List<WebhookDto> webhookDtos = dbClient.webhookDao().selectByProject(dbSession, project);
    assertThat(webhookDtos).isEmpty();
  }

  @Test
  public void return_403_if_not_project_admin_nor_org_admin() {
    ProjectData project = db.components().insertPrivateProject();

    userSessionRule.logIn()
      .addProjectPermission(UserRole.CODEVIEWER, project.getProjectDto())
      .addProjectPermission(UserRole.ISSUE_ADMIN, project.getProjectDto())
      .addProjectPermission(UserRole.USER, project.getProjectDto());

    TestRequest request = tester.newRequest().setParam(PARAM_PROJECT, project.projectKey());
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void return_401_if_not_logged_in() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    userSessionRule.anonymous();

    TestRequest request = tester.newRequest().setParam(PARAM_PROJECT, project.getKey());
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(UnauthorizedException.class);
  }

  private String verifyDeletedKey() {
    ArgumentCaptor<ProjectDto> argument = ArgumentCaptor.forClass(ProjectDto.class);
    verify(componentCleanerService).deleteEntity(any(DbSession.class), argument.capture());
    return argument.getValue().getKey();
  }

  private void call(TestRequest request) {
    TestResponse result = request.execute();
    result.assertNoContent();
  }
}
