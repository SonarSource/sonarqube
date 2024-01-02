/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common.Paging;
import org.sonarqube.ws.MediaTypes;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.server.ws.WebService.Param.FIELDS;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.UserGroups.Group;
import static org.sonarqube.ws.UserGroups.SearchWsResponse;

public class SearchActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();


  private final WsActionTester ws = new WsActionTester(new SearchAction(db.getDbClient(), userSession,
    new DefaultGroupFinder(db.getDbClient())));

  @Test
  public void define_search_action() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.key()).isEqualTo("search");
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(4);
    assertThat(action.changelog()).extracting(Change::getVersion, Change::getDescription).containsOnly(
      tuple("8.4", "Field 'id' in the response is deprecated. Format changes from integer to string."),
      tuple("6.4", "Paging response fields moved to a Paging object"),
      tuple("6.4", "'default' response field has been added"));
  }

  @Test
  public void search_without_parameters() {
    insertDefaultGroup(0);
    insertGroup("admins", 0);
    insertGroup("customer1", 0);
    insertGroup("customer2", 0);
    insertGroup("customer3", 0);
    loginAsAdmin();

    SearchWsResponse response = call(ws.newRequest());

    assertThat(response.getGroupsList()).extracting(Group::getName, Group::getDescription, Group::getMembersCount).containsOnly(
      tuple("admins", "Admins", 0),
      tuple("customer1", "Customer1", 0),
      tuple("customer2", "Customer2", 0),
      tuple("customer3", "Customer3", 0),
      tuple("sonar-users", "Users", 0));
  }

  @Test
  public void search_with_members() {
    insertDefaultGroup(5);
    insertGroup("admins", 1);
    insertGroup("customer1", 0);
    insertGroup("customer2", 4);
    insertGroup("customer3", 0);
    loginAsAdmin();

    SearchWsResponse response = call(ws.newRequest());

    assertThat(response.getGroupsList()).extracting(Group::getName, Group::getDescription, Group::getMembersCount).containsOnly(
      tuple("admins", "Admins", 1),
      tuple("customer1", "Customer1", 0),
      tuple("customer2", "Customer2", 4),
      tuple("customer3", "Customer3", 0),
      tuple("sonar-users", "Users", 5));
  }

  @Test
  public void search_with_query() {
    insertDefaultGroup(0);
    insertGroup("admins", 0);
    insertGroup("customer%_%/1", 0);
    insertGroup("customer%_%/2", 0);
    insertGroup("customer%_%/3", 0);
    loginAsAdmin();

    SearchWsResponse response = call(ws.newRequest().setParam(TEXT_QUERY, "tomer%_%/"));

    assertThat(response.getGroupsList()).extracting(Group::getName, Group::getDescription, Group::getMembersCount).containsOnly(
      tuple("customer%_%/1", "Customer%_%/1", 0),
      tuple("customer%_%/2", "Customer%_%/2", 0),
      tuple("customer%_%/3", "Customer%_%/3", 0));
  }

  @Test
  public void search_with_paging() {
    insertDefaultGroup(0);
    insertGroup("admins", 0);
    insertGroup("customer1", 0);
    insertGroup("customer2", 0);
    insertGroup("customer3", 0);
    loginAsAdmin();

    SearchWsResponse response = call(ws.newRequest().setParam(PAGE_SIZE, "3"));
    assertThat(response.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal).containsOnly(1, 3, 5);
    assertThat(response.getGroupsList()).extracting(Group::getName, Group::getDescription, Group::getMembersCount).containsOnly(
      tuple("admins", "Admins", 0),
      tuple("customer1", "Customer1", 0),
      tuple("customer2", "Customer2", 0));

    response = call(ws.newRequest().setParam(PAGE_SIZE, "3").setParam(PAGE, "2"));
    assertThat(response.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal).containsOnly(2, 3, 5);
    assertThat(response.getGroupsList()).extracting(Group::getName, Group::getDescription, Group::getMembersCount).containsOnly(
      tuple("customer3", "Customer3", 0),
      tuple("sonar-users", "Users", 0));

    response = call(ws.newRequest().setParam(PAGE_SIZE, "3").setParam(PAGE, "3"));
    assertThat(response.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal).containsOnly(3, 3, 5);
    assertThat(response.getGroupsList()).isEmpty();
  }

  @Test
  public void search_with_fields() {
    insertDefaultGroup(0);
    loginAsAdmin();

    assertThat(call(ws.newRequest()).getGroupsList()).extracting(Group::hasId, Group::hasName, Group::hasDescription, Group::hasMembersCount)
      .containsOnly(tuple(true, true, true, true));
    assertThat(call(ws.newRequest().setParam(FIELDS, "")).getGroupsList()).extracting(Group::hasId, Group::hasName, Group::hasDescription, Group::hasMembersCount)
      .containsOnly(tuple(true, true, true, true));
    assertThat(call(ws.newRequest().setParam(FIELDS, "name")).getGroupsList()).extracting(Group::hasId, Group::hasName, Group::hasDescription, Group::hasMembersCount)
      .containsOnly(tuple(true, true, false, false));
    assertThat(call(ws.newRequest().setParam(FIELDS, "description")).getGroupsList()).extracting(Group::hasId, Group::hasName, Group::hasDescription, Group::hasMembersCount)
      .containsOnly(tuple(true, false, true, false));
    assertThat(call(ws.newRequest().setParam(FIELDS, "membersCount")).getGroupsList()).extracting(Group::hasId, Group::hasName, Group::hasDescription, Group::hasMembersCount)
      .containsOnly(tuple(true, false, false, true));
  }

  @Test
  public void return_default_group() {
    db.users().insertDefaultGroup();
    loginAsAdmin();

    SearchWsResponse response = call(ws.newRequest());

    assertThat(response.getGroupsList()).extracting(Group::getName, Group::getDefault).containsOnly(tuple("sonar-users", true));
  }

  @Test
  public void fail_when_not_logged_in() {
    userSession.anonymous();

    assertThatThrownBy(() -> {
      call(ws.newRequest());
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void test_json_example() {
    insertDefaultGroup(17);
    insertGroup("administrators", 2);
    loginAsAdmin();

    String response = ws.newRequest().setMediaType(MediaTypes.JSON).execute().getInput();

    assertJson(response).ignoreFields("id").isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();

    assertThat(action.since()).isEqualTo("5.2");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();

    assertThat(action.params()).extracting(WebService.Param::key).containsOnly("p", "q", "ps", "f");

    assertThat(action.param("f").possibleValues()).containsOnly("name", "description", "membersCount");
  }

  private SearchWsResponse call(TestRequest request) {
    return request.executeProtobuf(SearchWsResponse.class);
  }

  private void insertDefaultGroup(int numberOfMembers) {
    GroupDto group = db.users().insertDefaultGroup();
    addMembers(group, numberOfMembers);
  }

  private void insertGroup(String name, int numberOfMembers) {
    GroupDto group = newGroupDto().setName(name).setDescription(capitalize(name));
    db.users().insertGroup(group);
    addMembers(group, numberOfMembers);
  }

  private void addMembers(GroupDto group, int numberOfMembers) {
    for (int i = 0; i < numberOfMembers; i++) {
      UserDto user = db.users().insertUser();
      db.users().insertMember(group, user);
    }
  }

  private void loginAsAdmin() {
    userSession.logIn("user").addPermission(ADMINISTER);
  }
}
