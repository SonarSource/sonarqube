/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
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

public class DeleteActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);
  private ComponentFinder componentFinder = TestComponentFinder.from(db);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  public WsActionTester tester = new WsActionTester(new DeleteAction(db.getDbClient(), componentFinder, userSession, componentCleanerService));

  @Test
  public void test_definition() {
    WebService.Action definition = tester.getDef();
    assertThat(definition.key()).isEqualTo("delete");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("project", "branch");
    assertThat(definition.since()).isEqualTo("6.6");
  }

  @Test
  public void fail_if_missing_project_parameter() {
    userSession.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'project' parameter is missing");

    tester.newRequest().execute();
  }

  @Test
  public void fail_if_missing_branch_parameter() {
    userSession.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'branch' parameter is missing");

    tester.newRequest().setParam("project", "projectName").execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    tester.newRequest().execute();
  }

  public void fail_branch_does_not_exist() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Branch 'branch1' not found");

    tester.newRequest()
      .setParam("project", file.getDbKey())
      .setParam("branch", "branch1")
      .execute();
  }

  @Test
  public void fail_if_project_does_not_exist() {
    userSession.logIn();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project key 'foo' not found");

    tester.newRequest()
      .setParam("project", "foo")
      .setParam("branch", "branch1")
      .execute();
  }

  @Test
  public void fail_if_branch_is_main() {
    ComponentDto project = db.components().insertMainBranch();
    db.executeUpdateSql("UPDATE project_branches set KEE = 'main'");
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    // not found because the DB keys don't contain the name
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Only non-main branches can be deleted");

    tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "main")
      .execute();
  }

  @Test
  public void delete_branch() {

    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch1"));

    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch1")
      .execute();
    verifyDeletedKey(branch.getDbKey());
  }

  private void verifyDeletedKey(String key) {
    ArgumentCaptor<ComponentDto> argument = ArgumentCaptor.forClass(ComponentDto.class);
    verify(componentCleanerService).deleteBranch(any(DbSession.class), argument.capture());
    assertThat(argument.getValue().getDbKey()).isEqualTo(key);
  }

}
