/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.user.ws;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.scim.ScimUserDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.IdentityProviderRepository;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.common.user.UserDeactivator;
import org.sonar.server.common.user.service.UserService;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common.Paging;
import org.sonarqube.ws.Users.SearchWsResponse;
import org.sonarqube.ws.Users.SearchWsResponse.User;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.user.ws.SearchAction.EXTERNAL_IDENTITY;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create();

  private final ManagedInstanceService managedInstanceService = mock(ManagedInstanceService.class);

  private final UserService userService = new UserService(
    db.getDbClient(),
    new AvatarResolverImpl(),
    managedInstanceService,
    mock(ManagedInstanceChecker.class),
    mock(UserDeactivator.class),
    mock(UserUpdater.class),
    mock(IdentityProviderRepository.class));

  private final SearchWsReponseGenerator searchWsReponseGenerator = new SearchWsReponseGenerator(userSession);

  private final WsActionTester ws = new WsActionTester(new SearchAction(userSession, userService, searchWsReponseGenerator));

  @Test
  public void search_for_all_active_users() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser(u -> u.setActive(false));

    userSession.logIn();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::getName)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), user1.getName()),
        tuple(user2.getLogin(), user2.getName()));
  }

  @Test
  public void search_deactivated_users() {
    UserDto user1 = db.users().insertUser(u -> u.setActive(false));
    UserDto user2 = db.users().insertUser(u -> u.setActive(true));
    userSession.logIn();

    SearchWsResponse response = ws.newRequest()
      .setParam("deactivated", "true")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::getName)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), user1.getName()));
  }

  @Test
  public void search_with_query() {
    userSession.logIn();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("user-%_%-login")
      .setName("user-name")
      .setEmail("user@mail.com")
      .setLocal(true)
      .setScmAccounts(singletonList("user1")));

    assertThat(ws.newRequest()
      .setParam("q", "user-%_%-")
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin)
        .containsExactlyInAnyOrder(user.getLogin());
    assertThat(ws.newRequest()
      .setParam("q", "user@MAIL.com")
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin)
        .containsExactlyInAnyOrder(user.getLogin());
    assertThat(ws.newRequest()
      .setParam("q", "user-name")
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin)
        .containsExactlyInAnyOrder(user.getLogin());
  }

  @Test
  public void return_avatar() {
    UserDto user = db.users().insertUser(u -> u.setEmail("john@doe.com"));
    userSession.logIn();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::getAvatar)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), "6a6c19fea4a3676970167ce51f39e6ee"));
  }

  @Test
  public void return_isManagedFlag() {
    UserDto nonManagedUser = db.users().insertUser(u -> u.setEmail("john@doe.com"));
    UserDto managedUser = db.users().insertUser(u -> u.setEmail("externalUser@doe.com"));
    mockUsersAsManaged(managedUser.getUuid());
    userSession.logIn().setSystemAdministrator();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::getManaged)
      .containsExactlyInAnyOrder(
        tuple(managedUser.getLogin(), true),
        tuple(nonManagedUser.getLogin(), false));
  }

  @Test
  public void search_whenFilteringByManagedAndInstanceNotManaged_throws() {
    userSession.logIn().setSystemAdministrator();

    TestRequest testRequest = ws.newRequest()
      .setParam("managed", "true");

    assertThatExceptionOfType(BadRequestException.class)
      .isThrownBy(() -> testRequest.executeProtobuf(SearchWsResponse.class))
      .withMessage("The 'managed' parameter is only available for managed instances.");
  }

  @Test
  public void search_whenFilteringByManagedAndInstanceManaged_returnsCorrectResults() {
    UserDto nonManagedUser = db.users().insertUser(u -> u.setEmail("john@doe.com"));
    UserDto managedUser = db.users().insertUser(u -> u.setEmail("externalUser@doe.com"));
    db.users().enableScimForUser(managedUser);
    mockUsersAsManaged(managedUser.getUuid());
    mockInstanceExternallyManagedAndFilterForManagedUsers();
    userSession.logIn().setSystemAdministrator();

    SearchWsResponse response = ws.newRequest()
      .setParam("managed", "true")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::getManaged)
      .containsExactlyInAnyOrder(
        tuple(managedUser.getLogin(), true));
  }

  @Test
  public void search_whenFilteringByNonManagedAndInstanceManaged_returnsCorrectResults() {
    UserDto nonManagedUser = db.users().insertUser(u -> u.setEmail("john@doe.com"));
    UserDto managedUser = db.users().insertUser(u -> u.setEmail("externalUser@doe.com"));
    db.users().enableScimForUser(managedUser);
    mockUsersAsManaged(managedUser.getUuid());
    mockInstanceExternallyManagedAndFilterForManagedUsers();
    userSession.logIn().setSystemAdministrator();

    SearchWsResponse response = ws.newRequest()
      .setParam("managed", "false")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::getManaged)
      .containsExactlyInAnyOrder(
        tuple(nonManagedUser.getLogin(), false));
  }

  private void mockInstanceExternallyManagedAndFilterForManagedUsers() {
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);
    when(managedInstanceService.getManagedUsersSqlFilter(anyBoolean()))
      .thenAnswer(invocation -> {
        Boolean managed = invocation.getArgument(0, Boolean.class);
        return new ScimUserDao(mock(UuidFactory.class)).getManagedUserSqlFilter(managed);
      });
  }

  @Test
  public void return_scm_accounts() {
    UserDto user = db.users().insertUser(u -> u.setScmAccounts(asList("john1", "john2")));

    userSession.logIn();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, u -> u.getScmAccounts().getScmAccountsList())
      .containsExactlyInAnyOrder(tuple(user.getLogin(), asList("john1", "john2")));
  }

  @Test
  public void return_tokens_count_when_system_administer() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user);
    db.users().insertToken(user);

    userSession.logIn().setSystemAdministrator();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::getTokensCount)
        .containsExactlyInAnyOrder(tuple(user.getLogin(), 2));

    userSession.logIn();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::hasTokensCount)
        .containsExactlyInAnyOrder(tuple(user.getLogin(), false));
  }

  @Test
  public void return_email_only_when_system_administer() {
    UserDto user = db.users().insertUser();

    userSession.logIn().setSystemAdministrator();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::getEmail)
        .containsExactlyInAnyOrder(tuple(user.getLogin(), user.getEmail()));

    userSession.logIn();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::hasEmail)
        .containsExactlyInAnyOrder(tuple(user.getLogin(), false));
  }

  @Test
  public void return_user_not_having_email() {
    UserDto user = db.users().insertUser(u -> u.setEmail(null));
    userSession.logIn().setSystemAdministrator();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::hasEmail)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), false));
  }

  @Test
  public void return_groups_only_when_system_administer() {
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup("group1");
    GroupDto group2 = db.users().insertGroup("group2");
    GroupDto group3 = db.users().insertGroup("group3");
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);

    userSession.logIn().setSystemAdministrator();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, u -> u.getGroups().getGroupsList())
        .containsExactlyInAnyOrder(tuple(user.getLogin(), asList(group1.getName(), group2.getName())));

    userSession.logIn();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::hasGroups)
        .containsExactlyInAnyOrder(tuple(user.getLogin(), false));
  }

  @Test
  public void return_external_information() {
    UserDto user = db.users().insertUser();
    userSession.logIn().setSystemAdministrator();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::getExternalIdentity, User::getExternalProvider)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), user.getExternalLogin(), user.getExternalIdentityProvider()));
  }

  @Test
  public void return_external_identity_only_when_system_administer() {
    UserDto user = db.users().insertUser();

    userSession.logIn().setSystemAdministrator();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::getExternalIdentity)
        .containsExactlyInAnyOrder(tuple(user.getLogin(), user.getExternalLogin()));

    userSession.logIn();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::hasExternalIdentity)
        .containsExactlyInAnyOrder(tuple(user.getLogin(), false));
  }

  @Test
  public void only_return_login_and_name_when_not_logged() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user);
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);
    userSession.anonymous();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::getName, User::hasTokensCount, User::hasScmAccounts, User::hasAvatar, User::hasGroups, User::hasManaged)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), user.getName(), false, false, false, false, false));
  }

  @Test
  public void return_last_connection_date_when_system_administer() {
    UserDto userWithLastConnectionDate = db.users().insertUser();
    db.users().updateLastConnectionDate(userWithLastConnectionDate, 10_000_000_000L);
    UserDto userWithoutLastConnectionDate = db.users().insertUser();
    userSession.logIn().setSystemAdministrator();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::hasLastConnectionDate, User::getLastConnectionDate)
      .containsExactlyInAnyOrder(
        tuple(userWithLastConnectionDate.getLogin(), true, formatDateTime(10_000_000_000L)),
        tuple(userWithoutLastConnectionDate.getLogin(), false, ""));
  }

  @Test
  public void return_all_fields_for_logged_user() {
    UserDto user = db.users().insertUser();
    db.users().updateLastConnectionDate(user, 10_000_000_000L);
    db.users().insertToken(user);
    db.users().insertToken(user);
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);
    UserDto otherUser = db.users().insertUser();

    userSession.logIn(user);
    assertThat(ws.newRequest().setParam("q", user.getLogin())
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::getName, User::getEmail, User::getExternalIdentity, User::getExternalProvider,
          User::hasScmAccounts, User::hasAvatar, User::hasGroups, User::getTokensCount, User::hasLastConnectionDate, User::hasManaged)
        .containsExactlyInAnyOrder(
          tuple(user.getLogin(), user.getName(), user.getEmail(), user.getExternalLogin(), user.getExternalIdentityProvider(), true, true, true, 2, true, true));

    userSession.logIn(otherUser);
    assertThat(ws.newRequest().setParam("q", user.getLogin())
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::getName, User::hasEmail, User::hasExternalIdentity, User::hasExternalProvider,
          User::hasScmAccounts, User::hasAvatar, User::hasGroups, User::hasTokensCount, User::hasLastConnectionDate)
        .containsExactlyInAnyOrder(
          tuple(user.getLogin(), user.getName(), false, false, true, true, true, false, false, false));
  }

  @Test
  public void search_whenNoPagingInformationProvided_setsDefaultValues() {
    userSession.logIn();
    IntStream.rangeClosed(0, 9).forEach(i -> db.users().insertUser(u -> u.setLogin("user-" + i).setName("User " + i)));

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getPaging().getTotal()).isEqualTo(10);
    assertThat(response.getPaging().getPageIndex()).isEqualTo(1);
    assertThat(response.getPaging().getPageSize()).isEqualTo(50);
  }

  @Test
  public void search_with_paging() {
    userSession.logIn();
    IntStream.rangeClosed(0, 9).forEach(i -> db.users().insertUser(u -> u.setLogin("user-" + i).setName("User " + i)));

    SearchWsResponse response = ws.newRequest()
      .setParam(Param.PAGE_SIZE, "5")
      .executeProtobuf(SearchWsResponse.class);
    assertThat(response.getUsersList())
      .extracting(User::getLogin)
      .containsExactly("user-0", "user-1", "user-2", "user-3", "user-4");
    assertThat(response.getPaging())
      .extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal)
      .containsExactly(1, 5, 10);

    response = ws.newRequest()
      .setParam(Param.PAGE_SIZE, "5")
      .setParam(Param.PAGE, "2")
      .executeProtobuf(SearchWsResponse.class);
    assertThat(response.getUsersList())
      .extracting(User::getLogin)
      .containsExactly("user-5", "user-6", "user-7", "user-8", "user-9");
    assertThat(response.getPaging())
      .extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal)
      .containsExactly(2, 5, 10);
  }

  @Test
  public void return_empty_result_when_no_user() {
    userSession.logIn();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList()).isEmpty();
    assertThat(response.getPaging().getTotal()).isZero();
  }

  @Test
  public void test_json_example() {
    UserDto fmallet = db.users().insertUser(u -> u.setLogin("fmallet").setName("Freddy Mallet").setEmail("f@m.com")
      .setLocal(true)
      .setScmAccounts(emptyList())
      .setExternalLogin("fmallet")
      .setExternalIdentityProvider("sonarqube"));
    long lastConnection = DateUtils.parseOffsetDateTime("2019-03-27T09:51:50+0100").toInstant().toEpochMilli();
    fmallet = db.users().updateLastConnectionDate(fmallet, lastConnection);
    fmallet = db.users().updateSonarLintLastConnectionDate(fmallet, lastConnection);
    UserDto simon = db.users().insertUser(u -> u.setLogin("sbrandhof").setName("Simon").setEmail("s.brandhof@company.tld")
      .setLocal(false)
      .setExternalLogin("sbrandhof@ldap.com")
      .setExternalIdentityProvider("sonarqube")
      .setScmAccounts(asList("simon.brandhof", "s.brandhof@company.tld")));

    mockUsersAsManaged(simon.getUuid());

    GroupDto sonarUsers = db.users().insertGroup("sonar-users");
    GroupDto sonarAdministrators = db.users().insertGroup("sonar-administrators");
    db.users().insertMember(sonarUsers, simon);
    db.users().insertMember(sonarUsers, fmallet);
    db.users().insertMember(sonarAdministrators, fmallet);
    db.users().insertToken(simon);
    db.users().insertToken(simon);
    db.users().insertToken(simon);
    db.users().insertToken(fmallet);
    userSession.logIn().setSystemAdministrator();

    String response = ws.newRequest().execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("search-example.json"));
  }

  @Test
  public void test_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(10);
  }

  @Test
  public void search_whenFilteringConnectionDate_shouldApplyFilter() {
    userSession.logIn().setSystemAdministrator();
    final Instant lastConnection = Instant.now();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("user-%_%-login")
      .setName("user-name")
      .setEmail("user@mail.com")
      .setLocal(true)
      .setScmAccounts(singletonList("user1")));
    user = db.users().updateLastConnectionDate(user, lastConnection.toEpochMilli());
    user = db.users().updateSonarLintLastConnectionDate(user, lastConnection.toEpochMilli());

    assertThat(ws.newRequest()
      .setParam("q", "user-%_%-")
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin)
        .containsExactlyInAnyOrder(user.getLogin());

    assertUserWithFilter(SearchAction.LAST_CONNECTION_DATE_FROM, lastConnection.minus(1, ChronoUnit.DAYS), user.getLogin(), true);
    assertUserWithFilter(SearchAction.LAST_CONNECTION_DATE_FROM, lastConnection.plus(1, ChronoUnit.DAYS), user.getLogin(), false);
    assertUserWithFilter(SearchAction.LAST_CONNECTION_DATE_TO, lastConnection.minus(1, ChronoUnit.DAYS), user.getLogin(), false);
    assertUserWithFilter(SearchAction.LAST_CONNECTION_DATE_TO, lastConnection.plus(1, ChronoUnit.DAYS), user.getLogin(), true);

    assertUserWithFilter(SearchAction.SONAR_LINT_LAST_CONNECTION_DATE_FROM, lastConnection.minus(1, ChronoUnit.DAYS), user.getLogin(), true);
    assertUserWithFilter(SearchAction.SONAR_LINT_LAST_CONNECTION_DATE_FROM, lastConnection.plus(1, ChronoUnit.DAYS), user.getLogin(), false);
    assertUserWithFilter(SearchAction.SONAR_LINT_LAST_CONNECTION_DATE_TO, lastConnection.minus(1, ChronoUnit.DAYS), user.getLogin(), false);
    assertUserWithFilter(SearchAction.SONAR_LINT_LAST_CONNECTION_DATE_TO, lastConnection.plus(1, ChronoUnit.DAYS), user.getLogin(), true);

    assertUserWithFilter(SearchAction.SONAR_LINT_LAST_CONNECTION_DATE_FROM, lastConnection, user.getLogin(), true);
    assertUserWithFilter(SearchAction.SONAR_LINT_LAST_CONNECTION_DATE_TO, lastConnection, user.getLogin(), true);
  }

  @Test
  public void search_whenNoLastConnection_shouldReturnForBeforeOnly() {
    userSession.logIn().setSystemAdministrator();
    final Instant lastConnection = Instant.now();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("user-%_%-login")
      .setName("user-name")
      .setEmail("user@mail.com")
      .setLocal(true)
      .setScmAccounts(singletonList("user1")));

    assertUserWithFilter(SearchAction.LAST_CONNECTION_DATE_FROM, lastConnection, user.getLogin(), false);
    assertUserWithFilter(SearchAction.LAST_CONNECTION_DATE_TO, lastConnection, user.getLogin(), true);

    assertUserWithFilter(SearchAction.SONAR_LINT_LAST_CONNECTION_DATE_FROM, lastConnection, user.getLogin(), false);
    assertUserWithFilter(SearchAction.SONAR_LINT_LAST_CONNECTION_DATE_TO, lastConnection, user.getLogin(), true);
  }

  @Test
  public void search_whenNotAdmin_shouldThrowForbidden() {
    userSession.logIn();

    Stream.of(SearchAction.LAST_CONNECTION_DATE_FROM, SearchAction.LAST_CONNECTION_DATE_TO,
      SearchAction.SONAR_LINT_LAST_CONNECTION_DATE_FROM, SearchAction.SONAR_LINT_LAST_CONNECTION_DATE_TO)
      .map(param -> ws.newRequest().setParam(param, formatDateTime(OffsetDateTime.now())))
      .forEach(SearchActionIT::assertForbiddenException);
  }

  private void assertUserWithFilter(String field, Instant filterValue, String userLogin, boolean isExpectedToBeThere) {
    var assertion = assertThat(ws.newRequest()
      .setParam("q", "user-%_%-")
      .setParam(field, DateUtils.formatDateTime(filterValue.toEpochMilli()))
      .executeProtobuf(SearchWsResponse.class).getUsersList());
    if (isExpectedToBeThere) {
      assertion
        .extracting(User::getLogin)
        .containsExactlyInAnyOrder(userLogin);
    } else {
      assertion.isEmpty();
    }
  }

  private void mockUsersAsManaged(String... userUuids) {
    when(managedInstanceService.getUserUuidToManaged(any(), any())).thenAnswer(invocation -> {
      Set<?> allUsersUuids = invocation.getArgument(1, Set.class);
      return allUsersUuids.stream()
        .map(String.class::cast)
        .collect(toMap(identity(), userUuid -> Set.of(userUuids).contains(userUuid)));
    });
  }

  @Test
  public void search_whenFilteringOnExternalIdentityAndNotAdmin_shouldThrow() {
    userSession.logIn();

    TestRequest testRequest = ws.newRequest()
      .setParam(EXTERNAL_IDENTITY, "login");

    assertForbiddenException(testRequest);
  }

  private static void assertForbiddenException(TestRequest testRequest) {
    assertThatThrownBy(() -> testRequest.executeProtobuf(SearchWsResponse.class))
      .asInstanceOf(InstanceOfAssertFactories.type(ServerException.class))
      .extracting(ServerException::httpCode)
      .isEqualTo(403);
  }

  @Test
  public void search_whenFilteringOnExternalIdentityAndMatch_shouldReturnMatchingUser() {
    userSession.logIn().setSystemAdministrator();

    prepareUsersWithExternalLogin();

    TestRequest testRequest = ws.newRequest()
      .setParam(EXTERNAL_IDENTITY, "user1");

    assertThat(testRequest.executeProtobuf(SearchWsResponse.class).getUsersList())
      .extracting(User::getExternalIdentity)
      .containsExactly("user1");
  }

  @Test
  public void search_whenFilteringOnExternalIdentityAndNoMatch_shouldReturnMatchingUser() {
    userSession.logIn().setSystemAdministrator();

    prepareUsersWithExternalLogin();

    TestRequest testRequest = ws.newRequest()
      .setParam(EXTERNAL_IDENTITY, "nomatch");

    assertThat(testRequest.executeProtobuf(SearchWsResponse.class).getUsersList())
      .extracting(User::getExternalIdentity)
      .isEmpty();
  }

  private void prepareUsersWithExternalLogin() {
    db.users().insertUser(user -> user.setExternalLogin("user1"));
    db.users().insertUser(user -> user.setExternalLogin("USER1"));
    db.users().insertUser(user -> user.setExternalLogin("user1-oldaccount"));
  }

}
