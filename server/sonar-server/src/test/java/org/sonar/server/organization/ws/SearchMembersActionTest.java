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
package org.sonar.server.organization.ws;

import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
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
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.ws.AvatarResolverImpl;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common.Paging;
import org.sonarqube.ws.Organizations.SearchMembersWsResponse;
import org.sonarqube.ws.Organizations.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchMembersActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public EsTester es = new EsTester(new UserIndexDefinition(new MapSettings().asConfig()));

  @Rule
  public DbTester db = DbTester.create();
  private DbClient dbClient = db.getDbClient();

  private DefaultOrganizationProvider organizationProvider = TestDefaultOrganizationProvider.from(db);
  private UserIndexer indexer = new UserIndexer(dbClient, es.client());

  private WsActionTester ws = new WsActionTester(new SearchMembersAction(dbClient, new UserIndex(es.client(), System2.INSTANCE), organizationProvider, userSession, new AvatarResolverImpl()));

  private SearchMembersRequest request = new SearchMembersRequest();

  @Test
  public void empty_response() {
    SearchMembersWsResponse result = call();

    assertThat(result.getUsersList()).isEmpty();
    assertThat(result.getPaging())
      .extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal)
      .containsExactly(1, 50, 0);
  }

  @Test
  public void search_members_of_default_organization() {
    UserDto user = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user);
    UserDto anotherUser = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), anotherUser);
    indexAllUsers();

    SearchMembersWsResponse result = call();

    assertThat(result.getUsersList())
      .extracting(User::getLogin, User::getName)
      .containsOnly(
        tuple(user.getLogin(), user.getName()),
        tuple(anotherUser.getLogin(), anotherUser.getName()));
  }

  @Test
  public void search_members_of_specified_organization() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();
    UserDto userInAnotherOrganization = db.users().insertUser();
    db.organizations().addMember(organization, user);
    db.organizations().addMember(organization, anotherUser);
    db.organizations().addMember(anotherOrganization, userInAnotherOrganization);
    indexAllUsers();
    request.setOrganization(organization.getKey());

    SearchMembersWsResponse result = call();

    assertThat(result.getUsersList())
      .extracting(User::getLogin, User::getName)
      .containsOnly(
        tuple(user.getLogin(), user.getName()),
        tuple(anotherUser.getLogin(), anotherUser.getName()));
  }

  @Test
  public void return_avatar() {
    UserDto user = db.users().insertUser(u -> u.setEmail("email@domain.com"));
    db.organizations().addMember(db.getDefaultOrganization(), user);
    indexer.commitAndIndex(db.getSession(), user);

    SearchMembersWsResponse result = call();

    assertThat(result.getUsers(0).getAvatar()).isEqualTo("7328fddefd53de471baeb6e2b764f78a");
  }

  @Test
  public void do_not_return_group_count_if_no_admin_permission() {
    UserDto user = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user);
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);
    indexAllUsers();

    SearchMembersWsResponse result = call();

    assertThat(result.getUsers(0).hasGroupCount()).isFalse();
  }

  @Test
  public void return_group_counts_if_org_admin() {
    userSession.addPermission(OrganizationPermission.ADMINISTER, db.getDefaultOrganization());
    UserDto user = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), user);
    UserDto anotherUser = db.users().insertUser();
    db.organizations().addMember(db.getDefaultOrganization(), anotherUser);
    IntStream.range(0, 10)
      .mapToObj(i -> db.users().insertGroup())
      .forEach(g -> db.users().insertMembers(g, user));
    OrganizationDto anotherOrganization = db.organizations().insert();
    GroupDto anotherGroup = db.users().insertGroup(anotherOrganization);
    db.users().insertMember(anotherGroup, user);
    indexAllUsers();

    SearchMembersWsResponse result = call();

    assertThat(result.getUsersList()).extracting(User::getLogin, User::getGroupCount).containsOnly(
      tuple(user.getLogin(), 10),
      tuple(anotherUser.getLogin(), 0));
  }

  @Test
  public void search_non_members() {
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();
    UserDto userInAnotherOrganization = db.users().insertUser();
    db.organizations().addMember(anotherOrganization, userInAnotherOrganization);
    indexAllUsers();
    request
      .setOrganization(anotherOrganization.getKey())
      .setSelected(WebService.SelectionMode.DESELECTED.value());

    SearchMembersWsResponse result = call();

    assertThat(result.getUsersList())
      .extracting(User::getLogin, User::getName)
      .containsOnly(
        tuple(user.getLogin(), user.getName()),
        tuple(anotherUser.getLogin(), anotherUser.getName()));
  }

  @Test
  public void search_members_pagination() {
    IntStream.range(0, 10).forEach(i -> {
      UserDto userDto = db.users().insertUser(user -> user.setName("USER_" + i));
      db.organizations().addMember(db.getDefaultOrganization(), userDto);
    });
    indexAllUsers();
    request.setPage(2).setPageSize(3);

    SearchMembersWsResponse result = call();

    assertThat(result.getUsersList()).extracting(User::getName)
      .containsExactly("USER_3", "USER_4", "USER_5");
    assertThat(result.getPaging())
      .extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal)
      .containsExactly(2, 3, 10);
  }

  @Test
  public void search_members_by_name() {
    IntStream.range(0, 10).forEach(i -> {
      UserDto userDto = db.users().insertUser(user -> user.setName("USER_" + i));
      db.organizations().addMember(db.getDefaultOrganization(), userDto);
    });
    indexAllUsers();
    request.setQuery("_9");

    SearchMembersWsResponse result = call();

    assertThat(result.getUsersList()).extracting(User::getName).containsExactly("USER_9");
  }

  @Test
  public void search_members_by_login() {
    IntStream.range(0, 10).forEach(i -> {
      UserDto userDto = db.users().insertUser(user -> user.setLogin("USER_" + i));
      db.organizations().addMember(db.getDefaultOrganization(), userDto);
    });
    indexAllUsers();
    request.setQuery("_9");

    SearchMembersWsResponse result = call();

    assertThat(result.getUsersList()).extracting(User::getLogin).containsExactly("USER_9");
  }

  @Test
  public void search_members_by_email() {
    IntStream.range(0, 10).forEach(i -> {
      UserDto userDto = db.users().insertUser(user -> user
        .setLogin("L" + i)
        .setEmail("USER_" + i + "@email.com"));
      db.organizations().addMember(db.getDefaultOrganization(), userDto);
    });
    indexAllUsers();
    request.setQuery("_9");

    SearchMembersWsResponse result = call();

    assertThat(result.getUsersList()).extracting(User::getLogin).containsExactly("L9");
  }

  @Test
  public void fail_if_organization_is_unknown() {
    request.setOrganization("ORGA 42");

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'ORGA 42'");

    call();
  }

  @Test
  public void fail_if_page_size_greater_than_500() {
    request.setPageSize(501);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'ps' value (501) must be less than 500");

    call();
  }

  @Test
  public void fail_if_query_length_lower_than_2() {
    request.setQuery("a");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Query length must be greater than or equal to 2");

    call();
  }

  @Test
  public void json_example() {
    UserDto user1 = db.users().insertUser(u -> u.setLogin("ada.lovelace").setName("Ada Lovelace").setEmail("ada@lovelace.com"));
    db.organizations().addMember(db.getDefaultOrganization(), user1);
    UserDto user2 = db.users().insertUser(u -> u.setLogin("grace.hopper").setName("Grace Hopper").setEmail("grace@hopper.com"));
    db.organizations().addMember(db.getDefaultOrganization(), user2);
    indexAllUsers();

    String result = ws.newRequest().execute().getInput();

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
  }

  private void indexAllUsers() {
    indexer.indexOnStartup(indexer.getIndexTypes());
  }

  private SearchMembersWsResponse call() {
    TestRequest wsRequest = ws.newRequest();
    setNullable(request.getOrganization(), o -> wsRequest.setParam("organization", o));
    setNullable(request.getQuery(), q -> wsRequest.setParam(Param.TEXT_QUERY, q));
    setNullable(request.getPage(), p -> wsRequest.setParam(Param.PAGE, String.valueOf(p)));
    setNullable(request.getPageSize(), ps -> wsRequest.setParam(Param.PAGE_SIZE, String.valueOf(ps)));
    setNullable(request.getSelected(), s -> wsRequest.setParam(Param.SELECTED, s));

    return wsRequest.executeProtobuf(SearchMembersWsResponse.class);
  }

  private static class SearchMembersRequest {
    private String organization;
    private String selected;
    private String query;
    private Integer page;
    private Integer pageSize;

    @CheckForNull
    public String getOrganization() {
      return organization;
    }

    public SearchMembersRequest setOrganization(@Nullable String organization) {
      this.organization = organization;
      return this;
    }

    @CheckForNull
    public String getSelected() {
      return selected;
    }

    public SearchMembersRequest setSelected(@Nullable String selected) {
      this.selected = selected;
      return this;
    }

    @CheckForNull
    public String getQuery() {
      return query;
    }

    public SearchMembersRequest setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    @CheckForNull
    public Integer getPage() {
      return page;
    }

    public SearchMembersRequest setPage(@Nullable Integer page) {
      this.page = page;
      return this;
    }

    @CheckForNull
    public Integer getPageSize() {
      return pageSize;
    }

    public SearchMembersRequest setPageSize(@Nullable Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }
  }
}
