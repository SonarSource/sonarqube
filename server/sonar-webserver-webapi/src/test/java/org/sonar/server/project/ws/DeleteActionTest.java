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
package org.sonar.server.project.ws;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.user.UserDto;
import org.sonar.db.webhook.WebhookDbTester;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.project.Project;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.component.TestComponentFinder.from;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;

public class DeleteActionTest {

  private static final String ACTION = "delete";

  private System2 system2 = System2.INSTANCE;

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private WebhookDbTester webhookDbTester = db.webhooks();
  private ComponentDbTester componentDbTester = new ComponentDbTester(db);
  private ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);
  private ProjectLifeCycleListeners projectLifeCycleListeners = mock(ProjectLifeCycleListeners.class);
  private DeleteAction underTest = new DeleteAction(
    componentCleanerService,
    from(db),
    dbClient,
    userSessionRule, projectLifeCycleListeners);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void organization_administrator_deletes_project_by_key() {
    ComponentDto project = componentDbTester.insertPrivateProject();
    userSessionRule.logIn().addPermission(ADMINISTER, project.getOrganizationUuid());

    call(tester.newRequest().setParam(PARAM_PROJECT, project.getDbKey()));

    assertThat(verifyDeletedKey()).isEqualTo(project.getDbKey());
    verify(projectLifeCycleListeners).onProjectsDeleted(singleton(Project.from(project)));
  }

  @Test
  public void project_administrator_deletes_the_project_by_key() {
    ComponentDto project = componentDbTester.insertPrivateProject();
    userSessionRule.logIn().addProjectPermission(ADMIN, project);

    call(tester.newRequest().setParam(PARAM_PROJECT, project.getDbKey()));

    assertThat(verifyDeletedKey()).isEqualTo(project.getDbKey());
    verify(projectLifeCycleListeners).onProjectsDeleted(singleton(Project.from(project)));
  }

  @Test
  public void project_deletion_also_ensure_that_homepage_on_this_project_if_it_exists_is_cleared() {
    ComponentDto project = componentDbTester.insertPrivateProject();
    UserDto insert = dbClient.userDao().insert(dbSession,
      newUserDto().setHomepageType("PROJECT").setHomepageParameter(project.uuid()));
    dbSession.commit();
    userSessionRule.logIn().addProjectPermission(ADMIN, project);
    DeleteAction underTest = new DeleteAction(
      new ComponentCleanerService(dbClient, new ResourceTypesRule().setAllQualifiers(PROJECT),
        new TestProjectIndexers()),
      from(db), dbClient, userSessionRule, projectLifeCycleListeners);

    new WsActionTester(underTest)
      .newRequest()
      .setParam(PARAM_PROJECT, project.getDbKey())
      .execute();

    UserDto userReloaded = dbClient.userDao().selectUserById(dbSession, insert.getId());
    assertThat(userReloaded.getHomepageType()).isNull();
    assertThat(userReloaded.getHomepageParameter()).isNull();
  }

  @Test
  public void project_deletion_also_ensure_that_webhooks_on_this_project_if_they_exists_are_deleted() {
    ComponentDto project = componentDbTester.insertPrivateProject();
    webhookDbTester.insertWebhook(project);
    webhookDbTester.insertWebhook(project);
    webhookDbTester.insertWebhook(project);
    webhookDbTester.insertWebhook(project);

    userSessionRule.logIn().addProjectPermission(ADMIN, project);
    DeleteAction underTest = new DeleteAction(
      new ComponentCleanerService(dbClient, new ResourceTypesRule().setAllQualifiers(PROJECT),
        new TestProjectIndexers()),
      from(db), dbClient, userSessionRule, projectLifeCycleListeners);

    new WsActionTester(underTest)
      .newRequest()
      .setParam(PARAM_PROJECT, project.getDbKey())
      .execute();

    List<WebhookDto> webhookDtos = dbClient.webhookDao().selectByProject(dbSession, project);
    assertThat(webhookDtos).isEmpty();
  }

  @Test
  public void return_403_if_not_project_admin_nor_org_admin() {
    ComponentDto project = componentDbTester.insertPrivateProject();

    userSessionRule.logIn()
      .addProjectPermission(UserRole.CODEVIEWER, project)
      .addProjectPermission(UserRole.ISSUE_ADMIN, project)
      .addProjectPermission(UserRole.USER, project);
    expectedException.expect(ForbiddenException.class);

    call(tester.newRequest().setParam(PARAM_PROJECT, project.getDbKey()));
  }

  @Test
  public void return_401_if_not_logged_in() {
    ComponentDto project = componentDbTester.insertPrivateProject();

    userSessionRule.anonymous();
    expectedException.expect(UnauthorizedException.class);

    call(tester.newRequest().setParam(PARAM_PROJECT, project.getDbKey()));
  }

  @Test
  public void fail_when_using_branch_db_key() {
    ComponentDto project = db.components().insertMainBranch();
    userSessionRule.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(String.format("Component key '%s' not found", branch.getDbKey()));

    call(tester.newRequest().setParam(PARAM_PROJECT, branch.getDbKey()));
  }

  private String verifyDeletedKey() {
    ArgumentCaptor<ComponentDto> argument = ArgumentCaptor.forClass(ComponentDto.class);
    verify(componentCleanerService).delete(any(DbSession.class), argument.capture());
    return argument.getValue().getDbKey();
  }

  private void call(TestRequest request) {
    TestResponse result = request.execute();
    result.assertNoContent();
  }
}
