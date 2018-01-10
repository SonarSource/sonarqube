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
package org.sonar.server.usergroups.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.test.JsonAssert.assertJson;

public class UpdateActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private WsActionTester ws = new WsActionTester(
    new UpdateAction(db.getDbClient(), userSession, new GroupWsSupport(db.getDbClient(), defaultOrganizationProvider, new DefaultGroupFinder(db.getDbClient()))));

  @Test
  public void update_both_name_and_description() {
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user);
    db.users().insertMember(group, user);
    loginAsAdminOnDefaultOrganization();

    String result = newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "new-name")
      .setParam("description", "New Description")
      .execute().getInput();

    assertJson(result).isSimilarTo("{" +
      "  \"group\": {" +
      "    \"name\": \"new-name\"," +
      "    \"description\": \"New Description\"," +
      "    \"membersCount\": 1" +
      "  }" +
      "}");
  }

  @Test
  public void update_only_name() {
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    String result = newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "new-name")
      .execute().getInput();

    assertJson(result).isSimilarTo("{" +
      "  \"group\": {" +
      "    \"name\": \"new-name\"," +
      "    \"description\": \"" + group.getDescription() + "\"," +
      "    \"membersCount\": 0" +
      "  }" +
      "}");
  }

  @Test
  public void update_only_description() {
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    String result = newRequest()
      .setParam("id", group.getId().toString())
      .setParam("description", "New Description")
      .execute().getInput();

    assertJson(result).isSimilarTo("{" +
      "  \"group\": {" +
      "    \"name\": \"" + group.getName() + "\"," +
      "    \"description\": \"New Description\"," +
      "    \"membersCount\": 0" +
      "  }" +
      "}");
  }

  @Test
  public void return_default_field() {
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    String result = newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "new-name")
      .execute().getInput();

    assertJson(result).isSimilarTo("{" +
      "  \"group\": {" +
      "    \"name\": \"new-name\"," +
      "    \"description\": \"" + group.getDescription() + "\"," +
      "    \"membersCount\": 0," +
      "    \"default\": false" +
      "  }" +
      "}");
  }

  @Test
  public void require_admin_permission_on_organization() {
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    userSession.logIn("not-admin");

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute();
  }

  @Test
  public void fails_if_admin_of_another_organization_only() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    GroupDto group = db.users().insertGroup(org1, "group1");
    db.users().insertDefaultGroup(org1);
    db.users().insertDefaultGroup(org2);
    loginAsAdmin(org2);

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute();
  }

  @Test
  public void fail_if_name_is_too_short() {
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Group name cannot be empty");

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "")
      .execute();
  }

  @Test
  public void fail_if_new_name_is_anyone() {
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Anyone group cannot be used");

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "AnYoNe")
      .execute();
  }

  @Test
  public void fail_to_update_if_name_already_exists() {
    insertDefaultGroupOnDefaultOrganization();
    OrganizationDto defaultOrg = db.getDefaultOrganization();
    GroupDto groupToBeRenamed = db.users().insertGroup(defaultOrg, "a name");
    String newName = "new-name";
    db.users().insertGroup(defaultOrg, newName);
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(ServerException.class);
    expectedException.expectMessage("Group 'new-name' already exists");

    newRequest()
      .setParam("id", groupToBeRenamed.getId().toString())
      .setParam("name", newName)
      .execute();
  }

  @Test
  public void fail_if_unknown_group_id() {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Could not find a user group with id '42'.");

    newRequest()
      .setParam("id", "42")
      .execute();
  }

  @Test
  public void fail_to_update_default_group_name() {
    GroupDto group = db.users().insertDefaultGroup(db.getDefaultOrganization(), "default");
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Default group 'default' cannot be used to perform this action");

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "new name")
      .execute();
  }

  @Test
  public void fail_to_update_default_group_description() {
    GroupDto group = db.users().insertDefaultGroup(db.getDefaultOrganization(), "default");
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Default group 'default' cannot be used to perform this action");

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("description", "new description")
      .execute();
  }

  private TestRequest newRequest() {
    return ws.newRequest();
  }

  private void loginAsAdminOnDefaultOrganization() {
    loginAsAdmin(db.getDefaultOrganization());
  }

  private void loginAsAdmin(OrganizationDto org) {
    userSession.logIn().addPermission(ADMINISTER, org);
  }

  private void insertDefaultGroupOnDefaultOrganization() {
    db.users().insertDefaultGroup(db.getDefaultOrganization());
  }

}
