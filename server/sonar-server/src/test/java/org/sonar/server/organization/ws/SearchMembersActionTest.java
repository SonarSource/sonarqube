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

import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.ws.AvatarResolverImpl;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common.Paging;
import org.sonarqube.ws.Organizations.SearchMembersWsResponse;
import org.sonarqube.ws.Organizations.User;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.server.ws.WebService.SelectionMode.DESELECTED;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchMembersActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public EsTester es = EsTester.create();

  @Rule
  public DbTester db = DbTester.create();
  private DbClient dbClient = db.getDbClient();

  private DefaultOrganizationProvider organizationProvider = TestDefaultOrganizationProvider.from(db);
  private UserIndexer indexer = new UserIndexer(dbClient, es.client());

  private WsActionTester ws = new WsActionTester(
    new SearchMembersAction(dbClient, new UserIndex(es.client(), System2.INSTANCE), organizationProvider, userSession, new AvatarResolverImpl()));

  @Test
  public void empty_response() {
    OrganizationDto organization = db.organizations().insert();
    logAsOrganizationMember(organization);

    SearchMembersWsResponse result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .executeProtobuf(SearchMembersWsResponse.class);

    assertThat(result.getUsersList()).isEmpty();
    assertThat(result.getPaging())
      .extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal)
      .containsExactly(1, 50, 0);
  }

  @Test
  public void search_members_of_default_organization() {
    OrganizationDto defaultOrganization = db.getDefaultOrganization();
    logAsOrganizationMember(defaultOrganization);
    UserDto user = db.users().insertUser();
    db.organizations().addMember(defaultOrganization, user);
    UserDto anotherUser = db.users().insertUser();
    db.organizations().addMember(defaultOrganization, anotherUser);
    indexAllUsers();

    SearchMembersWsResponse result = ws.newRequest()
      .setParam("organization", defaultOrganization.getKey())
      .executeProtobuf(SearchMembersWsResponse.class);

    assertThat(result.getUsersList())
      .extracting(User::getLogin, User::getName)
      .containsOnly(
        tuple(user.getLogin(), user.getName()),
        tuple(anotherUser.getLogin(), anotherUser.getName()));
  }

  @Test
  public void search_members_of_specified_organization() {
    OrganizationDto organization = db.organizations().insert();
    logAsOrganizationMember(organization);
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();
    UserDto userInAnotherOrganization = db.users().insertUser();
    db.organizations().addMember(organization, user, anotherUser);
    db.organizations().addMember(anotherOrganization, userInAnotherOrganization);
    indexAllUsers();

    SearchMembersWsResponse result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .executeProtobuf(SearchMembersWsResponse.class);

    assertThat(result.getUsersList())
      .extracting(User::getLogin, User::getName)
      .containsOnly(
        tuple(user.getLogin(), user.getName()),
        tuple(anotherUser.getLogin(), anotherUser.getName()));
  }

  @Test
  public void return_avatar() {
    OrganizationDto organization = db.organizations().insert();
    logAsOrganizationMember(organization);
    UserDto user = db.users().insertUser(u -> u.setEmail("email@domain.com"));
    db.organizations().addMember(organization, user);
    indexer.commitAndIndex(db.getSession(), user);

    SearchMembersWsResponse result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .executeProtobuf(SearchMembersWsResponse.class);

    assertThat(result.getUsers(0).getAvatar()).isEqualTo("7328fddefd53de471baeb6e2b764f78a");
  }

  @Test
  public void do_not_return_group_count_if_no_admin_permission() {
    OrganizationDto organization = db.organizations().insert();
    logAsOrganizationMember(organization);
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);
    indexAllUsers();

    SearchMembersWsResponse result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .executeProtobuf(SearchMembersWsResponse.class);

    assertThat(result.getUsers(0).hasGroupCount()).isFalse();
  }

  @Test
  public void return_group_counts_if_org_admin() {
    OrganizationDto organization = db.organizations().insert();
    UserDto currentUser = db.users().insertUser();
    userSession.logIn(currentUser)
      .addPermission(OrganizationPermission.ADMINISTER, organization)
      .addMembership(organization);
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    UserDto anotherUser = db.users().insertUser();
    db.organizations().addMember(organization, anotherUser);
    IntStream.range(0, 10)
      .mapToObj(i -> db.users().insertGroup(organization))
      .forEach(g -> db.users().insertMembers(g, user));
    OrganizationDto anotherOrganization = db.organizations().insert();
    GroupDto anotherGroup = db.users().insertGroup(anotherOrganization);
    db.users().insertMember(anotherGroup, user);
    indexAllUsers();

    SearchMembersWsResponse result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .executeProtobuf(SearchMembersWsResponse.class);

    assertThat(result.getUsersList()).extracting(User::getLogin, User::getGroupCount, User::hasGroupCount)
      .containsExactlyInAnyOrder(
        tuple(user.getLogin(), 10, true),
        tuple(anotherUser.getLogin(), 0, true));
  }

  @Test
  public void search_non_members() {
    OrganizationDto organization = db.organizations().insert();
    UserDto currentUser = db.users().insertUser();
    userSession.logIn(currentUser).addMembership(organization);
    UserDto anotherUser = db.users().insertUser();
    db.organizations().addMember(organization, currentUser, anotherUser);

    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto userInNoOrganization = db.users().insertUser();
    UserDto userInAnotherOrganization = db.users().insertUser();
    db.organizations().addMember(anotherOrganization, userInAnotherOrganization);
    indexAllUsers();

    SearchMembersWsResponse result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("selected", DESELECTED.value())
      .executeProtobuf(SearchMembersWsResponse.class);

    assertThat(result.getUsersList())
      .extracting(User::getLogin, User::getName)
      .containsOnly(
        tuple(userInNoOrganization.getLogin(), userInNoOrganization.getName()),
        tuple(userInAnotherOrganization.getLogin(), userInAnotherOrganization.getName()));
  }

  @Test
  public void search_members_pagination() {
    OrganizationDto organization = db.organizations().insert();
    logAsOrganizationMember(organization);
    IntStream.range(0, 10).forEach(i -> {
      UserDto userDto = db.users().insertUser(user -> user.setName("USER_" + i));
      db.organizations().addMember(organization, userDto);
    });
    indexAllUsers();

    SearchMembersWsResponse result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("p", "2")
      .setParam("ps", "3")
      .executeProtobuf(SearchMembersWsResponse.class);

    assertThat(result.getUsersList()).extracting(User::getName)
      .containsExactly("USER_3", "USER_4", "USER_5");
    assertThat(result.getPaging())
      .extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal)
      .containsExactly(2, 3, 10);
  }

  @Test
  public void search_members_by_name() {
    OrganizationDto organization = db.organizations().insert();
    logAsOrganizationMember(organization);
    IntStream.range(0, 10).forEach(i -> {
      UserDto userDto = db.users().insertUser(user -> user.setName("USER_" + i));
      db.organizations().addMember(organization, userDto);
    });
    indexAllUsers();

    SearchMembersWsResponse result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("q", "_9")
      .executeProtobuf(SearchMembersWsResponse.class);

    assertThat(result.getUsersList()).extracting(User::getName).containsExactly("USER_9");
  }

  @Test
  public void search_members_by_login() {
    OrganizationDto organization = db.organizations().insert();
    logAsOrganizationMember(organization);
    IntStream.range(0, 10).forEach(i -> {
      UserDto userDto = db.users().insertUser(user -> user.setLogin("USER_" + i));
      db.organizations().addMember(organization, userDto);
    });
    indexAllUsers();

    SearchMembersWsResponse result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("q", "_9")
      .executeProtobuf(SearchMembersWsResponse.class);

    assertThat(result.getUsersList()).extracting(User::getLogin).containsExactly("USER_9");
  }

  @Test
  public void search_members_by_email() {
    OrganizationDto organization = db.organizations().insert();
    logAsOrganizationMember(organization);
    IntStream.range(0, 10).forEach(i -> {
      UserDto userDto = db.users().insertUser(user -> user
        .setLogin("L" + i)
        .setEmail("USER_" + i + "@email.com"));
      db.organizations().addMember(organization, userDto);
    });
    indexAllUsers();

    SearchMembersWsResponse result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("q", "_9")
      .executeProtobuf(SearchMembersWsResponse.class);

    assertThat(result.getUsersList()).extracting(User::getLogin).containsExactly("L9");
  }

  @Test
  public void fail_if_organization_is_unknown() {
    OrganizationDto organization = db.organizations().insert();
    logAsOrganizationMember(organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'unknown'");

    ws.newRequest()
      .setParam("organization", "unknown")
      .execute();
  }

  @Test
  public void fail_when_not_member_of_organization() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage(format("You're not member of organization '%s'", organization.getKey()));

    ws.newRequest()
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void json_example() {
    OrganizationDto organization = db.organizations().insert();
    logAsOrganizationMember(organization);
    UserDto user1 = db.users().insertUser(u -> u.setLogin("ada.lovelace").setName("Ada Lovelace").setEmail("ada@lovelace.com"));
    db.organizations().addMember(organization, user1);
    UserDto user2 = db.users().insertUser(u -> u.setLogin("grace.hopper").setName("Grace Hopper").setEmail("grace@hopper.com"));
    db.organizations().addMember(organization, user2);
    indexAllUsers();

    String result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();

    assertThat(action.key()).isEqualTo("search_members");
    assertThat(action.params()).extracting(Param::key).containsOnly("q", "selected", "p", "ps", "organization");
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.since()).isEqualTo("6.4");
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.param("organization").isInternal()).isTrue();

    Param selected = action.param("selected");
    assertThat(selected.possibleValues()).containsOnly("selected", "deselected");
    assertThat(selected.isInternal()).isTrue();
    assertThat(selected.defaultValue()).isEqualTo("selected");

    assertThat(action.param("ps").maximumValue()).isEqualTo(500);
    assertThat(action.param("q").minimumLength()).isEqualTo(2);
  }

  private void indexAllUsers() {
    indexer.indexOnStartup(indexer.getIndexTypes());
  }

  private void logAsOrganizationMember(OrganizationDto organization) {
    userSession.logIn(db.users().insertUser()).addMembership(organization);
  }

}
