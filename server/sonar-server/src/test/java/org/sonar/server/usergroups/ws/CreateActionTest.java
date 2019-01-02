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

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.organization.DefaultOrganization;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;

public class CreateActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private WsTester ws;

  @Before
  public void setUp() {
    ws = new WsTester(new UserGroupsWs(new CreateAction(db.getDbClient(), userSession, newGroupWsSupport())));
  }

  @Test
  public void create_group_on_default_organization() throws Exception {
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute()
      .assertJson("{" +
        "  \"group\": {" +
        "    \"organization\": \"" + getDefaultOrganization().getKey() + "\"," +
        "    \"name\": \"some-product-bu\"," +
        "    \"description\": \"Business Unit for Some Awesome Product\"," +
        "    \"membersCount\": 0" +
        "  }" +
        "}");

    assertThat(db.users().selectGroup(db.getDefaultOrganization(), "some-product-bu")).isPresent();
  }

  @Test
  public void create_group_on_specific_organization() throws Exception {
    OrganizationDto org = db.organizations().insert();
    loginAsAdmin(org);

    newRequest()
      .setParam("organization", org.getKey())
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute()
      .assertJson("{" +
        "  \"group\": {" +
        "    \"organization\": \"" + org.getKey() + "\"," +
        "    \"name\": \"some-product-bu\"," +
        "    \"description\": \"Business Unit for Some Awesome Product\"," +
        "    \"membersCount\": 0" +
        "  }" +
        "}");

    GroupDto createdGroup = db.users().selectGroup(org, "some-product-bu").get();
    assertThat(createdGroup.getId()).isNotNull();
    assertThat(createdGroup.getOrganizationUuid()).isEqualTo(org.getUuid());
  }

  @Test
  public void return_default_field() throws Exception {
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute()
      .assertJson("{" +
        "  \"group\": {" +
        "    \"organization\": \"" + getDefaultOrganization().getKey() + "\"," +
        "    \"name\": \"some-product-bu\"," +
        "    \"description\": \"Business Unit for Some Awesome Product\"," +
        "    \"membersCount\": 0," +
        "    \"default\": false" +
        "  }" +
        "}");
  }

  @Test
  public void fail_if_not_administrator() throws Exception {
    userSession.logIn("not-admin");

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute();
  }

  @Test
  public void fail_if_administrator_of_another_organization() throws Exception {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    loginAsAdmin(org2);

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam("organization", org1.getKey())
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_if_name_is_too_short() throws Exception {
    loginAsAdminOnDefaultOrganization();
    newRequest()
      .setParam("name", "")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_if_name_is_too_long() throws Exception {
    loginAsAdminOnDefaultOrganization();
    newRequest()
      .setParam("name", StringUtils.repeat("a", 255 + 1))
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_if_name_is_anyone() throws Exception {
    loginAsAdminOnDefaultOrganization();
    newRequest()
      .setParam("name", "AnYoNe")
      .execute();
  }

  @Test
  public void fail_if_group_with_same_name_already_exists() throws Exception {
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(ServerException.class);
    expectedException.expectMessage("Group '" + group.getName() + "' already exists");

    newRequest()
      .setParam("name", group.getName())
      .execute();
  }

  @Test
  public void fail_if_group_with_same_name_already_exists_in_the_organization() throws Exception {
    OrganizationDto org = db.organizations().insert();
    GroupDto group = db.users().insertGroup(org, "the-group");
    loginAsAdmin(org);

    expectedException.expect(ServerException.class);
    expectedException.expectMessage("Group '" + group.getName() + "' already exists");

    newRequest()
      .setParam("organization", org.getKey())
      .setParam("name", group.getName())
      .execute();
  }

  @Test
  public void add_group_with_a_name_that_already_exists_in_another_organization() throws Exception {
    String name = "the-group";
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    GroupDto group = db.users().insertGroup(org1, name);
    loginAsAdmin(org2);

    newRequest()
      .setParam("organization", org2.getKey())
      .setParam("name", name)
      .execute()
      .assertJson("{" +
        "  \"group\": {" +
        "    \"organization\": \"" + org2.getKey() + "\"," +
        "    \"name\": \"" + group.getName() + "\"," +
        "  }" +
        "}");

    assertThat(db.users().selectGroups(org1)).extracting(GroupDto::getName).containsOnly(name);
    assertThat(db.users().selectGroups(org2)).extracting(GroupDto::getName).containsOnly(name);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_if_description_is_too_long() throws Exception {
    loginAsAdminOnDefaultOrganization();
    newRequest()
      .setParam("name", "long-desc")
      .setParam("description", StringUtils.repeat("a", 1_000))
      .execute();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest("api/user_groups", "create");
  }

  private void loginAsAdminOnDefaultOrganization() {
    loginAsAdmin(db.getDefaultOrganization());
  }

  private void loginAsAdmin(OrganizationDto org) {
    userSession.logIn().addPermission(ADMINISTER, org);
  }

  private GroupWsSupport newGroupWsSupport() {
    return new GroupWsSupport(db.getDbClient(), defaultOrganizationProvider, new DefaultGroupFinder(db.getDbClient()));
  }

  private DefaultOrganization getDefaultOrganization() {
    return defaultOrganizationProvider.get();
  }
}
