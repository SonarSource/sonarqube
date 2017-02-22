/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.organization.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchMyOrganizationsActionTest {
  private static final String NO_ORGANIZATIONS_RESPONSE = "{\"organizations\": []}";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();

  private WsActionTester underTest = new WsActionTester(new SearchMyOrganizationsAction(userSessionRule, dbClient));

  @Test
  public void verify_definition() {
    WebService.Action def = underTest.getDef();

    assertThat(def.key()).isEqualTo("search_my_organizations");
    assertThat(def.isPost()).isFalse();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.since()).isEqualTo("6.3");
    assertThat(def.description()).isEqualTo("List keys of the organizations for which the currently authenticated user has the System Administer permission for.");
    assertThat(def.responseExample()).isNotNull();

    assertThat(def.params()).isEmpty();
  }

  @Test
  public void verify_response_example() {
    OrganizationDto organization1 = dbTester.organizations().insertForKey("my-org");
    OrganizationDto organization2 = dbTester.organizations().insertForKey("foo-corp");

    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertPermissionOnUser(organization1, user, SYSTEM_ADMIN);
    dbTester.users().insertPermissionOnUser(organization2, user, SYSTEM_ADMIN);

    userSessionRule.logIn(user);

    TestResponse response = underTest.newRequest().execute();

    assertJson(response.getInput()).isSimilarTo(underTest.getDef().responseExampleAsString());
  }

  @Test
  public void returns_empty_response_when_user_is_not_logged_in() {
    TestResponse response = underTest.newRequest().execute();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(response.getInput()).isEmpty();
  }

  @Test
  public void returns_empty_array_when_user_is_logged_in_and_has_no_permission_on_anything() {
    userSessionRule.logIn();

    TestResponse response = underTest.newRequest().execute();

    assertJson(response.getInput()).isSimilarTo(NO_ORGANIZATIONS_RESPONSE);
  }

  @Test
  public void returns_organizations_of_authenticated_user_when_user_has_ADMIN_user_permission_on_some_organization() {
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertPermissionOnUser(dbTester.getDefaultOrganization(), user, SYSTEM_ADMIN);
    OrganizationDto organization1 = dbTester.organizations().insert();
    dbTester.users().insertPermissionOnUser(organization1, user, SYSTEM_ADMIN);
    UserDto otherUser = dbTester.users().insertUser();
    OrganizationDto organization2 = dbTester.organizations().insert();
    dbTester.users().insertPermissionOnUser(organization2, otherUser, SYSTEM_ADMIN);

    userSessionRule.logIn(user);
    assertJson(underTest.newRequest().execute().getInput()).isSimilarTo("{\"organizations\": [" +
      "\"" + dbTester.getDefaultOrganization().getKey() + "\"," +
      "\"" + organization1.getKey() + "\"" +
      "]}");

    userSessionRule.logIn(otherUser);
    assertJson(underTest.newRequest().execute().getInput()).isSimilarTo("{\"organizations\": [" +
      "\"" + organization2.getKey() + "\"" +
      "]}");

    userSessionRule.logIn();
    assertJson(underTest.newRequest().execute().getInput()).isSimilarTo(NO_ORGANIZATIONS_RESPONSE);
  }

  @Test
  public void returns_organizations_of_authenticated_user_when_user_has_ADMIN_group_permission_on_some_organization() {
    UserDto user = dbTester.users().insertUser();
    GroupDto defaultGroup = dbTester.users().insertGroup(dbTester.getDefaultOrganization());
    dbTester.users().insertPermissionOnGroup(defaultGroup, ADMINISTER);
    dbTester.users().insertMember(defaultGroup, user);
    OrganizationDto organization1 = dbTester.organizations().insert();
    GroupDto group1 = dbTester.users().insertGroup(organization1);
    dbTester.users().insertPermissionOnGroup(group1, ADMINISTER);
    dbTester.users().insertMember(group1, user);
    UserDto otherUser = dbTester.users().insertUser();
    OrganizationDto organization2 = dbTester.organizations().insert();
    GroupDto group2 = dbTester.users().insertGroup(organization2);
    dbTester.users().insertPermissionOnGroup(group2, ADMINISTER);
    dbTester.users().insertMember(group2, otherUser);

    userSessionRule.logIn(user);
    assertJson(underTest.newRequest().execute().getInput()).isSimilarTo("{\"organizations\": [" +
      "\"" + dbTester.getDefaultOrganization().getKey() + "\"," +
      "\"" + organization1.getKey() + "\"" +
      "]}");

    userSessionRule.logIn(otherUser);
    assertJson(underTest.newRequest().execute().getInput()).isSimilarTo("{\"organizations\": [" +
      "\"" + organization2.getKey() + "\"" +
      "]}");

    userSessionRule.logIn();
    assertJson(underTest.newRequest().execute().getInput()).isSimilarTo(NO_ORGANIZATIONS_RESPONSE);
  }

  @Test
  public void returns_organization_of_authenticated_user_only_for_ADMIN_permission() {
    UserDto user = dbTester.users().insertUser();
    OrganizationDto organization1 = dbTester.organizations().insert();
    OrganizationDto organization2 = dbTester.organizations().insert();
    GroupDto group = dbTester.users().insertGroup(organization2);
    dbTester.users().insertMember(group, user);
    OrganizationPermission.all()
      .filter(p -> p != ADMINISTER)
      .forEach(p -> {
        dbTester.users().insertPermissionOnUser(organization1, user, p);
        dbTester.users().insertPermissionOnGroup(group, p);
      });

    userSessionRule.logIn(user);
    assertJson(underTest.newRequest().execute().getInput()).isSimilarTo(NO_ORGANIZATIONS_RESPONSE);
  }

  @Test
  public void do_not_return_organization_twice_if_user_has_ADMIN_permission_twice_or_more() {
    UserDto user = dbTester.users().insertUser();
    OrganizationDto organization = dbTester.organizations().insert();
    GroupDto group1 = dbTester.users().insertGroup(organization);
    dbTester.users().insertPermissionOnGroup(group1, ADMINISTER);
    dbTester.users().insertPermissionOnUser(organization, user, SYSTEM_ADMIN);

    userSessionRule.logIn(user);
    assertJson(underTest.newRequest().execute().getInput()).isSimilarTo("{\"organizations\": [" +
      "\"" + organization.getKey() + "\"" +
      "]}");
  }
}
