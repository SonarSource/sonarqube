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
package org.sonar.server.usergroups.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_LOGIN;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_ORGANIZATION_KEY;

public class AddUserActionTest {

  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private WsActionTester ws = new WsActionTester(new AddUserAction(db.getDbClient(), userSession, newGroupWsSupport()));

  @Test
  public void add_user_to_group_referenced_by_its_id() {
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user);
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("login", user.getLogin())
      .execute();

    assertThat(db.users().selectGroupIdsOfUser(user)).containsOnly(group.getId());
  }

  @Test
  public void add_user_to_group_referenced_by_its_name() {
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user);
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(db.users().selectGroupIdsOfUser(user)).containsOnly(group.getId());
  }

  @Test
  public void add_user_to_group_referenced_by_its_name_and_organization() {
    OrganizationDto org = db.organizations().insert();
    db.users().insertDefaultGroup(org);
    GroupDto group = db.users().insertGroup(org, "a-group");
    UserDto user = db.users().insertUser("user_login");
    db.organizations().addMember(org, user);
    loginAsAdmin(org);

    newRequest()
      .setParam(PARAM_ORGANIZATION_KEY, org.getKey())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(db.users().selectGroupIdsOfUser(user)).containsOnly(group.getId());
  }

  @Test
  public void add_user_to_another_group() {
    insertDefaultGroupOnDefaultOrganization();
    OrganizationDto defaultOrg = db.getDefaultOrganization();
    GroupDto admins = db.users().insertGroup(defaultOrg, "admins");
    GroupDto users = db.users().insertGroup(defaultOrg, "users");
    UserDto user = db.users().insertUser("my-admin");
    db.organizations().addMember(db.getDefaultOrganization(), user);
    db.users().insertMember(users, user);
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", admins.getId().toString())
      .setParam("login", user.getLogin())
      .execute();

    assertThat(db.users().selectGroupIdsOfUser(user)).containsOnly(admins.getId(), users.getId());
  }

  @Test
  public void do_not_fail_if_user_is_already_member_of_group() {
    insertDefaultGroupOnDefaultOrganization();
    GroupDto users = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user);
    db.users().insertMember(users, user);
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", users.getId().toString())
      .setParam("login", user.getLogin())
      .execute();

    // do not insert duplicated row
    assertThat(db.users().selectGroupIdsOfUser(user)).hasSize(1).containsOnly(users.getId());
  }

  @Test
  public void group_has_multiple_members() {
    insertDefaultGroupOnDefaultOrganization();
    GroupDto users = db.users().insertGroup();
    UserDto user1 = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user1);
    UserDto user2 = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user2);
    db.users().insertMember(users, user1);
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", users.getId().toString())
      .setParam("login", user2.getLogin())
      .execute();

    assertThat(db.users().selectGroupIdsOfUser(user1)).containsOnly(users.getId());
    assertThat(db.users().selectGroupIdsOfUser(user2)).containsOnly(users.getId());
  }

  @Test
  public void response_status_is_no_content() {
    db.users().insertDefaultGroup(db.getDefaultOrganization());
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user);
    loginAsAdminOnDefaultOrganization();

    TestResponse response = newRequest()
      .setParam("id", group.getId().toString())
      .setParam("login", user.getLogin())
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
  }

  @Test
  public void fail_if_group_does_not_exist() {
    UserDto user = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user);
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No group with id '42'");

    newRequest()
      .setParam("id", "42")
      .setParam("login", user.getLogin())
      .execute();
  }

  @Test
  public void fail_if_user_does_not_exist() {
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "admins");
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Could not find a user with login 'my-admin'");

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("login", "my-admin")
      .execute();
  }

  @Test
  public void fail_if_not_administrator() throws Exception {
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user);

    expectedException.expect(UnauthorizedException.class);

    executeRequest(group, user);
  }

  @Test
  public void fail_if_administrator_of_another_organization() {
    OrganizationDto org1 = db.organizations().insert();
    GroupDto group = db.users().insertGroup(org1, "a-group");
    UserDto user = db.users().insertUser("user_login");
    db.organizations().addMember(db.getDefaultOrganization(), user);
    OrganizationDto org2 = db.organizations().insert();
    loginAsAdmin(org2);

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam(PARAM_ORGANIZATION_KEY, org1.getKey())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();
  }

  @Test
  public void fail_to_add_user_to_group_when_user_is_not_member_of_given_organization() {
    OrganizationDto org = db.organizations().insert(organizationDto -> organizationDto.setKey("Organization key"));
    GroupDto group = db.users().insertGroup(org, "a-group");
    UserDto user = db.users().insertUser("user_login");
    OrganizationDto otherOrganization = db.organizations().insert();
    db.organizations().addMember(otherOrganization, user);
    loginAsAdmin(org);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("User 'user_login' is not member of organization 'Organization key'");

    newRequest()
      .setParam(PARAM_ORGANIZATION_KEY, org.getKey())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();
  }

  @Test
  public void fail_to_add_user_to_anyone() {
    OrganizationDto organization = db.organizations().insert(org -> org.setKey("org"));
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    loginAsAdmin(organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No group with name 'Anyone' in organization 'org'");

    newRequest()
      .setParam(PARAM_GROUP_NAME, ANYONE)
      .setParam(PARAM_LOGIN, user.getLogin())
      .setParam(PARAM_ORGANIZATION_KEY, organization.getKey())
      .execute();
  }

  @Test
  public void fail_to_add_user_to_default_group() {
    UserDto user = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user);
    GroupDto defaultGroup = db.users().insertDefaultGroup(db.getDefaultOrganization(), "default");
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Default group 'default' cannot be used to perform this action");

    newRequest()
      .setParam("id", Integer.toString(defaultGroup.getId()))
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();
  }

  @Test
  public void fail_when_no_default_group() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto group = db.users().insertGroup(organization);
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    loginAsAdmin(organization);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default group cannot be found");

    newRequest()
      .setParam(PARAM_LOGIN, user.getLogin())
      .setParam(PARAM_ORGANIZATION_KEY, organization.getKey())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();
  }

  private void executeRequest(GroupDto groupDto, UserDto userDto) {
    newRequest()
      .setParam("id", groupDto.getId().toString())
      .setParam("login", userDto.getLogin())
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

  private GroupWsSupport newGroupWsSupport() {
    return new GroupWsSupport(db.getDbClient(), defaultOrganizationProvider, new DefaultGroupFinder(db.getDbClient()));
  }

}
