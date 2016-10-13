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
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.GroupTesting.newGroupDto;

public class SearchActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private WsTester ws;

  @Before
  public void setUp() {
    ws = new WsTester(new UserGroupsWs(new SearchAction(db.getDbClient(), userSession, newGroupWsSupport())));
  }

  @Test
  public void search_empty() throws Exception {
    loginAsSimpleUser();
    newRequest().execute().assertJson(getClass(), "empty.json");
  }

  @Test
  public void search_without_parameters() throws Exception {
    insertGroup(db.getDefaultOrganization(), "users", 0);
    insertGroup(db.getDefaultOrganization(), "admins", 0);
    insertGroup(db.getDefaultOrganization(), "customer1", 0);
    insertGroup(db.getDefaultOrganization(), "customer2", 0);
    insertGroup(db.getDefaultOrganization(), "customer3", 0);

    loginAsSimpleUser();
    newRequest().execute().assertJson(getClass(), "five_groups.json");
  }

  @Test
  public void search_with_members() throws Exception {
    insertGroup(db.getDefaultOrganization(), "users", 5);
    insertGroup(db.getDefaultOrganization(), "admins", 1);
    insertGroup(db.getDefaultOrganization(), "customer1", 0);
    insertGroup(db.getDefaultOrganization(), "customer2", 4);
    insertGroup(db.getDefaultOrganization(), "customer3", 0);

    loginAsSimpleUser();
    newRequest().execute().assertJson(getClass(), "with_members.json");
  }

  @Test
  public void search_with_query() throws Exception {
    insertGroup(db.getDefaultOrganization(), "users", 0);
    insertGroup(db.getDefaultOrganization(), "admins", 0);
    insertGroup(db.getDefaultOrganization(), "customer%_%/1", 0);
    insertGroup(db.getDefaultOrganization(), "customer%_%/2", 0);
    insertGroup(db.getDefaultOrganization(), "customer%_%/3", 0);

    loginAsSimpleUser();
    newRequest().setParam(Param.TEXT_QUERY, "tomer%_%/").execute().assertJson(getClass(), "customers.json");
  }

  @Test
  public void search_with_paging() throws Exception {
    insertGroup(db.getDefaultOrganization(), "users", 0);
    insertGroup(db.getDefaultOrganization(), "admins", 0);
    insertGroup(db.getDefaultOrganization(), "customer1", 0);
    insertGroup(db.getDefaultOrganization(), "customer2", 0);
    insertGroup(db.getDefaultOrganization(), "customer3", 0);

    loginAsSimpleUser();
    newRequest()
      .setParam(Param.PAGE_SIZE, "3").execute().assertJson(getClass(), "page_1.json");
    newRequest()
      .setParam(Param.PAGE_SIZE, "3").setParam(Param.PAGE, "2").execute().assertJson(getClass(), "page_2.json");
    newRequest()
      .setParam(Param.PAGE_SIZE, "3").setParam(Param.PAGE, "3").execute().assertJson(getClass(), "page_3.json");
  }

  @Test
  public void search_with_fields() throws Exception {
    insertGroup(db.getDefaultOrganization(), "sonar-users", 0);

    loginAsSimpleUser();
    assertThat(newRequest().execute().outputAsString())
      .contains("id")
      .contains("name")
      .contains("description")
      .contains("membersCount");

    assertThat(newRequest().setParam(Param.FIELDS, "").execute().outputAsString())
      .contains("id")
      .contains("name")
      .contains("description")
      .contains("membersCount");

    assertThat(newRequest().setParam(Param.FIELDS, "name").execute().outputAsString())
      .contains("id")
      .contains("name")
      .doesNotContain("description")
      .doesNotContain("membersCount");

    assertThat(newRequest().setParam(Param.FIELDS, "description").execute().outputAsString())
      .contains("id")
      .doesNotContain("name")
      .contains("description")
      .doesNotContain("membersCount");

    assertThat(newRequest().setParam(Param.FIELDS, "membersCount").execute().outputAsString())
      .contains("id")
      .doesNotContain("name")
      .doesNotContain("description")
      .contains("membersCount");
  }

  @Test
  public void search_in_organization() throws Exception {
    OrganizationDto org = db.organizations().insert();
    GroupDto group = db.users().insertGroup(org, "users");
    // the group in default org is not returned
    db.users().insertGroup(db.getDefaultOrganization(), "users");

    loginAsSimpleUser();
    newRequest()
      .setParam("organization", org.getKey())
      .execute()
      .assertJson(
        "{\"total\":1,\"p\":1,\"ps\":100," +
          "\"groups\":[{\"id\":\"" + group.getId() + "\",\"name\":\"users\"}]}\n");
  }

  @Test
  public void fail_when_not_logged_in() throws Exception {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);
    newRequest().execute();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newGetRequest("api/user_groups", "search");
  }

  private void insertGroup(OrganizationDto org, String name, int numberOfMembers) {
    GroupDto group = newGroupDto().setName(name).setDescription(capitalize(name)).setOrganizationUuid(org.getUuid());
    db.users().insertGroup(group);
    for (int i = 0; i < numberOfMembers; i++) {
      UserDto user = db.users().insertUser();
      db.users().insertMember(group, user);
    }
  }

  private void loginAsSimpleUser() {
    userSession.login("user");
  }

  private GroupWsSupport newGroupWsSupport() {
    return new GroupWsSupport(db.getDbClient(), TestDefaultOrganizationProvider.from(db));
  }

}
