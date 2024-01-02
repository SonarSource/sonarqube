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
package org.sonar.db.scim;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Fail.fail;

@RunWith(DataProviderRunner.class)
public class ScimUserDaoTest {

  @Rule
  public DbTester db = DbTester.create();
  private final DbSession dbSession = db.getSession();
  private final ScimUserDao scimUserDao = db.getDbClient().scimUserDao();

  @Test
  public void findAll_ifNoData_returnsEmptyList() {
    assertThat(scimUserDao.findAll(dbSession)).isEmpty();
  }

  @Test
  public void findAll_returnsAllEntries() {
    ScimUserTestData scimUser1TestData = insertScimUser("scimUser1");
    ScimUserTestData scimUser2TestData = insertScimUser("scimUser2");

    List<ScimUserDto> scimUserDtos = scimUserDao.findAll(dbSession);

    assertThat(scimUserDtos).hasSize(2)
      .map(scimUserDto -> new ScimUserTestData(scimUserDto.getScimUserUuid(), scimUserDto.getUserUuid()))
      .containsExactlyInAnyOrder(scimUser1TestData, scimUser2TestData);

  }

  @Test
  public void findByScimUuid_whenScimUuidNotFound_shouldReturnEmptyOptional() {
    assertThat(scimUserDao.findByScimUuid(dbSession, "unknownId")).isEmpty();
  }

  @Test
  public void findByScimUuid_whenScimUuidFound_shouldReturnDto() {
    ScimUserTestData scimUser1TestData = insertScimUser("scimUser1");
    insertScimUser("scimUser2");

    ScimUserDto scimUserDto = scimUserDao.findByScimUuid(dbSession, scimUser1TestData.getScimUserUuid())
      .orElseGet(() -> fail("User not found"));

    assertThat(scimUserDto.getScimUserUuid()).isEqualTo(scimUser1TestData.getScimUserUuid());
    assertThat(scimUserDto.getUserUuid()).isEqualTo(scimUser1TestData.getUserUuid());
  }

  @Test
  public void findByUserUuid_whenScimUuidNotFound_shouldReturnEmptyOptional() {
    assertThat(scimUserDao.findByUserUuid(dbSession, "unknownId")).isEmpty();
  }

  @Test
  public void findByUserUuid_whenScimUuidFound_shouldReturnDto() {
    ScimUserTestData scimUser1TestData = insertScimUser("scimUser1");
    insertScimUser("scimUser2");

    ScimUserDto scimUserDto = scimUserDao.findByUserUuid(dbSession, scimUser1TestData.getUserUuid())
      .orElseGet(() -> fail("User not found"));

    assertThat(scimUserDto.getScimUserUuid()).isEqualTo(scimUser1TestData.getScimUserUuid());
    assertThat(scimUserDto.getUserUuid()).isEqualTo(scimUser1TestData.getUserUuid());
  }

  @DataProvider
  public static Object[][] paginationData() {
    return new Object[][] {
      {5, 0, 20, List.of("1", "2", "3", "4", "5")},
      {9, 0, 5, List.of("1", "2", "3", "4", "5")},
      {9, 3, 3, List.of("4", "5", "6")},
      {9, 7, 3, List.of("8", "9")},
      {5, 5, 20, List.of()},
      {5, 0, 0, List.of()}
    };
  }

  @Test
  @UseDataProvider("paginationData")
  public void findScimUsers_whenPaginationAndStartIndex_shouldReturnTheCorrectNumberOfScimUsers(int totalScimUsers, int offset, int pageSize, List<String> expectedScimUserUuids) {
    generateScimUsers(totalScimUsers);

    List<ScimUserDto> scimUserDtos = scimUserDao.findScimUsers(dbSession, ScimUserQuery.empty(), offset, pageSize);

    List<String> scimUsersUuids = toScimUsersUuids(scimUserDtos);
    assertThat(scimUsersUuids).containsExactlyElementsOf(expectedScimUserUuids);
  }

  private List<String> toScimUsersUuids(Collection<ScimUserDto> scimUserDtos) {
    return scimUserDtos.stream()
      .map(ScimUserDto::getScimUserUuid)
      .collect(Collectors.toList());
  }

  @Test
  public void countScimUsers_shouldReturnTheTotalNumberOfScimUsers() {
    int totalScimUsers = 15;
    generateScimUsers(totalScimUsers);

    assertThat(scimUserDao.countScimUsers(dbSession, ScimUserQuery.empty())).isEqualTo(totalScimUsers);
  }

  @Test
  public void countScimUsers_shouldReturnZero_whenNoScimUsers() {
    assertThat(scimUserDao.countScimUsers(dbSession, ScimUserQuery.empty())).isZero();
  }

