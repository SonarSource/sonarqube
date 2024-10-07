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
package org.sonar.db.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.scim.ScimUserDto;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;

class UserDaoIT {
  private static final long NOW = 1_500_000_000_000L;

  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession session = db.getSession();
  private final UserDao underTest = db.getDbClient().userDao();

  @Test
  void selectByUuid() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser(user -> user.setActive(false));

    assertThat(underTest.selectByUuid(session, user1.getUuid())).isNotNull();
    assertThat(underTest.selectByUuid(session, user2.getUuid())).isNotNull();
    assertThat(underTest.selectByUuid(session, "unknown")).isNull();
  }

  @Test
  void selectByUuid_withScmAccount_retrievesScmAccounts() {
    List<String> scmAccountsUser1 = List.of("account1_1", "account1_2");
    UserDto user1 = db.users().insertUser(u -> u.setScmAccounts(scmAccountsUser1));
    UserDto user2 = db.users().insertUser(u -> u.setScmAccounts(List.of("account2_1", "account2_2")));

    UserDto userDto = underTest.selectByUuid(session, user1.getUuid());
    assertThat(userDto).isNotNull();
    assertThat(userDto.getSortedScmAccounts()).containsExactlyElementsOf(scmAccountsUser1);
  }

  @Test
  void selectActiveUsersByScmAccountOrLoginOrEmail_findsCorrectResults() {
    String user1 = db.users().insertUser(user -> user.setLogin("user1").setEmail("toto@tata.com")).getUuid();
    String user2 = db.users().insertUser(user -> user.setLogin("user2")).getUuid();
    String user3 = db.users().insertUser(user -> user.setLogin("user3").setScmAccounts(List.of("scmuser3", "scmuser3bis"))).getUuid();
    String user4 = db.users().insertUser(user -> user.setLogin("user4").setEmail("UPPERCASE@tata.com")).getUuid();
    db.users().insertUser();
    db.users().insertUser(user -> user.setLogin("inactive_user1").setActive(false));
    db.users().insertUser(user -> user.setLogin("inactive_user2").setActive(false).setScmAccounts(List.of("inactive_user2")));

    assertThat(underTest.selectActiveUsersByScmAccountOrLoginOrEmail(session, "toto@tata.com"))
      .extracting(UserIdDto::getUuid, UserIdDto::getLogin).containsExactly(new Tuple(user1, "user1"));
    assertThat(underTest.selectActiveUsersByScmAccountOrLoginOrEmail(session, "user2"))
      .extracting(UserIdDto::getUuid, UserIdDto::getLogin).containsExactly(new Tuple(user2, "user2"));
    assertThat(underTest.selectActiveUsersByScmAccountOrLoginOrEmail(session, "scmuser3"))
      .extracting(UserIdDto::getUuid, UserIdDto::getLogin).containsExactly(new Tuple(user3, "user3"));
    assertThat(underTest.selectActiveUsersByScmAccountOrLoginOrEmail(session, "inactive_user1")).isEmpty();
    assertThat(underTest.selectActiveUsersByScmAccountOrLoginOrEmail(session, "inactive_user2")).isEmpty();
    assertThat(underTest.selectActiveUsersByScmAccountOrLoginOrEmail(session, "uppercase@tata.com"))
      .extracting(UserIdDto::getUuid, UserIdDto::getLogin).containsExactly(new Tuple(user4, "user4"));
  }

  @Test
  void selectUserByLogin_ignore_inactive() {
    db.users().insertUser(user -> user.setLogin("user1"));
    db.users().insertUser(user -> user.setLogin("user2"));
    db.users().insertUser(user -> user.setLogin("inactive_user").setActive(false));

    UserDto user = underTest.selectActiveUserByLogin(session, "inactive_user");

    assertThat(user).isNull();
  }

  @Test
  void selectExternalIdentityProviders() {
    db.users().insertUser(user -> user.setLogin("user1").setExternalIdentityProvider("github"));
    db.users().insertUser(user -> user.setLogin("user2").setExternalIdentityProvider("sonarqube"));
    db.users().insertUser(user -> user.setLogin("user3").setExternalIdentityProvider("github"));

    assertThat(underTest.selectExternalIdentityProviders(session)).containsExactlyInAnyOrder("github", "sonarqube");
  }

  @Test
  void selectUserByLogin_not_found() {
    db.users().insertUser(user -> user.setLogin("user"));

    UserDto user = underTest.selectActiveUserByLogin(session, "not_found");

    assertThat(user).isNull();
  }

  @Test
  void selectUsersByLogins() {
    db.users().insertUser(user -> user.setLogin("user1"));
    db.users().insertUser(user -> user.setLogin("user2"));
    db.users().insertUser(user -> user.setLogin("inactive_user").setActive(false));

    Collection<UserDto> users = underTest.selectByLogins(session, asList("user1", "inactive_user", "other"));

    assertThat(users).extracting("login").containsExactlyInAnyOrder("user1", "inactive_user");
  }

  @Test
  void selectUsersByUuids() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser(user -> user.setActive(false));

    assertThat((Collection<UserDto>) underTest.selectByUuids(session, asList(user1.getUuid(), user2.getUuid(), user3.getUuid()))).hasSize(3);
    assertThat((Collection<UserDto>) underTest.selectByUuids(session, asList(user1.getUuid(), "unknown"))).hasSize(1);
    assertThat((Collection<UserDto>) underTest.selectByUuids(session, Collections.emptyList())).isEmpty();
  }

  @Test
  void selectUsersByLogins_empty_logins() {
    // no need to access db
    Collection<UserDto> users = underTest.selectByLogins(session, emptyList());
    assertThat(users).isEmpty();
  }

  @Test
  void selectByOrderedLogins() {
    db.users().insertUser(user -> user.setLogin("U1"));
    db.users().insertUser(user -> user.setLogin("U2"));

    Iterable<UserDto> users = underTest.selectByOrderedLogins(session, asList("U1", "U2", "U3"));
    assertThat(users).extracting("login").containsExactly("U1", "U2");

    users = underTest.selectByOrderedLogins(session, asList("U2", "U3", "U1"));
    assertThat(users).extracting("login").containsExactly("U2", "U1");

    assertThat(underTest.selectByOrderedLogins(session, emptyList())).isEmpty();
  }

  @Test
  void selectUsersByQuery_all() {
    db.users().insertUser(user -> user.setLogin("user").setName("User"));
    db.users().insertUser(user -> user.setLogin("inactive_user").setName("Disabled").setActive(false));

    List<UserDto> users = underTest.selectUsers(session, UserQuery.builder().build());

    assertThat(users).hasSize(2);
  }

  @Test
  void selectUsersByQuery_only_actives() {
    db.users().insertUser(user -> user.setLogin("user").setName("User"));
    db.users().insertUser(user -> user.setLogin("inactive_user").setName("Disabled").setActive(false));

    List<UserDto> users = underTest.selectUsers(session, UserQuery.builder().isActive(true).build());

    assertThat(users).extracting(UserDto::getName).containsExactlyInAnyOrder("User");
  }

  @Test
  void selectUsersByQuery_whenSearchTextMatchPartOfTheLoginCaseInsensitively_findsTheRightResults() {
    db.users().insertUser(user -> user.setLogin("tata"));
    UserDto userToFind = db.users().insertUser(user -> user.setLogin("simon"));
    UserDto userToFind2 = db.users().insertUser(user -> user.setLogin("ToSimonTo"));

    UserQuery query = UserQuery.builder().searchText("Simon").build();
    List<UserDto> users = underTest.selectUsers(session, query);

    assertThat(users).usingRecursiveFieldByFieldElementComparator().containsOnly(userToFind, userToFind2);
    assertThat(underTest.countUsers(session, query)).isEqualTo(2);
  }

  @Test
  void selectUsersByQuery_whenSearchTextMatchPartOfTheNameCaseInsensitively_findsTheRightResults() {
    db.users().insertUser(user -> user.setName("tata"));
    UserDto userToFind = db.users().insertUser(user -> user.setName("simon"));
    UserDto userToFind2 = db.users().insertUser(user -> user.setName("ToSimonTo"));

    UserQuery query = UserQuery.builder().searchText("Simon").build();
    List<UserDto> users = underTest.selectUsers(session, query);

    assertThat(users).usingRecursiveFieldByFieldElementComparator().containsOnly(userToFind, userToFind2);
    assertThat(underTest.countUsers(session, query)).isEqualTo(2);
  }

  @Test
  void selectUsersByQuery_whenSearchTextMatchPartOfTheEmailCaseInsensitively_findsTheRightResults() {
    db.users().insertUser(user -> user.setEmail("user@user.com"));
    UserDto userToFind = db.users().insertUser(user -> user.setEmail("simon@brandhof.com"));
    UserDto userToFind2 = db.users().insertUser(user -> user.setEmail("tagadasimon2@brandhof.com"));

    UserQuery query = UserQuery.builder().searchText("Simon").build();
    List<UserDto> users = underTest.selectUsers(session, query);

    assertThat(users).usingRecursiveFieldByFieldElementComparator().containsOnly(userToFind, userToFind2);
    assertThat(underTest.countUsers(session, query)).isEqualTo(2);
  }

  @Test
  void selectUsersByQuery_escape_special_characters_in_like() {
    db.users().insertUser(user -> user.setLogin("user").setName("User"));
    db.users().insertUser(user -> user.setLogin("sbrandhof").setName("Simon Brandhof"));

    UserQuery query = UserQuery.builder().searchText("%s%").build();
    // we expect really a login or name containing the 3 characters "%s%"

    List<UserDto> users = underTest.selectUsers(session, query);
    assertThat(users).isEmpty();
  }

  @Test
  void selectUsersByQuery_whenSearchingByUuids_findsTheRightResults() {
    db.users().insertUser();
    UserDto userToFind1 = db.users().insertUser();
    UserDto userToFind2 = db.users().insertUser();

    UserQuery query = UserQuery.builder().userUuids(Set.of(userToFind1.getUuid(), userToFind2.getUuid())).build();
    List<UserDto> users = underTest.selectUsers(session, query);

    assertThat(users).usingRecursiveFieldByFieldElementComparator().containsOnly(userToFind1, userToFind2);
    assertThat(underTest.countUsers(session, query)).isEqualTo(2);
  }

  @Test
  void selectUsersByQuery_whenSearchingByGroupUuid_findsTheRightResults() {
    db.users().insertUser();
    UserDto userToFind1 = db.users().insertUser(u -> u.setLogin("z"));
    UserDto userToFind2 = db.users().insertUser(u -> u.setLogin("a"));

    GroupDto groupDto = db.users().insertGroup();
    db.users().insertMember(groupDto, userToFind2);
    db.users().insertMember(groupDto, userToFind1);

    UserQuery query = UserQuery.builder().groupUuid(groupDto.getUuid()).build();
    List<UserDto> users = underTest.selectUsers(session, query);

    assertThat(users).usingRecursiveFieldByFieldElementComparator().containsExactly(userToFind2, userToFind1);
    assertThat(underTest.countUsers(session, query)).isEqualTo(2);
  }

  @Test
  void selectUsersByQuery_whenExcludingGroupUuid_findsTheRightResults() {
    UserDto userToFind1 = db.users().insertUser(u -> u.setLogin("z"));
    UserDto userToFind2 = db.users().insertUser(u -> u.setLogin("a"));
    UserDto userToFind3 = db.users().insertUser(u -> u.setLogin("b"));

    GroupDto groupDto = db.users().insertGroup();
    db.users().insertMember(groupDto, userToFind2);
    db.users().insertMember(groupDto, userToFind1);

    UserQuery query = UserQuery.builder().excludedGroupUuid(groupDto.getUuid()).build();
    List<UserDto> users = underTest.selectUsers(session, query);

    assertThat(users).usingRecursiveFieldByFieldElementComparator().containsExactly(userToFind3);
    assertThat(underTest.countUsers(session, query)).isEqualTo(1);
  }

  @Test
  void selectUsersByQuery_whenSearchingByUuidsWithLongRange_shouldReturnTheExpectedUsers() {
    db.users().insertUser();
    List<UserDto> users = generateAndInsertUsers(3200);
    Set<String> userUuids = users.stream()
      .map(UserDto::getUuid)
      .collect(toSet());

    UserQuery query = UserQuery.builder().userUuids(userUuids).build();
    List<UserDto> actualUsers = underTest.selectUsers(session, query);

    assertThat(actualUsers.stream().map(UserDto::getUuid).collect(toSet()))
      .containsExactlyInAnyOrderElementsOf(userUuids);
  }

  private List<UserDto> generateAndInsertUsers(int totalUsers) {
    return IntStream.range(0, totalUsers)
      .mapToObj(i -> db.users().insertUser())
      .toList();
  }

  private static Object[][] paginationTestCases() {
    return new Object[][]{
      {100, 1, 5},
      {100, 3, 18},
      {2075, 41, 50},
      {0, 2, 5},
    };
  }

  @ParameterizedTest
  @MethodSource("paginationTestCases")
  void selectUsers_whenUsingPagination_findsTheRightResults(int numberOfUsersToGenerate, int offset, int limit) {
    Map<String, UserDto> allUsers = generateUsers(numberOfUsersToGenerate);

    UserQuery query = UserQuery.builder().build();
    List<UserDto> users = underTest.selectUsers(session, query, offset, limit);

    Set<UserDto> expectedUsers = getExpectedUsers(offset, limit, allUsers);

    assertThat(users).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrderElementsOf(expectedUsers);
    assertThat(underTest.countUsers(session, query)).isEqualTo(numberOfUsersToGenerate);
  }

  private Map<String, UserDto> generateUsers(int numberOfUsersToGenerate) {
    if (numberOfUsersToGenerate == 0) {
      return emptyMap();
    }
    return IntStream.range(1000, 1000 + numberOfUsersToGenerate)
      .mapToObj(i -> db.users().insertUser(user -> user.setLogin(i + "_user").setName(i + "_name")))
      .collect(toMap(UserDto::getName, Function.identity()));
  }

  private static Set<UserDto> getExpectedUsers(int offset, int limit, Map<String, UserDto> allUsers) {
    if (allUsers.isEmpty()) {
      return emptySet();
    }
    return IntStream.range(1000 + (offset - 1) * limit, 1000 + offset * limit)
      .mapToObj(i -> allUsers.get(i + "_name"))
      .collect(toSet());
  }

  @Test
  void insert_user_with_default_values() {
    UserDto userDto = new UserDto()
      .setLogin("john")
      .setName("John")
      .setEmail("jo@hn.com")
      .setExternalLogin("john-1")
      .setExternalIdentityProvider("sonarqube")
      .setExternalId("EXT_ID");
    underTest.insert(db.getSession(), userDto);
    db.getSession().commit();

    UserDto user = underTest.selectActiveUserByLogin(session, "john");
    assertThat(user).isNotNull();
    assertThat(user.getUuid()).isNotNull();
    assertThat(user.isActive()).isTrue();
    assertThat(user.isResetPassword()).isFalse();
    assertThat(user.isLocal()).isTrue();

    assertThat(user.getSortedScmAccounts()).isEmpty();
    assertThat(user.getHashMethod()).isNull();
    assertThat(user.getLastConnectionDate()).isNull();
    assertThat(user.getHomepageType()).isNull();
    assertThat(user.getHomepageParameter()).isNull();
  }

  @Test
  void insert_user() {
    long date = DateUtils.parseDate("2014-06-20").getTime();

    UserDto userDto = new UserDto()
      .setLogin("john")
      .setName("John")
      .setEmail("jo@hn.com")
      .setScmAccounts(List.of("jo.hn", "john2", "", "JoHn"))
      .setActive(true)
      .setResetPassword(true)
      .setSalt("1234")
      .setCryptedPassword("abcd")
      .setHashMethod("SHA1")
      .setExternalLogin("johngithub")
      .setExternalIdentityProvider("github")
      .setExternalId("EXT_ID")
      .setLocal(true)
      .setHomepageType("project")
      .setHomepageParameter("OB1")
      .setCreatedAt(date)
      .setUpdatedAt(date);
    underTest.insert(db.getSession(), userDto);
    db.getSession().commit();

    UserDto user = underTest.selectActiveUserByLogin(session, "john");
    assertThat(user).isNotNull();
    assertThat(user.getUuid()).isNotNull();
    assertThat(user.getLogin()).isEqualTo("john");
    assertThat(user.getName()).isEqualTo("John");
    assertThat(user.getEmail()).isEqualTo("jo@hn.com");
    assertThat(user.isActive()).isTrue();
    assertThat(user.isResetPassword()).isTrue();
    assertThat(user.getSortedScmAccounts()).containsExactly("jo.hn", "john", "john2");
    assertThat(user.getSalt()).isEqualTo("1234");
    assertThat(user.getCryptedPassword()).isEqualTo("abcd");
    assertThat(user.getHashMethod()).isEqualTo("SHA1");
    assertThat(user.getExternalLogin()).isEqualTo("johngithub");
    assertThat(user.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(user.getExternalId()).isEqualTo("EXT_ID");
    assertThat(user.isLocal()).isTrue();
    assertThat(user.getHomepageType()).isEqualTo("project");
    assertThat(user.getHomepageParameter()).isEqualTo("OB1");
  }

  @Test
  void insert_user_does_not_set_last_connection_date() {
    UserDto user = newUserDto().setLastConnectionDate(10_000_000_000L);
    underTest.insert(db.getSession(), user);
    db.getSession().commit();

    UserDto reloaded = underTest.selectByUuid(db.getSession(), user.getUuid());

    assertThat(reloaded.getLastConnectionDate()).isNull();
  }

  @Test
  void update_user() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("john")
      .setName("John")
      .setEmail("jo@hn.com")
      .setActive(true)
      .setLocal(true)
      .setResetPassword(false));

    underTest.update(db.getSession(), newUserDto()
      .setUuid(user.getUuid())
      .setLogin("johnDoo")
      .setName("John Doo")
      .setEmail("jodoo@hn.com")
      .setScmAccounts(List.of("jo.hn", "john2", "johndoo", ""))
      .setActive(false)
      .setResetPassword(true)
      .setSalt("12345")
      .setCryptedPassword("abcde")
      .setHashMethod("BCRYPT")
      .setExternalLogin("johngithub")
      .setExternalIdentityProvider("github")
      .setExternalId("EXT_ID")
      .setLocal(false)
      .setHomepageType("project")
      .setHomepageParameter("OB1")
      .setLastConnectionDate(10_000_000_000L));

    UserDto reloaded = underTest.selectByUuid(db.getSession(), user.getUuid());
    assertThat(reloaded).isNotNull();
    assertThat(reloaded.getUuid()).isEqualTo(user.getUuid());
    assertThat(reloaded.getLogin()).isEqualTo("johnDoo");
    assertThat(reloaded.getName()).isEqualTo("John Doo");
    assertThat(reloaded.getEmail()).isEqualTo("jodoo@hn.com");
    assertThat(reloaded.isActive()).isFalse();
    assertThat(reloaded.isResetPassword()).isTrue();
    assertThat(reloaded.getSortedScmAccounts()).containsExactly("jo.hn", "john2", "johndoo");
    assertThat(reloaded.getSalt()).isEqualTo("12345");
    assertThat(reloaded.getCryptedPassword()).isEqualTo("abcde");
    assertThat(reloaded.getHashMethod()).isEqualTo("BCRYPT");
    assertThat(reloaded.getExternalLogin()).isEqualTo("johngithub");
    assertThat(reloaded.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(reloaded.getExternalId()).isEqualTo("EXT_ID");
    assertThat(reloaded.isLocal()).isFalse();
    assertThat(reloaded.getHomepageType()).isEqualTo("project");
    assertThat(reloaded.getHomepageParameter()).isEqualTo("OB1");
    assertThat(reloaded.getLastConnectionDate()).isEqualTo(10_000_000_000L);
  }

  @Test
  void update_scmAccounts() {
    UserDto user = db.users().insertUser(u -> u.setScmAccounts(emptyList()));

    underTest.update(db.getSession(), user.setScmAccounts(List.of("jo.hn", "john2", "johndooUpper", "")));
    UserDto reloaded = Objects.requireNonNull(underTest.selectByUuid(db.getSession(), user.getUuid()));
    assertThat(reloaded.getSortedScmAccounts()).containsExactly("jo.hn", "john2", "johndooupper");

    underTest.update(db.getSession(), user.setScmAccounts(List.of("jo.hn", "john2")));
    reloaded = Objects.requireNonNull(underTest.selectByUuid(db.getSession(), user.getUuid()));
    assertThat(reloaded.getSortedScmAccounts()).containsExactly("jo.hn", "john2");

    underTest.update(db.getSession(), user.setScmAccounts(List.of("jo.hn", "john3", "john2")));
    reloaded = Objects.requireNonNull(underTest.selectByUuid(db.getSession(), user.getUuid()));
    assertThat(reloaded.getSortedScmAccounts()).containsExactly("jo.hn", "john2", "john3");
  }

  @Test
  void deactivate_user() {
    UserDto user = insertActiveUser();
    insertUserGroup(user);
    UserDto otherUser = insertActiveUser();
    underTest.update(db.getSession(), user.setLastConnectionDate(10_000_000_000L));
    session.commit();

    underTest.deactivateUser(session, user);

    UserDto userReloaded = underTest.selectByUuid(session, user.getUuid());
    assertThat(userReloaded.isActive()).isFalse();
    assertThat(userReloaded.getName()).isEqualTo(user.getName());
    assertThat(userReloaded.getLogin()).isEqualTo(user.getLogin());
    assertThat(userReloaded.getExternalId()).isEqualTo(user.getExternalId());
    assertThat(userReloaded.getExternalLogin()).isEqualTo(user.getExternalLogin());
    assertThat(userReloaded.getExternalIdentityProvider()).isEqualTo(user.getExternalIdentityProvider());
    assertThat(userReloaded.getEmail()).isNull();
    assertThat(userReloaded.getSortedScmAccounts()).isEmpty();
    assertThat(userReloaded.getSalt()).isNull();
    assertThat(userReloaded.getCryptedPassword()).isNull();
    assertThat(userReloaded.getUpdatedAt()).isEqualTo(NOW);
    assertThat(userReloaded.getHomepageType()).isNull();
    assertThat(userReloaded.getHomepageParameter()).isNull();
    assertThat(userReloaded.getLastConnectionDate()).isNull();
    assertThat(underTest.selectByUuid(session, otherUser.getUuid())).isNotNull();
  }

  @Test
  void clean_users_homepage_when_deleting_project() {

    UserDto userUnderTest = newUserDto().setHomepageType("PROJECT").setHomepageParameter("dummy-project-UUID");
    underTest.insert(session, userUnderTest);

    UserDto untouchedUser = newUserDto().setHomepageType("PROJECT").setHomepageParameter("not-so-dummy-project-UUID");
    underTest.insert(session, untouchedUser);

    session.commit();

    underTest.cleanHomepage(session, new ProjectDto().setUuid("dummy-project-UUID"));

    UserDto userWithAHomepageReloaded = underTest.selectByUuid(session, userUnderTest.getUuid());
    assertThat(userWithAHomepageReloaded.getUpdatedAt()).isEqualTo(NOW);
    assertThat(userWithAHomepageReloaded.getHomepageType()).isNull();
    assertThat(userWithAHomepageReloaded.getHomepageParameter()).isNull();

    UserDto untouchedUserReloaded = underTest.selectByUuid(session, untouchedUser.getUuid());
    assertThat(untouchedUserReloaded.getUpdatedAt()).isEqualTo(untouchedUser.getUpdatedAt());
    assertThat(untouchedUserReloaded.getHomepageType()).isEqualTo(untouchedUser.getHomepageType());
    assertThat(untouchedUserReloaded.getHomepageParameter()).isEqualTo(untouchedUser.getHomepageParameter());
  }

  @Test
  void update_last_sonarlint_connection_date() {
    UserDto user = db.users().insertUser();
    assertThat(user.getLastSonarlintConnectionDate()).isNull();
    underTest.updateSonarlintLastConnectionDate(db.getSession(), user.getLogin());
    assertThat(underTest.selectByLogin(db.getSession(), user.getLogin()).getLastSonarlintConnectionDate()).isEqualTo(NOW);
  }

  @Test
  void count_sonarlint_weekly_users() {
    UserDto user1 = db.users().insertUser(c -> c.setLastSonarlintConnectionDate(NOW - 100_000));
    UserDto user2 = db.users().insertUser(c -> c.setLastSonarlintConnectionDate(NOW));
    // these don't count
    UserDto user3 = db.users().insertUser(c -> c.setLastSonarlintConnectionDate(NOW - 1_000_000_000));
    UserDto user4 = db.users().insertUser();

    assertThat(underTest.countSonarlintWeeklyUsers(db.getSession())).isEqualTo(2);
  }

  @Test
  void count_active_users() {
    db.users().insertUser();
    db.users().insertUser();
    db.users().insertUser();
    db.users().insertUser(c -> c.setActive(false));

    assertThat(underTest.countActiveUsers(db.getSession())).isEqualTo(3);
  }

  @Test
  void clean_user_homepage() {

    UserDto user = newUserDto().setHomepageType("RANDOM").setHomepageParameter("any-string");
    underTest.insert(session, user);
    session.commit();

    underTest.cleanHomepage(session, user);

    UserDto reloaded = underTest.selectByUuid(session, user.getUuid());
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);
    assertThat(reloaded.getHomepageType()).isNull();
    assertThat(reloaded.getHomepageParameter()).isNull();

  }

  @Test
  void does_not_fail_to_deactivate_missing_user() {
    assertThatNoException().isThrownBy(() -> underTest.deactivateUser(session, newUserDto()));
  }

  @Test
  void select_by_login() {
    UserDto user1 = db.users().insertUser(user -> user
      .setLogin("marius")
      .setName("Marius")
      .setEmail("marius@lesbronzes.fr")
      .setActive(true)
      .setScmAccounts(List.of("ma", "marius33"))
      .setSalt("79bd6a8e79fb8c76ac8b121cc7e8e11ad1af8365")
      .setCryptedPassword("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg")
      .setHomepageType("project")
      .setHomepageParameter("OB1"));

    UserDto dto = underTest.selectByLogin(session, user1.getLogin());
    assertThat(dto.getUuid()).isEqualTo(user1.getUuid());
    assertThat(dto.getLogin()).isEqualTo("marius");
    assertThat(dto.getName()).isEqualTo("Marius");
    assertThat(dto.getEmail()).isEqualTo("marius@lesbronzes.fr");
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.getSortedScmAccounts()).containsExactly("ma", "marius33");
    assertThat(dto.getSalt()).isEqualTo("79bd6a8e79fb8c76ac8b121cc7e8e11ad1af8365");
    assertThat(dto.getCryptedPassword()).isEqualTo("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg");
    assertThat(dto.getCreatedAt()).isEqualTo(user1.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isEqualTo(user1.getUpdatedAt());
    assertThat(dto.getHomepageType()).isEqualTo("project");
    assertThat(dto.getHomepageParameter()).isEqualTo("OB1");

  }

  @Test
  void select_nullable_by_scm_account() {
    db.users().insertUser(user -> user.setLogin("marius").setName("Marius").setEmail("marius@lesbronzes.fr").setScmAccounts(asList("ma",
      "marius33")));
    db.users().insertUser(user -> user.setLogin("sbrandhof").setName("Simon Brandhof").setEmail("sbrandhof@lesbronzes.fr").setScmAccounts(emptyList()));

    List<UserDto> searchByMa = underTest.selectByScmAccountOrLoginOrEmail(session, "ma");
    assertThat(searchByMa).extracting(UserDto::getLogin).containsExactly("marius");
    assertThat(searchByMa.iterator().next().getSortedScmAccounts()).containsExactly("ma", "marius33");

    assertThat(underTest.selectByScmAccountOrLoginOrEmail(session, "marius")).extracting(UserDto::getLogin).containsExactly("marius");
    assertThat(underTest.selectByScmAccountOrLoginOrEmail(session, "marius@lesbronzes.fr")).extracting(UserDto::getLogin).containsExactly("marius");
    assertThat(underTest.selectByScmAccountOrLoginOrEmail(session, "m")).isEmpty();
    assertThat(underTest.selectByScmAccountOrLoginOrEmail(session, "unknown")).isEmpty();
  }

  @Test
  void select_nullable_by_scm_account_return_many_results_when_same_email_is_used_by_many_users() {
    db.users().insertUser(user -> user.setLogin("marius").setName("Marius").setEmail("marius@lesbronzes.fr").setScmAccounts(asList("ma",
      "marius33")));
    db.users().insertUser(user -> user.setLogin("sbrandhof").setName("Simon Brandhof").setEmail("marius@lesbronzes.fr").setScmAccounts(emptyList()));

    List<UserDto> results = underTest.selectByScmAccountOrLoginOrEmail(session, "marius@lesbronzes.fr");

    assertThat(results).hasSize(2);
  }

  @Test
  void select_nullable_by_login() {
    db.users().insertUser(user -> user.setLogin("marius"));
    db.users().insertUser(user -> user.setLogin("sbrandhof"));

    assertThat(underTest.selectByLogin(session, "marius")).isNotNull();
    assertThat(underTest.selectByLogin(session, "unknown")).isNull();
  }

  @Test
  void select_by_email() {
    UserDto activeUser1 = db.users().insertUser(u -> u.setEmail("user1@email.com"));
    UserDto activeUser2 = db.users().insertUser(u -> u.setEmail("user1@email.com"));
    UserDto disableUser = db.users().insertUser(u -> u.setActive(false));

    assertThat(underTest.selectByEmail(session, "user1@email.com")).hasSize(2);
    assertThat(underTest.selectByEmail(session, disableUser.getEmail())).isEmpty();
    assertThat(underTest.selectByEmail(session, "unknown")).isEmpty();
  }

  @Test
  void select_by_external_id_and_identity_provider() {
    UserDto activeUser = db.users().insertUser();
    UserDto disableUser = db.users().insertUser(u -> u.setActive(false));

    assertThat(underTest.selectByExternalIdAndIdentityProvider(session, activeUser.getExternalId(),
      activeUser.getExternalIdentityProvider())).isNotNull();
    assertThat(underTest.selectByExternalIdAndIdentityProvider(session, disableUser.getExternalId(),
      disableUser.getExternalIdentityProvider())).isNotNull();
    assertThat(underTest.selectByExternalIdAndIdentityProvider(session, "unknown", "unknown")).isNull();
  }

  @Test
  void select_by_external_ids_and_identity_provider() {
    UserDto user1 = db.users().insertUser(u -> u.setExternalIdentityProvider("github"));
    UserDto user2 = db.users().insertUser(u -> u.setExternalIdentityProvider("github"));
    UserDto user3 = db.users().insertUser(u -> u.setExternalIdentityProvider("bitbucket"));
    UserDto disableUser = db.users().insertDisabledUser(u -> u.setExternalIdentityProvider("github"));

    assertThat(underTest.selectByExternalIdsAndIdentityProvider(session, singletonList(user1.getExternalId()), "github"))
      .extracting(UserDto::getUuid).containsExactlyInAnyOrder(user1.getUuid());
    assertThat(underTest.selectByExternalIdsAndIdentityProvider(session,
      asList(user1.getExternalId(), user2.getExternalId(), user3.getExternalId(), disableUser.getExternalId()), "github"))
      .extracting(UserDto::getUuid).containsExactlyInAnyOrder(user1.getUuid(), user2.getUuid(), disableUser.getUuid());
    assertThat(underTest.selectByExternalIdsAndIdentityProvider(session, singletonList("unknown"), "github")).isEmpty();
    assertThat(underTest.selectByExternalIdsAndIdentityProvider(session, singletonList(user1.getExternalId()), "unknown")).isEmpty();
  }

  @Test
  void select_by_external_login_and_identity_provider() {
    UserDto activeUser = db.users().insertUser();
    UserDto disableUser = db.users().insertUser(u -> u.setActive(false));

    assertThat(underTest.selectByExternalLoginAndIdentityProvider(session, activeUser.getExternalLogin(),
      activeUser.getExternalIdentityProvider())).isNotNull();
    assertThat(underTest.selectByExternalLoginAndIdentityProvider(session, disableUser.getExternalLogin(),
      disableUser.getExternalIdentityProvider())).isNotNull();
    assertThat(underTest.selectByExternalLoginAndIdentityProvider(session, "unknown", "unknown")).isNull();
  }

  @Test
  void scrollByLUuids() {
    UserDto u1 = insertUser(true);
    UserDto u2 = insertUser(false);
    UserDto u3 = insertUser(false);

    List<UserDto> result = new ArrayList<>();
    underTest.scrollByUuids(db.getSession(), asList(u2.getUuid(), u3.getUuid(), "does not exist"), result::add);

    assertThat(result).extracting(UserDto::getUuid, UserDto::getName)
      .containsExactlyInAnyOrder(tuple(u2.getUuid(), u2.getName()), tuple(u3.getUuid(), u3.getName()));
  }

  @Test
  void scrollByUuids_scrolls_by_pages_of_1000_uuids() {
    List<String> uuids = new ArrayList<>();
    for (int i = 0; i < DatabaseUtils.PARTITION_SIZE_FOR_ORACLE + 10; i++) {
      uuids.add(insertUser(true).getUuid());
    }

    List<UserDto> result = new ArrayList<>();
    underTest.scrollByUuids(db.getSession(), uuids, result::add);

    assertThat(result)
      .extracting(UserDto::getUuid)
      .containsExactlyInAnyOrder(uuids.toArray(new String[0]));
  }

  @Test
  void scrollAll() {
    UserDto u1 = insertUser(true);
    UserDto u2 = insertUser(false);

    List<UserDto> result = new ArrayList<>();
    underTest.scrollAll(db.getSession(), result::add);

    assertThat(result).extracting(UserDto::getLogin, UserDto::getName)
      .containsExactlyInAnyOrder(tuple(u1.getLogin(), u1.getName()), tuple(u2.getLogin(), u2.getName()));
  }

  @Test
  void selectUserTelemetry() {
    UserDto u1 = insertUser(true);
    UserDto u2 = insertUser(false);

    List<UserTelemetryDto> result = underTest.selectUsersForTelemetry(db.getSession());

    assertThat(result)
      .extracting(UserTelemetryDto::getUuid, UserTelemetryDto::isActive, UserTelemetryDto::getLastConnectionDate,
        UserTelemetryDto::getLastSonarlintConnectionDate,
        UserTelemetryDto::getScimUuid)
      .containsExactlyInAnyOrder(
        tuple(u1.getUuid(), u1.isActive(), u1.getLastConnectionDate(), u1.getLastSonarlintConnectionDate(), null),
        tuple(u2.getUuid(), u2.isActive(), u2.getLastConnectionDate(), u2.getLastSonarlintConnectionDate(), null)
      );
  }

  @Test
  void selectUserTelemetryUpdatedLastConnectionDate() {
    UserDto u1 = insertUser(true);
    UserDto u2 = insertUser(false);

    assertThat(underTest.selectUsersForTelemetry(db.getSession()))
      .extracting(UserTelemetryDto::getUuid, UserTelemetryDto::isActive, UserTelemetryDto::getLastConnectionDate,
        UserTelemetryDto::getLastSonarlintConnectionDate)
      .containsExactlyInAnyOrder(
        tuple(u1.getUuid(), u1.isActive(), null, u1.getLastSonarlintConnectionDate()),
        tuple(u2.getUuid(), u2.isActive(), null, u2.getLastSonarlintConnectionDate()));

    underTest.update(db.getSession(), u1.setLastConnectionDate(10_000_000_000L));
    underTest.update(db.getSession(), u2.setLastConnectionDate(20_000_000_000L));

    assertThat(underTest.selectUsersForTelemetry(db.getSession()))
      .extracting(UserTelemetryDto::getUuid, UserTelemetryDto::isActive, UserTelemetryDto::getLastConnectionDate,
        UserTelemetryDto::getLastSonarlintConnectionDate)
      .containsExactlyInAnyOrder(
        tuple(u1.getUuid(), u1.isActive(), 10_000_000_000L, u1.getLastSonarlintConnectionDate()),
        tuple(u2.getUuid(), u2.isActive(), 20_000_000_000L, u2.getLastSonarlintConnectionDate()));
  }

  @Test
  void selectUserTelemetryWithScim() {
    UserDto u1 = insertUser(true);
    UserDto u2 = insertUser(false);
    ScimUserDto scimUser1 = enableScimForUser(u1);

    List<UserTelemetryDto> result = underTest.selectUsersForTelemetry(db.getSession());

    assertThat(result)
      .extracting(UserTelemetryDto::getUuid, UserTelemetryDto::isActive, UserTelemetryDto::getLastConnectionDate,
        UserTelemetryDto::getLastSonarlintConnectionDate,
        UserTelemetryDto::getScimUuid)
      .containsExactlyInAnyOrder(
        tuple(u1.getUuid(), u1.isActive(), u1.getLastConnectionDate(), u1.getLastSonarlintConnectionDate(), scimUser1.getScimUserUuid()),
        tuple(u2.getUuid(), u2.isActive(), u2.getLastConnectionDate(), u2.getLastSonarlintConnectionDate(), null)
      );
  }

  private UserDto insertActiveUser() {
    return insertUser(true);
  }

  private UserDto insertUser(boolean active) {
    UserDto dto = newUserDto().setActive(active);
    underTest.insert(session, dto);
    return dto;
  }

  private ScimUserDto enableScimForUser(UserDto userDto) {
    return dbClient.scimUserDao().enableScimForUser(db.getSession(), userDto.getUuid());
  }

  private UserGroupDto insertUserGroup(UserDto user) {
    GroupDto group = newGroupDto().setName(secure().nextAlphanumeric(30));
    dbClient.groupDao().insert(session, group);

    UserGroupDto dto = new UserGroupDto().setUserUuid(user.getUuid()).setGroupUuid(group.getUuid());
    dbClient.userGroupDao().insert(session, dto, group.getName(), user.getLogin());
    return dto;
  }
}
