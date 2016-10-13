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

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.organization.DefaultOrganizationProviderRule;
import org.sonar.server.platform.PersistentSettings;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateActionTest {

  private static final String DEFAULT_GROUP_NAME_KEY = "sonar.defaultGroup";
  private static final String DEFAULT_GROUP_NAME_VALUE = "DEFAULT_GROUP_NAME_VALUE";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DefaultOrganizationProviderRule defaultOrganizationProvider = DefaultOrganizationProviderRule.create(db);
  private PersistentSettings settings = mock(PersistentSettings.class);
  private WsTester ws = new WsTester(
    new UserGroupsWs(new UpdateAction(db.getDbClient(), userSession, new GroupWsSupport(db.getDbClient(), defaultOrganizationProvider), settings, defaultOrganizationProvider)));

  @Before
  public void setUp() throws Exception {
    GroupWsSupport groupSupport = new GroupWsSupport(db.getDbClient(), defaultOrganizationProvider);
    ws = new WsTester(new UserGroupsWs(new UpdateAction(db.getDbClient(), userSession, groupSupport, settings, defaultOrganizationProvider)));
    when(settings.getString(DEFAULT_GROUP_NAME_KEY)).thenReturn(DEFAULT_GROUP_NAME_VALUE);
  }

  @Test
  public void update_both_name_and_description() throws Exception {
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    db.users().insertMember(group, user);
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "new-name")
      .setParam("description", "New Description")
      .execute().assertJson("{" +
        "  \"group\": {" +
        "    \"name\": \"new-name\"," +
        "    \"description\": \"New Description\"," +
        "    \"membersCount\": 1" +
        "  }" +
        "}");
  }

  @Test
  public void update_only_name() throws Exception {
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "new-name")
      .execute().assertJson("{" +
        "  \"group\": {" +
        "    \"name\": \"new-name\"," +
        "    \"description\": \"" + group.getDescription() + "\"," +
        "    \"membersCount\": 0" +
        "  }" +
        "}");
  }

  @Test
  public void update_only_description() throws Exception {
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("description", "New Description")
      .execute().assertJson("{" +
        "  \"group\": {" +
        "    \"name\": \"" + group.getName() + "\"," +
        "    \"description\": \"New Description\"," +
        "    \"membersCount\": 0" +
        "  }" +
        "}");
  }

  @Test
  public void update_default_group_name_also_update_default_group_property() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), DEFAULT_GROUP_NAME_VALUE);
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "new-name")
      .execute();

    verify(settings).saveProperty(any(DbSession.class), eq(DEFAULT_GROUP_NAME_KEY), eq("new-name"));
  }

  @Test
  public void update_default_group_name_does_not_update_default_group_setting_when_null() throws Exception {
    when(settings.getString(DEFAULT_GROUP_NAME_KEY)).thenReturn(null);
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), DEFAULT_GROUP_NAME_VALUE);
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "new-name")
      .execute();

    verify(settings, never()).saveProperty(any(DbSession.class), eq(DEFAULT_GROUP_NAME_KEY), eq("new-name"));
  }

  @Test
  public void do_not_update_default_group_of_default_organization_if_updating_group_on_non_default_organization() throws Exception {
    OrganizationDto org = db.organizations().insert();
    when(settings.getString(DEFAULT_GROUP_NAME_KEY)).thenReturn(DEFAULT_GROUP_NAME_VALUE);
    GroupDto groupInDefaultOrg = db.users().insertGroup(defaultOrganizationProvider.getDto(), DEFAULT_GROUP_NAME_VALUE);
    GroupDto group = db.users().insertGroup(org, DEFAULT_GROUP_NAME_VALUE);
    loginAsAdmin(org);

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "new-name")
      .execute();

    verify(settings, never()).saveProperty(any(DbSession.class), eq(DEFAULT_GROUP_NAME_KEY), eq("new-name"));
  }

  @Test
  public void require_admin_permission_on_organization() throws Exception {
    GroupDto group = db.users().insertGroup();
    userSession.login("not-admin");

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute();
  }

  @Test
  public void fails_if_admin_of_another_organization_only() throws Exception {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    GroupDto group = db.users().insertGroup(org1, "group1");
    loginAsAdmin(org2);

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute();
  }

  @Test
  public void fail_if_name_is_too_short() throws Exception {
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
  public void fail_if_name_is_too_long() throws Exception {
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Group name cannot be longer than 255 characters");

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", StringUtils.repeat("a", 255 + 1))
      .execute();
  }

  @Test
  public void fail_if_new_name_is_anyone() throws Exception {
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
  public void fail_to_update_if_name_already_exists() throws Exception {
    OrganizationDto defaultOrg = defaultOrganizationProvider.getDto();
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
  public void fail_if_description_is_too_long() throws Exception {
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Description cannot be longer than 200 characters");

    newRequest()
      .setParam("id", group.getId().toString())
      .setParam("name", "long-group-description-is-looooooooooooong")
      .setParam("description", StringUtils.repeat("a", 201))
      .execute();
  }

  @Test
  public void fail_if_unknown_group_id() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Could not find a user group with id '42'.");

    newRequest()
      .setParam("id", "42")
      .execute();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest("api/user_groups", "update");
  }

  private void loginAsAdminOnDefaultOrganization() {
    loginAsAdmin(db.getDefaultOrganization());
  }

  private void loginAsAdmin(OrganizationDto org) {
    userSession.login().addOrganizationPermission(org.getUuid(), GlobalPermissions.SYSTEM_ADMIN);
  }
}