  @Test
  public void countScimUsers_shoudReturnZero_whenNoScimUsersMatchesQuery() {
    int totalScimUsers = 15;
    generateScimUsers(totalScimUsers);
    ScimUserQuery scimUserQuery = ScimUserQuery.builder().userName("jean.okta").build();

    assertThat(scimUserDao.countScimUsers(dbSession, scimUserQuery)).isZero();
  }

  @Test
  public void countScimUsers_shoudReturnCorrectNumberOfScimUser_whenFilteredByScimUserName() {
    inserScimUsersWithUsers(List.of("TEST_A", "TEST_B", "TEST_B_BIS", "TEST_C", "TEST_D"));
    ScimUserQuery scimUserQuery = ScimUserQuery.builder().userName("test_b").build();

    assertThat(scimUserDao.countScimUsers(dbSession, scimUserQuery)).isEqualTo(1);
  }

  private void generateScimUsers(int totalScimUsers) {
    List<ScimUserTestData> allScimUsers = Stream.iterate(1, i -> i + 1)
      .map(i -> insertScimUser(i.toString()))
      .limit(totalScimUsers)
      .collect(Collectors.toList());
    assertThat(allScimUsers).hasSize(totalScimUsers);
  }

  @Test
  public void enableScimForUser_addsUserToScimUsers() {
    ScimUserDto scimUserDto = scimUserDao.enableScimForUser(dbSession, "sqUser1");

    assertThat(scimUserDto.getScimUserUuid()).isNotBlank();
    ScimUserDto actualScimUserDto = scimUserDao.findByScimUuid(dbSession, scimUserDto.getScimUserUuid()).orElseThrow();
    assertThat(scimUserDto.getScimUserUuid()).isEqualTo(actualScimUserDto.getScimUserUuid());
    assertThat(scimUserDto.getUserUuid()).isEqualTo(actualScimUserDto.getUserUuid());
  }

  @DataProvider
  public static Object[][] filterData() {
    return new Object[][] {
      {"test_user", List.of("test_user", "Test_USEr", "xxx.test_user.yyy", "test_xxx_user"), List.of("1", "2")},
      {"TEST_USER", List.of("test_user", "Test_USEr", "xxx.test_user.yyy", "test_xxx_user"), List.of("1", "2")},
      {"test_user_x", List.of("test_user"), List.of()},
      {"test_x_user", List.of("test_user"), List.of()},
    };
  }

  @Test
  @UseDataProvider("filterData")
  public void findScimUsers_whenFilteringByUserName_shouldReturnTheExpectedScimUsers(String search, List<String> userLogins, List<String> expectedScimUserUuids) {
    inserScimUsersWithUsers(userLogins);
    ScimUserQuery query = ScimUserQuery.builder().userName(search).build();

    List<ScimUserDto> scimUsersByQuery = scimUserDao.findScimUsers(dbSession, query, 0, 100);

    List<String> scimUsersUuids = toScimUsersUuids(scimUsersByQuery);
    assertThat(scimUsersUuids).containsExactlyElementsOf(expectedScimUserUuids);
  }

  @Test
  public void deleteFromUserUuid_shouldDeleteScimUser() {
    ScimUserTestData scimUserTestData = insertScimUser("scimUser");

    scimUserDao.deleteByUserUuid(dbSession, scimUserTestData.getUserUuid());

    assertThat(scimUserDao.findAll(dbSession)).isEmpty();
  }

  @Test
  public void deleteFromUserUuid_shouldNotFail_whenNoUser() {
    assertThatCode(() -> scimUserDao.deleteByUserUuid(dbSession, randomAlphanumeric(6))).doesNotThrowAnyException();
  }

  private void inserScimUsersWithUsers(List<String> userLogins) {
    IntStream.range(0, userLogins.size())
      .forEachOrdered(i -> insertScimUserWithUser(userLogins.get(i), String.valueOf(i + 1)));
  }

  private void insertScimUserWithUser(String userLogin, String scimUuid) {
    UserDto userDto = db.users().insertUser(u -> u.setExternalId(userLogin));
    insertScimUser(scimUuid, userDto.getUuid());
  }

  private ScimUserTestData insertScimUser(String scimUserUuid) {
    return insertScimUser(scimUserUuid, randomAlphanumeric(40));
  }

  private ScimUserTestData insertScimUser(String scimUserUuid, String userUuid) {
    ScimUserTestData scimUserTestData = new ScimUserTestData(scimUserUuid, userUuid);
    Map<String, Object> data = Map.of("scim_uuid", scimUserTestData.getScimUserUuid(), "user_uuid", scimUserTestData.getUserUuid());
    db.executeInsert("scim_users", data);

    return scimUserTestData;
  }

  private static class ScimUserTestData {

    private final String scimUserUuid;
    private final String userUuid;

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
