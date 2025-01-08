/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.scim;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.OffsetBasedPagination;
import org.sonar.db.Pagination;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.assertj.core.api.Fail.fail;

class ScimUserDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create();
  private final DbSession dbSession = db.getSession();
  private final ScimUserDao scimUserDao = db.getDbClient().scimUserDao();

  @Test
  void findAll_ifNoData_returnsEmptyList() {
    assertThat(scimUserDao.findAll(dbSession)).isEmpty();
  }

  @Test
  void findAll_returnsAllEntries() {
    ScimUserTestData scimUser1TestData = insertScimUser("scimUser1");
    ScimUserTestData scimUser2TestData = insertScimUser("scimUser2");

    List<ScimUserDto> scimUserDtos = scimUserDao.findAll(dbSession);

    assertThat(scimUserDtos).hasSize(2)
      .map(scimUserDto -> new ScimUserTestData(scimUserDto.getScimUserUuid(), scimUserDto.getUserUuid()))
      .containsExactlyInAnyOrder(scimUser1TestData, scimUser2TestData);
  }

  @Test
  void findByScimUuid_whenScimUuidNotFound_shouldReturnEmptyOptional() {
    assertThat(scimUserDao.findByScimUuid(dbSession, "unknownId")).isEmpty();
  }

  @Test
  void findByScimUuid_whenScimUuidFound_shouldReturnDto() {
    ScimUserTestData scimUser1TestData = insertScimUser("scimUser1");
    insertScimUser("scimUser2");

    ScimUserDto scimUserDto = scimUserDao.findByScimUuid(dbSession, scimUser1TestData.getScimUserUuid())
      .orElseGet(() -> fail("User not found"));

    assertThat(scimUserDto.getScimUserUuid()).isEqualTo(scimUser1TestData.getScimUserUuid());
    assertThat(scimUserDto.getUserUuid()).isEqualTo(scimUser1TestData.getUserUuid());
  }

  @Test
  void findByUserUuid_whenScimUuidNotFound_shouldReturnEmptyOptional() {
    assertThat(scimUserDao.findByUserUuid(dbSession, "unknownId")).isEmpty();
  }

  @Test
  void findByUserUuid_whenScimUuidFound_shouldReturnDto() {
    ScimUserTestData scimUser1TestData = insertScimUser("scimUser1");
    insertScimUser("scimUser2");

    ScimUserDto scimUserDto = scimUserDao.findByUserUuid(dbSession, scimUser1TestData.getUserUuid())
      .orElseGet(() -> fail("User not found"));

    assertThat(scimUserDto.getScimUserUuid()).isEqualTo(scimUser1TestData.getScimUserUuid());
    assertThat(scimUserDto.getUserUuid()).isEqualTo(scimUser1TestData.getUserUuid());
  }

  static Object[][] paginationData() {
    return new Object[][]{
      {5, 0, 20, List.of("1", "2", "3", "4", "5")},
      {9, 0, 5, List.of("1", "2", "3", "4", "5")},
      {9, 3, 3, List.of("4", "5", "6")},
      {9, 7, 3, List.of("8", "9")},
      {5, 5, 20, List.of()},
    };
  }

  @ParameterizedTest
  @MethodSource("paginationData")
  void findScimUsers_whenPaginationAndStartIndex_shouldReturnTheCorrectNumberOfScimUsers(int totalScimUsers, int offset, int pageSize,
    List<String> expectedScimUserUuids) {
    generateScimUsers(totalScimUsers);

    List<ScimUserWithUsernameDto> scimUserDtos = scimUserDao.findScimUsers(dbSession, ScimUserQuery.empty(), OffsetBasedPagination.forOffset(offset,
      pageSize));

    List<String> scimUsersUuids = toScimUsersUuids(scimUserDtos);
    assertThat(scimUsersUuids).containsExactlyElementsOf(expectedScimUserUuids);
  }

  private List<String> toScimUsersUuids(Collection<ScimUserWithUsernameDto> scimUserDtos) {
    return scimUserDtos.stream()
      .map(ScimUserWithUsernameDto::getScimUserUuid)
      .toList();
  }

  @Test
  void countScimUsers_shouldReturnTheTotalNumberOfScimUsers() {
    int totalScimUsers = 15;
    generateScimUsers(totalScimUsers);

    assertThat(scimUserDao.countScimUsers(dbSession, ScimUserQuery.empty())).isEqualTo(totalScimUsers);
  }

  @Test
  void countScimUsers_shouldReturnZero_whenNoScimUsers() {
    assertThat(scimUserDao.countScimUsers(dbSession, ScimUserQuery.empty())).isZero();
  }

  @Test
  void countScimUsers_shoudReturnZero_whenNoScimUsersMatchesQuery() {
    int totalScimUsers = 15;
    generateScimUsers(totalScimUsers);
    ScimUserQuery scimUserQuery = ScimUserQuery.builder().userName("jean.okta").build();

    assertThat(scimUserDao.countScimUsers(dbSession, scimUserQuery)).isZero();
  }

  @Test
  void countScimUsers_shoudReturnCorrectNumberOfScimUser_whenFilteredByScimUserName() {
    insertScimUsersWithUsers(List.of("TEST_A", "TEST_B", "TEST_B_BIS", "TEST_C", "TEST_D"));
    ScimUserQuery scimUserQuery = ScimUserQuery.builder().userName("test_b").build();

    assertThat(scimUserDao.countScimUsers(dbSession, scimUserQuery)).isEqualTo(1);
  }

  private List<ScimUserTestData> generateScimUsers(int totalScimUsers) {
    List<String> userNames = IntStream.range(0, totalScimUsers)
      .mapToObj(i -> "username_" + i)
      .toList();
    return insertScimUsersWithUsers(userNames);
  }

  @Test
  void enableScimForUser_addsUserToScimUsers() {
    ScimUserDto scimUserDto = scimUserDao.enableScimForUser(dbSession, "sqUser1");

    assertThat(scimUserDto.getScimUserUuid()).isNotBlank();
    ScimUserDto actualScimUserDto = scimUserDao.findByScimUuid(dbSession, scimUserDto.getScimUserUuid()).orElseThrow();
    assertThat(scimUserDto.getScimUserUuid()).isEqualTo(actualScimUserDto.getScimUserUuid());
    assertThat(scimUserDto.getUserUuid()).isEqualTo(actualScimUserDto.getUserUuid());
  }

  static Object[][] filterData() {
    return new Object[][]{
      {"test_user", List.of("test_user", "Test_USEr", "xxx.test_user.yyy", "test_xxx_user"), List.of("1", "2")},
      {"TEST_USER", List.of("test_user", "Test_USEr", "xxx.test_user.yyy", "test_xxx_user"), List.of("1", "2")},
      {"test_user_x", List.of("test_user"), List.of()},
      {"test_x_user", List.of("test_user"), List.of()},
    };
  }

  @ParameterizedTest
  @MethodSource("filterData")
  void findScimUsers_whenFilteringByUserName_shouldReturnTheExpectedScimUsers(String search, List<String> userLogins,
    List<String> expectedScimUserUuids) {
    insertScimUsersWithUsers(userLogins);
    ScimUserQuery query = ScimUserQuery.builder().userName(search).build();

    List<ScimUserWithUsernameDto> scimUsersByQuery = scimUserDao.findScimUsers(dbSession, query, Pagination.all());

    List<String> scimUsersUuids = toScimUsersUuids(scimUsersByQuery);
    assertThat(scimUsersUuids).containsExactlyElementsOf(expectedScimUserUuids);
  }

  @Test
  void findScimUsers_whenFilteringByGroupUuid_shouldReturnTheExpectedScimUsers() {
    List<ScimUserTestData> scimUsersTestData = insertScimUsersWithUsers(List.of("userAInGroupA", "userBInGroupA", "userAInGroupB",
      "userNotInGroup"));
    Map<String, ScimUserTestData> users = scimUsersTestData.stream()
      .collect(Collectors.toMap(testData -> testData.getUserDto().getExternalId(), Function.identity()));

    GroupDto group1dto = createGroupWithUsers(users.get("userAInGroupA"), users.get("userBInGroupA"));
    createGroupWithUsers(users.get("userAInGroupB"));

    ScimUserQuery query = ScimUserQuery.builder().groupUuid(group1dto.getUuid()).build();

    List<ScimUserWithUsernameDto> scimUsers = scimUserDao.findScimUsers(dbSession, query, Pagination.all());

    List<String> scimUsersUuids = toScimUsersUuids(scimUsers);
    assertThat(scimUsersUuids).containsExactlyInAnyOrder(
      users.get("userAInGroupA").getScimUserUuid(),
      users.get("userBInGroupA").getScimUserUuid()
    );
  }

  @Test
  void findScimUsers_shouldReturnTheExpectedScimUsersWithUsername() {
    insertScimUsersWithUsers(List.of("userA", "userB"));

    ScimUserQuery query = ScimUserQuery.builder().userName("userA").build();

    List<ScimUserWithUsernameDto> scimUsers = scimUserDao.findScimUsers(dbSession, query, Pagination.all());

    List<String> scimUsersUuids = toScimUsersUuids(scimUsers);

    assertThat(scimUsers)
      .extracting(ScimUserWithUsernameDto::getScimUserUuid, ScimUserWithUsernameDto::getUserName)
      .contains(
        tuple(scimUsersUuids.get(0), "userA")
      );
  }

  private GroupDto createGroupWithUsers(ScimUserTestData... testUsers) {
    GroupDto group = db.users().insertGroup();

    UserDto[] userDtos = Arrays.stream(testUsers)
      .map(ScimUserTestData::getUserDto)
      .toArray(UserDto[]::new);
    db.users().insertMembers(group, userDtos);
    return group;
  }

  @Test
  void findScimUsers_whenFilteringByScimUuidsWithLongRange_shouldReturnTheExpectedScimUsers() {
    generateScimUsers(3000);
    Set<String> expectedScimUserUuids = generateStrings(1, 2050);

    ScimUserQuery query = ScimUserQuery.builder().scimUserUuids(expectedScimUserUuids).build();

    List<ScimUserWithUsernameDto> scimUsersByQuery = scimUserDao.findScimUsers(dbSession, query, Pagination.all());

    List<String> scimUsersUuids = toScimUsersUuids(scimUsersByQuery);
    assertThat(scimUsersByQuery)
      .hasSameSizeAs(scimUsersUuids)
      .extracting(ScimUserDto::getScimUserUuid)
      .containsAll(expectedScimUserUuids);
  }

  @Test
  void findScimUsers_whenFilteringByScimUuidsAndUserName_shouldReturnTheExpectedScimUser() {
    Set<String> scimUserUuids = generateScimUsers(10).stream()
      .map(ScimUserTestData::getScimUserUuid)
      .collect(Collectors.toSet());

    ScimUserQuery query = ScimUserQuery.builder().scimUserUuids(scimUserUuids).userName("username_5").build();

    List<ScimUserWithUsernameDto> scimUsersByQuery = scimUserDao.findScimUsers(dbSession, query, Pagination.all());

    assertThat(scimUsersByQuery).hasSize(1)
      .extracting(ScimUserWithUsernameDto::getScimUserUuid)
      .contains("6");
  }

  @Test
  void findScimUsers_whenFilteringByUserUuidsWithLongRange_shouldReturnTheExpectedScimUsers() {
    List<ScimUserTestData> scimUsersTestData = generateScimUsers(3000);
    Set<String> allUsersUuid = scimUsersTestData.stream()
      .map(ScimUserTestData::getUserUuid)
      .collect(Collectors.toSet());

    ScimUserQuery query = ScimUserQuery.builder().userUuids(allUsersUuid).build();

    List<ScimUserWithUsernameDto> scimUsersByQuery = scimUserDao.findScimUsers(dbSession, query, Pagination.all());

    assertThat(scimUsersByQuery)
      .hasSameSizeAs(allUsersUuid)
      .extracting(ScimUserWithUsernameDto::getUserUuid)
      .containsAll(allUsersUuid);
  }

  @Test
  void findScimUsers_whenFilteringByUserUuidsAndUserName_shouldReturnTheExpectedScimUser() {
    List<ScimUserTestData> scimUsersTestData = generateScimUsers(10);
    Set<String> allUsersUuid = scimUsersTestData.stream()
      .map(ScimUserTestData::getUserUuid)
      .collect(Collectors.toSet());

    ScimUserQuery query = ScimUserQuery.builder().userUuids(allUsersUuid).userName("username_5").build();

    List<ScimUserWithUsernameDto> scimUsersByQuery = scimUserDao.findScimUsers(dbSession, query, Pagination.all());

    assertThat(scimUsersByQuery).hasSize(1)
      .extracting(ScimUserWithUsernameDto::getScimUserUuid)
      .contains("6");
  }

  private static Set<String> generateStrings(int startInclusive, int endExclusive) {
    return generateStrings(startInclusive, endExclusive, "");
  }

  private static Set<String> generateStrings(int startInclusive, int endExclusive, String prefix) {
    return IntStream.range(startInclusive, endExclusive)
      .mapToObj(String::valueOf)
      .map(string -> prefix + string)
      .collect(Collectors.toSet());
  }

  @Test
  void deleteByUserUuid_shouldDeleteScimUser() {
    ScimUserTestData scimUserTestData = insertScimUser("scimUser");

    scimUserDao.deleteByUserUuid(dbSession, scimUserTestData.getUserUuid());

    assertThat(scimUserDao.findAll(dbSession)).isEmpty();
  }

  @Test
  void deleteByScimUuid_shouldDeleteScimUser() {
    ScimUserTestData scimUserTestData = insertScimUser("scimUser");
    ScimUserTestData scimUserTestData2 = insertScimUser("scimUser2");

    scimUserDao.deleteByScimUuid(dbSession, scimUserTestData.getScimUserUuid());

    List<ScimUserDto> remainingUsers = scimUserDao.findAll(dbSession);
    assertThat(remainingUsers).hasSize(1);

    ScimUserDto remainingUser = remainingUsers.get(0);
    assertThat(remainingUser.getScimUserUuid()).isEqualTo(scimUserTestData2.scimUserUuid);
    assertThat(remainingUser.getUserUuid()).isEqualTo(scimUserTestData2.userUuid);
  }

  @Test
  void deleteAll_should_remove_all_ScimUsers() {
    insertScimUser("scimUser");
    insertScimUser("scimUser2");

    scimUserDao.deleteAll(dbSession);

    assertThat(scimUserDao.findAll(dbSession)).isEmpty();
  }

  @Test
  void deleteFromUserUuid_shouldNotFail_whenNoUser() {
    assertThatCode(() -> scimUserDao.deleteByUserUuid(dbSession, secure().nextAlphanumeric(6))).doesNotThrowAnyException();
  }

  private List<ScimUserTestData> insertScimUsersWithUsers(List<String> userLogins) {
    return IntStream.range(0, userLogins.size())
      .mapToObj(i -> insertScimUserWithUser(userLogins.get(i), String.valueOf(i + 1)))
      .toList();
  }

  private ScimUserTestData insertScimUserWithUser(String userLogin, String scimUuid) {
    UserDto userDto = db.users().insertUser(u -> u.setExternalId(userLogin));
    ScimUserTestData scimUserTestData = insertScimUser(scimUuid, userDto.getUuid());
    scimUserTestData.setUserDto(userDto);
    return scimUserTestData;
  }

  private ScimUserTestData insertScimUser(String scimUserUuid) {
    return insertScimUser(scimUserUuid, secure().nextAlphanumeric(40));
  }

  private ScimUserTestData insertScimUser(String scimUserUuid, String userUuid) {
    ScimUserTestData scimUserTestData = new ScimUserTestData(scimUserUuid, userUuid);
    Map<String, Object> data = Map.of("scim_uuid", scimUserTestData.getScimUserUuid(), "user_uuid", scimUserTestData.getUserUuid());
    db.executeInsert("scim_users", data);
    return scimUserTestData;
  }

  @Test
  void getManagedUserSqlFilter_isNotEmpty() {
    String filterManagedUser = scimUserDao.getManagedUserSqlFilter(true);
    assertThat(filterManagedUser).isNotEmpty();
    String filterNonManagedUser = scimUserDao.getManagedUserSqlFilter(false);
    assertThat(filterNonManagedUser).isNotEmpty();

    assertThat(filterManagedUser).isNotEqualTo(filterNonManagedUser);
  }

  @Test
  void getManagedGroupsSqlFilter_whenFilterByManagedIsTrue_returnsCorrectQuery() {
    String filterManagedUser = scimUserDao.getManagedUserSqlFilter(true);
    assertThat(filterManagedUser).isEqualTo(" exists (select user_uuid from scim_users su where su.user_uuid = uuid)");
  }

  @Test
  void getManagedGroupsSqlFilter_whenFilterByManagedIsFalse_returnsCorrectQuery() {
    String filterNonManagedUser = scimUserDao.getManagedUserSqlFilter(false);
    assertThat(filterNonManagedUser).isEqualTo("not exists (select user_uuid from scim_users su where su.user_uuid = uuid)");
  }

  private static class ScimUserTestData {

    private final String scimUserUuid;
    private final String userUuid;
    private UserDto userDto;

    private ScimUserTestData(String scimUserUuid, String userUuid) {
      this.scimUserUuid = scimUserUuid;
      this.userUuid = userUuid;
    }

    private String getScimUserUuid() {
      return scimUserUuid;
    }

    private String getUserUuid() {
      return userUuid;
    }

    private UserDto getUserDto() {
      return userDto;
    }

    private void setUserDto(UserDto userDto) {
      this.userDto = userDto;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      ScimUserTestData that = (ScimUserTestData) o;
      return getScimUserUuid().equals(that.getScimUserUuid()) && getUserUuid().equals(that.getUserUuid());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getScimUserUuid(), getUserUuid());
    }
  }
}
