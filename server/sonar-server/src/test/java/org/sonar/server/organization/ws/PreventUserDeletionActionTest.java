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
package org.sonar.server.organization.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Organizations;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.test.JsonAssert.assertJson;

@RunWith(DataProviderRunner.class)
public class PreventUserDeletionActionTest {

  private static final Random RANDOM = new Random();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(mock(System2.class)).setDisableDefaultOrganization(true);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final PreventUserDeletionAction underTest = new PreventUserDeletionAction(db.getDbClient(), userSession);
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void fail_if_user_is_not_logged_in() {
    UserDto user1 = db.users().insertUser();

    OrganizationDto org1 = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org1);
    db.users().insertMember(group1, user1);

    expectedException.expect(UnauthorizedException.class);

    call();
  }

  @Test
  public void returns_empty_list_when_user_is_not_admin_of_any_orgs() {
    UserDto user1 = db.users().insertUser();

    OrganizationDto org1 = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org1);
    db.users().insertMember(group1, user1);

    userSession.logIn(user1);
    assertThat(call().getOrganizationsList()).isEmpty();
  }

  @Test
  public void returns_orgs_where_user_is_last_admin() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();

    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();

    setAsDirectOrIndirectAdmin(user1, org1);
    setAsDirectOrIndirectAdmin(user2, org1);
    setAsDirectOrIndirectAdmin(user1, org2);

    userSession.logIn(user1);
    assertThat(call().getOrganizationsList())
      .extracting(Organizations.Organization::getKey)
      .containsExactly(org2.getKey());
  }

  @Test
  @UseDataProvider("adminUserCombinationsAndExpectedOrgKeys")
  public void returns_correct_orgs_for_interesting_combinations_of_last_admin_or_not(
    boolean user2IsAdminOfOrg1, boolean user1IsAdminOfOrg2, boolean user2IsAdminOfOrg2, List<String> expectedOrgKeys) {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();

    OrganizationDto org1 = db.organizations().insert(o -> o.setKey("org1"));
    OrganizationDto org2 = db.organizations().insert(o -> o.setKey("org2"));

    setAsDirectOrIndirectAdmin(user1, org1);
    if (user2IsAdminOfOrg1) {
      setAsDirectOrIndirectAdmin(user2, org1);
    }
    if (user1IsAdminOfOrg2) {
      setAsDirectOrIndirectAdmin(user1, org2);
    }
    if (user2IsAdminOfOrg2) {
      setAsDirectOrIndirectAdmin(user2, org2);
    }

    userSession.logIn(user1);
    assertThat(call().getOrganizationsList())
      .extracting(Organizations.Organization::getKey)
      .containsExactlyInAnyOrderElementsOf(expectedOrgKeys);
  }

  @DataProvider
  public static Object[][] adminUserCombinationsAndExpectedOrgKeys() {
    return new Object[][] {
      // note: user1 is always admin of org1
      // param 1: user2 is admin of org1
      // param 2: user1 is admin of org2
      // param 3: user2 is admin of org2
      // param 4: list of orgs preventing user1 to delete
      {true, true, true, emptyList()},
      {true, true, false, singletonList("org2")},
      {true, false, true, emptyList()},
      {true, false, false, emptyList()},
      {false, true, true, singletonList("org1")},
      {false, true, false, asList("org1", "org2")},
      {false, false, true, singletonList("org1")},
      {false, false, false, singletonList("org1")},
    };
  }

  @Test
  public void json_example() {
    UserDto user1 = db.users().insertUser();

    OrganizationDto org1 = db.organizations().insert(o -> {
      o.setKey("foo-company");
      o.setName("Foo Company");
    });
    OrganizationDto org2 = db.organizations().insert(o -> {
      o.setKey("bar-company");
      o.setName("Bar Company");
    });

    setAsDirectOrIndirectAdmin(user1, org1);
    setAsDirectOrIndirectAdmin(user1, org2);

    userSession.logIn(user1);

    String result = ws.newRequest()
      .execute()
      .getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();

    assertThat(action.key()).isEqualTo("prevent_user_deletion");
    assertThat(action.params()).isEmpty();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.since()).isEqualTo("7.9");
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
  }

  private void setAsDirectOrIndirectAdmin(UserDto user, OrganizationDto organization) {
    boolean useDirectAdmin = RANDOM.nextBoolean();
    if (useDirectAdmin) {
      db.users().insertPermissionOnUser(organization, user, ADMINISTER);
    } else {
      GroupDto group = db.users().insertGroup(organization);
      db.users().insertPermissionOnGroup(group, ADMINISTER);
      db.users().insertMember(group, user);
    }
  }

  private Organizations.SearchWsResponse call() {
    return ws.newRequest().executeProtobuf(Organizations.SearchWsResponse.class);
  }
}
