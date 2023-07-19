/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.common.user.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.scim.ScimUserDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.management.ManagedInstanceService;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserServiceIT {

  private static final UsersSearchRequest SEARCH_REQUEST = getBuilderWithDefaultsPageSize().build();
  @Rule
  public DbTester db = DbTester.create();

  private final ManagedInstanceService managedInstanceService = mock(ManagedInstanceService.class);

  private final UserService userService = new UserService(db.getDbClient(), new AvatarResolverImpl(), managedInstanceService);

  @Test
  public void search_for_all_active_users() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser(u -> u.setActive(false));

    SearchResults<UserSearchResult> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), r -> r.userDto().getName())
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), user1.getName()),
        tuple(user2.getLogin(), user2.getName()));
  }

  @Test
  public void search_deactivated_users() {
    UserDto user1 = db.users().insertUser(u -> u.setActive(false));
    UserDto user2 = db.users().insertUser(u -> u.setActive(true));

    SearchResults<UserSearchResult> users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(50).setDeactivated(true).build());

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), r -> r.userDto().getName())
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), user1.getName()));
  }

  @Test
  public void search_with_query() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("user-%_%-login")
      .setName("user-name")
      .setEmail("user@mail.com")
      .setLocal(true)
      .setScmAccounts(singletonList("user1")));

    SearchResults<UserSearchResult> users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(50).setQuery("user-%_%-").build());
    assertThat(users.searchResults()).extracting(UserSearchResult::userDto).extracting(UserDto::getLogin)
      .containsExactly(user.getLogin());

    users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(50).setQuery("user@MAIL.com").build());
    assertThat(users.searchResults()).extracting(UserSearchResult::userDto).extracting(UserDto::getLogin)
      .containsExactly(user.getLogin());

    users = userService.findUsers(getBuilderWithDefaultsPageSize().setQuery("user-name").build());
    assertThat(users.searchResults()).extracting(UserSearchResult::userDto).extracting(UserDto::getLogin)
      .containsExactly(user.getLogin());
  }

  @Test
  public void return_avatar() {
    UserDto user = db.users().insertUser(u -> u.setEmail("john@doe.com"));

    SearchResults<UserSearchResult> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), UserSearchResult::avatar)
      .containsExactlyInAnyOrder(
        tuple(user.getLogin(), Optional.of("6a6c19fea4a3676970167ce51f39e6ee")));

  }

  @Test
  public void return_isManagedFlag() {
    UserDto nonManagedUser = db.users().insertUser(u -> u.setEmail("john@doe.com"));
    UserDto managedUser = db.users().insertUser(u -> u.setEmail("externalUser@doe.com"));
    mockUsersAsManaged(managedUser.getUuid());

    SearchResults<UserSearchResult> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), UserSearchResult::managed)
      .containsExactlyInAnyOrder(
        tuple(managedUser.getLogin(), true),
        tuple(nonManagedUser.getLogin(), false)
      );

  }

  @Test
  public void search_whenFilteringByManagedAndInstanceManaged_returnsCorrectResults() {
    UserDto nonManagedUser = db.users().insertUser(u -> u.setEmail("john@doe.com"));
    UserDto managedUser = db.users().insertUser(u -> u.setEmail("externalUser@doe.com"));
    db.users().enableScimForUser(managedUser);
    mockUsersAsManaged(managedUser.getUuid());
    mockInstanceExternallyManagedAndFilterForManagedUsers();

    SearchResults<UserSearchResult> users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(50).setManaged(true).build());

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), UserSearchResult::managed)
      .containsExactlyInAnyOrder(
        tuple(managedUser.getLogin(), true)
      );

  }

  @Test
  public void search_whenFilteringByNonManagedAndInstanceManaged_returnsCorrectResults() {
    UserDto nonManagedUser = db.users().insertUser(u -> u.setEmail("john@doe.com"));
    UserDto managedUser = db.users().insertUser(u -> u.setEmail("externalUser@doe.com"));
    db.users().enableScimForUser(managedUser);
    mockUsersAsManaged(managedUser.getUuid());
    mockInstanceExternallyManagedAndFilterForManagedUsers();

    SearchResults<UserSearchResult> users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(50).setManaged(false).build());

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), UserSearchResult::managed)
      .containsExactlyInAnyOrder(
        tuple(nonManagedUser.getLogin(), false)
      );
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

    SearchResults<UserSearchResult> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), userSearchResult -> userSearchResult.userDto().getSortedScmAccounts())
      .containsExactlyInAnyOrder(tuple(user.getLogin(), asList("john1", "john2")));
  }

  @Test
  public void return_tokens_count_when_system_administer() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user);
    db.users().insertToken(user);

    SearchResults<UserSearchResult> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), UserSearchResult::tokensCount)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), 2));
  }

  @Test
  public void return_user_not_having_email() {
    UserDto user = db.users().insertUser(u -> u.setEmail(null));

    SearchResults<UserSearchResult> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), userSearchResult -> userSearchResult.userDto().getEmail())
      .containsExactlyInAnyOrder(tuple(user.getLogin(), null));
  }

  @Test
  public void return_groups() {
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup("group1");
    GroupDto group2 = db.users().insertGroup("group2");
    GroupDto group3 = db.users().insertGroup("group3");
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);

    SearchResults<UserSearchResult> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), UserSearchResult::groups)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), asList(group1.getName(), group2.getName())));
  }

  @Test
  public void return_external_information() {
    UserDto user = db.users().insertUser();

    SearchResults<UserSearchResult> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(
        r -> r.userDto().getLogin(),
        userSearchResult -> userSearchResult.userDto().getExternalLogin(),
        userSearchResult -> userSearchResult.userDto().getExternalIdentityProvider()
      )
      .containsExactlyInAnyOrder(tuple(user.getLogin(), user.getExternalLogin(), user.getExternalIdentityProvider()));
  }

  @Test
  public void return_last_connection_date() {
    UserDto userWithLastConnectionDate = db.users().insertUser();
    db.users().updateLastConnectionDate(userWithLastConnectionDate, 10_000_000_000L);
    UserDto userWithoutLastConnectionDate = db.users().insertUser();

    SearchResults<UserSearchResult> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), userSearchResult -> userSearchResult.userDto().getLastConnectionDate())
      .containsExactlyInAnyOrder(
        tuple(userWithLastConnectionDate.getLogin(), 10_000_000_000L),
        tuple(userWithoutLastConnectionDate.getLogin(), null));
  }

  @Test
  public void return_all_fields_for_logged_user() {
    UserDto user = db.users().insertUser(u -> u.setEmail("aa@bb.com"));
    db.users().updateLastConnectionDate(user, 10_000_000_000L);
    db.users().insertToken(user);
    db.users().insertToken(user);
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);

    SearchResults<UserSearchResult> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(UserSearchResult::userDto)
      .extracting(UserDto::getLogin, UserDto::getName, UserDto::getEmail, UserDto::getExternalLogin, UserDto::getExternalIdentityProvider,
        userDto -> !userDto.getSortedScmAccounts().isEmpty(), UserDto::getLastConnectionDate)
      .containsExactlyInAnyOrder(
        tuple(user.getLogin(), user.getName(), user.getEmail(), user.getExternalLogin(), user.getExternalIdentityProvider(), true, 10_000_000_000L));

    assertThat(users.searchResults())
      .extracting(UserSearchResult::avatar, UserSearchResult::tokensCount, userSearchResult -> userSearchResult.groups().size())
      .containsExactly(tuple(Optional.of("5dcdf28d944831f2fb87d48b81500c66"), 2, 1));

  }

  @Test
  public void search_whenNoPagingInformationProvided_setsDefaultValues() {
    IntStream.rangeClosed(0, 9).forEach(i -> db.users().insertUser(u -> u.setLogin("user-" + i).setName("User " + i)));

    SearchResults<UserSearchResult> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.total()).isEqualTo(10);
  }

  @Test
  public void search_with_paging() {
    IntStream.rangeClosed(0, 9).forEach(i -> db.users().insertUser(u -> u.setLogin("user-" + i).setName("User " + i)));

    SearchResults<UserSearchResult> users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(5).build());

    assertThat(users.searchResults())
      .extracting(u -> u.userDto().getLogin())
      .containsExactly("user-0", "user-1", "user-2", "user-3", "user-4");
    assertThat(users.total()).isEqualTo(10);

    users = userService.findUsers(UsersSearchRequest.builder().setPage(2).setPageSize(5).build());

    assertThat(users.searchResults())
      .extracting(u -> u.userDto().getLogin())
      .containsExactly("user-5", "user-6", "user-7", "user-8", "user-9");
    assertThat(users.total()).isEqualTo(10);

  }

  @Test
  public void return_empty_result_when_no_user() {
    SearchResults<UserSearchResult> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults()).isEmpty();
    assertThat(users.total()).isZero();
  }

  @Test
  public void search_whenFilteringConnectionDate_shouldApplyFilter() {
    final Instant lastConnection = Instant.now();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("user-%_%-login")
      .setName("user-name")
      .setEmail("user@mail.com")
      .setLocal(true)
      .setScmAccounts(singletonList("user1")));
    user = db.users().updateLastConnectionDate(user, lastConnection.toEpochMilli());
    user = db.users().updateSonarLintLastConnectionDate(user, lastConnection.toEpochMilli());

    SearchResults<UserSearchResult> users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(50).setQuery("user-%_%-").build());

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin())
      .containsExactlyInAnyOrder(user.getLogin());

    assertUserWithFilter(b -> b.setLastConnectionDateFrom(DateUtils.formatDateTime(lastConnection.minus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), true);
    assertUserWithFilter(b -> b.setLastConnectionDateFrom(DateUtils.formatDateTime(lastConnection.plus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), false);
    assertUserWithFilter(b -> b.setLastConnectionDateTo(DateUtils.formatDateTime(lastConnection.minus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), false);
    assertUserWithFilter(b -> b.setLastConnectionDateTo(DateUtils.formatDateTime(lastConnection.plus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), true);

    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateFrom(DateUtils.formatDateTime(lastConnection.minus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), true);
    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateFrom(DateUtils.formatDateTime(lastConnection.plus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), false);
    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateTo(DateUtils.formatDateTime(lastConnection.minus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), false);
    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateTo(DateUtils.formatDateTime(lastConnection.plus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), true);

    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateFrom(DateUtils.formatDateTime(lastConnection.toEpochMilli())), user.getLogin(), true);
    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateTo(DateUtils.formatDateTime(lastConnection.toEpochMilli())), user.getLogin(), true);
  }

  @Test
  public void search_whenNoLastConnection_shouldReturnForBeforeOnly() {
    final Instant lastConnection = Instant.now();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("user-%_%-login")
      .setName("user-name")
      .setEmail("user@mail.com")
      .setLocal(true)
      .setScmAccounts(singletonList("user1")));

    assertUserWithFilter(b -> b.setLastConnectionDateFrom(DateUtils.formatDateTime(lastConnection.toEpochMilli())), user.getLogin(), false);
    assertUserWithFilter(b -> b.setLastConnectionDateTo(DateUtils.formatDateTime(lastConnection.toEpochMilli())), user.getLogin(), true);

    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateFrom(DateUtils.formatDateTime(lastConnection.toEpochMilli())), user.getLogin(), false);
    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateTo(DateUtils.formatDateTime(lastConnection.toEpochMilli())), user.getLogin(), true);

  }

  private void assertUserWithFilter(Function<UsersSearchRequest.Builder, UsersSearchRequest.Builder> query, String userLogin, boolean isExpectedToBeThere) {

    UsersSearchRequest.Builder builder = getBuilderWithDefaultsPageSize();
    builder = query.apply(builder);

    SearchResults<UserSearchResult> users = userService.findUsers(builder.setQuery("user-%_%-").build());

    var assertion = assertThat(users.searchResults());
    if (isExpectedToBeThere) {
      assertion
        .extracting(r -> r.userDto().getLogin())
        .containsExactlyInAnyOrder(userLogin);
    } else {
      assertion.isEmpty();
    }
  }

  private void mockUsersAsManaged(String... userUuids) {
    when(managedInstanceService.getUserUuidToManaged(any(), any())).thenAnswer(invocation ->
      {
        Set<?> allUsersUuids = invocation.getArgument(1, Set.class);
        return allUsersUuids.stream()
          .map(userUuid -> (String) userUuid)
          .collect(toMap(identity(), userUuid -> Set.of(userUuids).contains(userUuid)));
      }
    );
  }

  private static UsersSearchRequest.Builder getBuilderWithDefaultsPageSize() {
    return UsersSearchRequest.builder().setPage(1).setPageSize(50);
  }
}
