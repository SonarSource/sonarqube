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
package org.sonar.server.source.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class HashActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private WsActionTester tester = new WsActionTester(new HashAction(db.getDbClient(), userSessionRule, TestComponentFinder.from(db)));

  @Test
  public void show_hashes() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organizationDto);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    FileSourceDto fileSource = db.fileSources().insertFileSource(file, f -> f.setLineHashes(singletonList("ABC")));
    loginAsProjectViewer(project);

    TestRequest request = tester.newRequest().setParam("key", file.getDbKey());

    assertThat(request.execute().getInput()).isEqualTo("ABC");
  }

  @Test
  public void hashes_empty_if_no_source() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organizationDto);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    loginAsProjectViewer(project);

    TestRequest request = tester.newRequest().setParam("key", file.getKey());

    assertThat(request.execute().getStatus()).isEqualTo(204);
  }

  @Test
  public void fail_to_show_hashes_if_file_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    tester.newRequest().setParam("key", "unknown").execute();
  }

  @Test
  public void fail_when_using_branch_db_key() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    loginAsProjectViewer(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    tester.newRequest().setParam("key", branch.getDbKey()).execute();
  }

  @Test
  public void fail_on_missing_permission() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organizationDto);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    FileSourceDto fileSource = db.fileSources().insertFileSource(file);
    userSessionRule.logIn(db.users().insertUser());

    expectedException.expect(ForbiddenException.class);

    tester.newRequest().setParam("key", file.getKey()).execute();
  }

  private void loginAsProjectViewer(ComponentDto project) {
    userSessionRule.logIn(db.users().insertUser()).addProjectPermission(USER, project);
  }
}
