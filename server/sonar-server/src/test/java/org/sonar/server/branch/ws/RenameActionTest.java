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

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.resources.Qualifiers.PROJECT;

public class RenameActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(PROJECT);
  private ComponentFinder componentFinder = new ComponentFinder(db.getDbClient(), resourceTypes);
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

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'project' parameter is missing");

    tester.newRequest().execute();
  }

  @Test
  public void fail_if_missing_branch_parameter() {
    userSession.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is missing");

    tester.newRequest().setParam("project", "projectName").execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    tester.newRequest().execute();
  }

  @Test
  public void fail_if_no_administer_permission() {
    userSession.logIn();
    ComponentDto project = db.components().insertMainBranch();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("name", "branch1")
      .execute();
  }

  @Test
  public void rename() {
    userSession.logIn();
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));
    userSession.addProjectPermission(UserRole.ADMIN, project);

    tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("name", "master")
      .execute();

    assertThat(db.countRowsOfTable("project_branches")).isEqualTo(2);
    Optional<BranchDto> mainBranch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.uuid());
    assertThat(mainBranch.get().getKey()).isEqualTo("master");

    Optional<BranchDto> unchangedBranch = db.getDbClient().branchDao().selectByUuid(db.getSession(), branch.uuid());
    assertThat(unchangedBranch.get().getKey()).isEqualTo("branch");
  }

  @Test
  public void rename_with_same_name() {
    userSession.logIn();
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));
    userSession.addProjectPermission(UserRole.ADMIN, project);

    tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("name", "master")
      .execute();

    tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("name", "master")
      .execute();

    assertThat(db.countRowsOfTable("project_branches")).isEqualTo(2);
    Optional<BranchDto> mainBranch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.uuid());
    assertThat(mainBranch.get().getKey()).isEqualTo("master");

    Optional<BranchDto> unchangedBranch = db.getDbClient().branchDao().selectByUuid(db.getSession(), branch.uuid());
    assertThat(unchangedBranch.get().getKey()).isEqualTo("branch");
  }

  @Test
  public void fail_if_name_already_used() {
    userSession.logIn();
    ComponentDto project = db.components().insertMainBranch();
    userSession.addProjectPermission(UserRole.ADMIN, project);
    db.components().insertProjectBranch(project, b -> b.setKey("branch"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Impossible to update branch name: a branch with name \"branch\" already exists");

    tester.newRequest()
      .setParam("project", project.getKey())
      .setParam("name", "branch")
      .execute();
  }

  @Test
  public void fail_if_project_does_not_exist() {
    userSession.logIn();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project key 'foo' not found");

    tester.newRequest()
      .setParam("project", "foo")
      .setParam("name", "branch1")
      .execute();
  }
}
