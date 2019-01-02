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
package org.sonar.server.branch.pr.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.component.BranchType.PULL_REQUEST;

public class DeleteActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);
  private ComponentFinder componentFinder = TestComponentFinder.from(db);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  public WsActionTester ws = new WsActionTester(new DeleteAction(db.getDbClient(), componentFinder, userSession, componentCleanerService));

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("delete");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("project", "pullRequest");
    assertThat(definition.since()).isEqualTo("7.1");
  }

  @Test
  public void delete_pull_request() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("1984").setBranchType(PULL_REQUEST));

    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("pullRequest", "1984")
      .execute();
    verifyDeletedKey(branch.getDbKey());
  }

  @Test
  public void fail_if_missing_project_parameter() {
    userSession.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'project' parameter is missing");

    ws.newRequest().execute();
  }

  @Test
  public void fail_if_missing_pull_request_parameter() {
    userSession.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'pullRequest' parameter is missing");

    ws.newRequest().setParam("project", "projectName").execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    ws.newRequest().execute();
  }

  @Test
  public void fail_if_pull_request_does_not_exist() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setDbKey("orwell"));
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Pull request '1984' is not found for project 'orwell'");

    ws.newRequest()
      .setParam("project", project.getDbKey())
      .setParam("pullRequest", "1984")
      .execute();
  }

  @Test
  public void fail_if_project_does_not_exist() {
    userSession.logIn();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project key 'foo' not found");

    ws.newRequest()
      .setParam("project", "foo")
      .setParam("pullRequest", "123")
      .execute();
  }

  private void verifyDeletedKey(String key) {
    ArgumentCaptor<ComponentDto> argument = ArgumentCaptor.forClass(ComponentDto.class);
    verify(componentCleanerService).deleteBranch(any(DbSession.class), argument.capture());
    assertThat(argument.getValue().getDbKey()).isEqualTo(key);
  }

}
