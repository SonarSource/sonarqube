/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProviderRule;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_LOGIN;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_ORGANIZATION_KEY;

public class RemoveUserActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DefaultOrganizationProviderRule defaultOrganizationProvider = DefaultOrganizationProviderRule.create(db);
  private WsTester ws;

  @Before
  public void setUp() {
    GroupWsSupport groupSupport = new GroupWsSupport(db.getDbClient(), defaultOrganizationProvider);
    ws = new WsTester(new UserGroupsWs(new RemoveUserAction(db.getDbClient(), userSession, groupSupport)));
  }

  @Test
  public void does_nothing_if_user_is_not_in_group() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "admins");
    UserDto user = db.users().insertUser("my-admin");

    loginAsAdmin();
    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("login", user.getLogin())
      .execute()
      .assertNoContent();

    assertThat(db.users().selectGroupIdsOfUser(user)).isEmpty();
  }

  @Test
  public void remove_user_by_group_id() throws Exception {
    GroupDto users = db.users().insertGroup(defaultOrganizationProvider.getDto(), "users");
    UserDto user = db.users().insertUser("my-admin");
    db.users().insertMember(users, user);

    loginAsAdmin();
    newRequest()
      .setParam("id", users.getId().toString())
      .setParam("login", user.getLogin())
      .execute()
      .assertNoContent();

    assertThat(db.users().selectGroupIdsOfUser(user)).isEmpty();
  }

  @Test
  public void remove_user_by_group_name_in_default_organization() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "group_name");
    UserDto user = db.users().insertUser("user_login");
    db.users().insertMember(group, user);

    loginAsAdmin();
    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute()
      .assertNoContent();

    assertThat(db.users().selectGroupIdsOfUser(user)).isEmpty();
  }

  @Test
  public void remove_user_by_group_name_in_specific_organization() throws Exception {
    OrganizationDto org = db.organizations().insert();
    GroupDto group = db.users().insertGroup(org, "a_group");
    UserDto user = db.users().insertUser("user_login");
    db.users().insertMember(group, user);

    loginAsAdmin();
    newRequest()
      .setParam(PARAM_ORGANIZATION_KEY, org.getKey())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute()
      .assertNoContent();

    assertThat(db.users().selectGroupIdsOfUser(user)).isEmpty();
  }

  @Test
  public void remove_user_only_from_one_group() throws Exception {
    OrganizationDto defaultOrg = defaultOrganizationProvider.getDto();
    GroupDto users = db.users().insertGroup(defaultOrg, "user");
    GroupDto admins = db.users().insertGroup(defaultOrg, "admins");
    UserDto user = db.users().insertUser("user");
    db.users().insertMember(users, user);
    db.users().insertMember(admins, user);

    loginAsAdmin();
    newRequest()
      .setParam("id", admins.getId().toString())
      .setParam("login", user.getLogin())
      .execute()
      .assertNoContent();

    assertThat(db.users().selectGroupIdsOfUser(user)).containsOnly(users.getId());
  }

  @Test
  public void fail_if_unknown_group() throws Exception {
    UserDto user = db.users().insertUser("my-admin");

    expectedException.expect(NotFoundException.class);

    loginAsAdmin();
    newRequest()
      .setParam("id", "42")
      .setParam("login", user.getLogin())
      .execute();
  }

  @Test
  public void fail_if_unknown_user() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "admins");

    expectedException.expect(NotFoundException.class);

    loginAsAdmin();
    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("login", "my-admin")
      .execute();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest("api/user_groups", "remove_user");
  }

  private void loginAsAdmin() {
    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

}
