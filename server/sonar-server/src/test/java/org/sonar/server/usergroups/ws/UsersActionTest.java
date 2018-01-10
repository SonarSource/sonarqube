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
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_ID;
import static org.sonar.test.JsonAssert.assertJson;

public class UsersActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private WsActionTester ws = new WsActionTester(
    new UsersAction(db.getDbClient(), userSession, new GroupWsSupport(db.getDbClient(), defaultOrganizationProvider, new DefaultGroupFinder(db.getDbClient()))));

  @Test
  public void fail_if_unknown_group_id() {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No group with id '42'");

    newUsersRequest()
      .setParam("id", "42")
      .setParam("login", "john").execute();
  }

  @Test
  public void fail_if_not_admin_of_organization() {
    GroupDto group = db.users().insertGroup();
    userSession.logIn("not-admin");

    expectedException.expect(ForbiddenException.class);

    newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam("login", "john").execute();
  }

  @Test
  public void fail_if_admin_of_other_organization_only() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    GroupDto group = db.users().insertGroup(org1, "the-group");
    loginAsAdmin(org2);

    expectedException.expect(ForbiddenException.class);

    newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam("login", "john").execute();
  }

  @Test
  public void group_has_no_users() {
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    String result = newUsersRequest()
      .setParam("login", "john")
      .setParam("id", group.getId().toString())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"p\": 1,\n" +
      "  \"total\": 0,\n" +
      "  \"users\": []\n" +
      "}");
  }

  @Test
  public void return_members_by_group_id() {
    GroupDto group = db.users().insertGroup();
    UserDto lovelace = db.users().insertUser(newUserDto().setLogin("ada.login").setName("Ada Lovelace"));
    db.organizations().addMember(db.getDefaultOrganization(), lovelace);
    UserDto hopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper"));
    db.organizations().addMember(db.getDefaultOrganization(), hopper);
    db.users().insertMember(group, lovelace);
    loginAsAdminOnDefaultOrganization();

    String result = newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"users\": [\n" +
      "    {\"login\": \"ada.login\", \"name\": \"Ada Lovelace\", \"selected\": true},\n" +
      "    {\"login\": \"grace\", \"name\": \"Grace Hopper\", \"selected\": false}\n" +
      "  ]\n" +
      "}\n");
  }

  @Test
  public void references_group_by_its_name() {
    OrganizationDto org = db.organizations().insert();
    GroupDto group = db.users().insertGroup(org, "the-group");
    UserDto lovelace = db.users().insertUser(newUserDto().setLogin("ada.login").setName("Ada Lovelace"));
    UserDto hopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper"));
    db.users().insertMember(group, lovelace);
    db.organizations().addMember(org, lovelace);
    db.organizations().addMember(org, hopper);
    loginAsAdmin(org);

    String result = newUsersRequest()
      .setParam("organization", org.getKey())
      .setParam("name", group.getName())
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"users\": [\n" +
      "    {\"login\": \"ada.login\", \"name\": \"Ada Lovelace\", \"selected\": true},\n" +
      "    {\"login\": \"grace\", \"name\": \"Grace Hopper\", \"selected\": false}\n" +
      "  ]\n" +
      "}\n");
  }

  @Test
  public void references_group_in_default_organization_by_its_name() {
    GroupDto group = db.users().insertGroup();
    UserDto lovelace = db.users().insertUser(newUserDto().setLogin("ada.login").setName("Ada Lovelace"));
    db.organizations().addMember(db.getDefaultOrganization(), lovelace);
    UserDto hopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper"));
    db.organizations().addMember(db.getDefaultOrganization(), hopper);
    db.users().insertMember(group, lovelace);
    loginAsAdminOnDefaultOrganization();

    String result = newUsersRequest()
      .setParam("name", group.getName())
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"users\": [\n" +
      "    {\"login\": \"ada.login\", \"name\": \"Ada Lovelace\", \"selected\": true},\n" +
      "    {\"login\": \"grace\", \"name\": \"Grace Hopper\", \"selected\": false}\n" +
      "  ]\n" +
      "}\n");
  }

  @Test
  public void filter_members_by_name() {
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "a group");
    UserDto adaLovelace = db.users().insertUser(newUserDto().setLogin("ada").setName("Ada Lovelace"));
    db.organizations().addMember(db.getDefaultOrganization(), adaLovelace);
    UserDto graceHopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper"));
    db.organizations().addMember(db.getDefaultOrganization(), graceHopper);
    db.users().insertMember(group, adaLovelace);
    db.users().insertMember(group, graceHopper);
    loginAsAdminOnDefaultOrganization();

    String response = newUsersRequest().setParam(PARAM_GROUP_ID, group.getId().toString()).execute().getInput();

    assertThat(response).contains("Ada Lovelace", "Grace Hopper");
  }

  @Test
  public void selected_users() {
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "a group");
    UserDto lovelace = db.users().insertUser(newUserDto().setLogin("ada").setName("Ada Lovelace"));
    db.organizations().addMember(db.getDefaultOrganization(), lovelace);
    UserDto hopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper"));
    db.organizations().addMember(db.getDefaultOrganization(), hopper);
    db.users().insertMember(group, lovelace);
    loginAsAdminOnDefaultOrganization();

    assertJson(newUsersRequest()
      .setParam("id", group.getId().toString())
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"users\": [\n" +
        "    {\"login\": \"ada\", \"name\": \"Ada Lovelace\", \"selected\": true}\n" +
        "  ]\n" +
        "}");

    assertJson(newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam(Param.SELECTED, SelectionMode.SELECTED.value())
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"users\": [\n" +
        "    {\"login\": \"ada\", \"name\": \"Ada Lovelace\", \"selected\": true}\n" +
        "  ]\n" +
        "}");
  }

  @Test
  public void deselected_users() {
    GroupDto group = db.users().insertGroup();
    UserDto lovelace = db.users().insertUser(newUserDto().setLogin("ada").setName("Ada Lovelace"));
    db.organizations().addMember(db.getDefaultOrganization(), lovelace);
    UserDto hopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper"));
    db.organizations().addMember(db.getDefaultOrganization(), hopper);
    db.users().insertMember(group, lovelace);
    loginAsAdminOnDefaultOrganization();

    String result = newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam(Param.SELECTED, SelectionMode.DESELECTED.value())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"users\": [\n" +
      "    {\"login\": \"grace\", \"name\": \"Grace Hopper\", \"selected\": false}\n" +
      "  ]\n" +
      "}");
  }

  @Test
  public void paging() {
    GroupDto group = db.users().insertGroup();
    UserDto lovelace = db.users().insertUser(newUserDto().setLogin("ada").setName("Ada Lovelace"));
    db.organizations().addMember(db.getDefaultOrganization(), lovelace);
    UserDto hopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper"));
    db.organizations().addMember(db.getDefaultOrganization(), hopper);
    db.users().insertMember(group, lovelace);
    loginAsAdminOnDefaultOrganization();

    assertJson(newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam("ps", "1")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"p\": 1,\n" +
        "  \"ps\": 1,\n" +
        "  \"total\": 2,\n" +
        "  \"users\": [\n" +
        "    {\"login\": \"ada\", \"name\": \"Ada Lovelace\", \"selected\": true}\n" +
        "  ]\n" +
        "}");

    assertJson(newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam("ps", "1")
      .setParam("p", "2")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"p\": 2,\n" +
        "  \"ps\": 1,\n" +
        "  \"total\": 2,\n" +
        "  \"users\": [\n" +
        "    {\"login\": \"grace\", \"name\": \"Grace Hopper\", \"selected\": false}\n" +
        "  ]\n" +
        "}");
  }

  @Test
  public void filtering_by_name_email_and_login() {
    GroupDto group = db.users().insertGroup();
    UserDto lovelace = db.users().insertUser(newUserDto().setLogin("ada.login").setName("Ada Lovelace").setEmail("ada@email.com"));
    db.organizations().addMember(db.getDefaultOrganization(), lovelace);
    UserDto hopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper").setEmail("grace@hopper.com"));
    db.organizations().addMember(db.getDefaultOrganization(), hopper);
    db.users().insertMember(group, lovelace);
    loginAsAdminOnDefaultOrganization();

    assertJson(newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam("q", "ace")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"users\": [\n" +
        "    {\"login\": \"ada.login\", \"name\": \"Ada Lovelace\", \"selected\": true},\n" +
        "    {\"login\": \"grace\", \"name\": \"Grace Hopper\", \"selected\": false}\n" +
        "  ]\n" +
        "}\n");

    assertJson(newUsersRequest().setParam("id", group.getId().toString())
      .setParam("q", ".logi")
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"users\": [\n" +
        "    {\n" +
        "      \"login\": \"ada.login\",\n" +
        "      \"name\": \"Ada Lovelace\",\n" +
        "      \"selected\": true\n" +
        "    }\n" +
        "  ]\n" +
        "}\n");

    assertJson(newUsersRequest().setParam("id", group.getId().toString())
      .setParam("q", "OvE")
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"users\": [\n" +
        "    {\n" +
        "      \"login\": \"ada.login\",\n" +
        "      \"name\": \"Ada Lovelace\",\n" +
        "      \"selected\": true\n" +
        "    }\n" +
        "  ]\n" +
        "}\n");

    assertJson(newUsersRequest().setParam("id", group.getId().toString())
      .setParam("q", "mail")
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"users\": [\n" +
        "    {\n" +
        "      \"login\": \"ada.login\",\n" +
        "      \"name\": \"Ada Lovelace\",\n" +
        "      \"selected\": true\n" +
        "    }\n" +
        "  ]\n" +
        "}\n");
  }

  private TestRequest newUsersRequest() {
    return ws.newRequest();
  }

  private void loginAsAdminOnDefaultOrganization() {
    loginAsAdmin(db.getDefaultOrganization());
  }

  private void loginAsAdmin(OrganizationDto org) {
    userSession.logIn().addPermission(ADMINISTER, org);
  }
}
