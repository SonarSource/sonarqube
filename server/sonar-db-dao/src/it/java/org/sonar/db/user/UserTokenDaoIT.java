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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.UserTokenTesting.newProjectAnalysisToken;
import static org.sonar.db.user.UserTokenTesting.newUserToken;

class UserTokenDaoIT {
  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();

  private final UserTokenDao underTest = db.getDbClient().userTokenDao();

  @Test
  void insert_user_token() {
    UserTokenDto userToken = newUserToken();

    underTest.insert(db.getSession(), userToken, "login");

    UserTokenDto userTokenFromDb = underTest.selectByTokenHash(db.getSession(), userToken.getTokenHash());
    assertTokenStandardFields(userToken, userTokenFromDb);
    assertThat(userTokenFromDb.getExpirationDate()).isNull();
  }

  @Test
  void insert_user_token_with_expiration_date() {
    UserTokenDto userToken = newUserToken().setExpirationDate(nextLong());

    underTest.insert(db.getSession(), userToken, "login");

    UserTokenDto userTokenFromDb = underTest.selectByTokenHash(db.getSession(), userToken.getTokenHash());
    assertTokenStandardFields(userToken, userTokenFromDb);
    assertThat(userTokenFromDb.getExpirationDate()).isEqualTo(userToken.getExpirationDate());
  }

  @Test
  void insert_project_analysis_token() {
    UserTokenDto projectAnalysisToken = newProjectAnalysisToken();
    ComponentDto project = db.components().insertPublicProject(projectAnalysisToken.getProjectUuid()).getMainBranchComponent();
    underTest.insert(db.getSession(), projectAnalysisToken, "login");

    UserTokenDto projectAnalysisTokenFromDb = underTest.selectByTokenHash(db.getSession(), projectAnalysisToken.getTokenHash());
    assertTokenStandardFields(projectAnalysisToken, projectAnalysisTokenFromDb);
    assertThat(projectAnalysisTokenFromDb.getProjectUuid()).isEqualTo(projectAnalysisToken.getProjectUuid());
    assertThat(projectAnalysisTokenFromDb.getProjectKey()).isEqualTo(project.getKey());
    assertThat(projectAnalysisTokenFromDb.getProjectName()).isEqualTo(project.name());
    assertThat(projectAnalysisTokenFromDb.getExpirationDate()).isNull();
  }

  @Test
  void select_tokens_expired_in_7_days() {
    String token1 = insertTokenExpiredInDays(0);
    String token2 = insertTokenExpiredInDays(7);
    String token3 = insertTokenExpiredInDays(14);
    List<UserTokenDto> tokenList = underTest.selectTokensExpiredInDays(dbSession, 7);
    assertThat(tokenList).hasSize(1)
      .extracting(UserTokenDto::getTokenHash)
      .containsOnly(token2);
  }

  private String insertTokenExpiredInDays(long days) {
    long expirationDate = LocalDate.now().plusDays(days).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    UserTokenDto userToken = newUserToken().setExpirationDate(expirationDate);
    underTest.insert(dbSession, userToken, "login");
    return userToken.getTokenHash();
  }

  @Test
  void update_last_connection_date() {
    UserDto user1 = db.users().insertUser();
    UserTokenDto userToken1 = db.users().insertToken(user1);
    UserTokenDto userToken2 = db.users().insertToken(user1);
    assertThat(underTest.selectByTokenHash(dbSession, userToken1.getTokenHash()).getLastConnectionDate()).isNull();

    underTest.updateWithoutAudit(dbSession, userToken1.setLastConnectionDate(10_000_000_000L));

    UserTokenDto userTokenReloaded = underTest.selectByTokenHash(dbSession, userToken1.getTokenHash());
    assertThat(userTokenReloaded.getLastConnectionDate()).isEqualTo(10_000_000_000L);
    assertThat(userTokenReloaded.getTokenHash()).isEqualTo(userToken1.getTokenHash());
    assertThat(userTokenReloaded.getCreatedAt()).isEqualTo(userToken1.getCreatedAt());
  }

  @Test
  void select_by_token_hash() {
    UserDto user = db.users().insertUser();
    String tokenHash = "123456789";
    db.users().insertToken(user, t -> t.setTokenHash(tokenHash));

    UserTokenDto result = underTest.selectByTokenHash(db.getSession(), tokenHash);

    assertThat(result).isNotNull();
  }

  @Test
  void select_by_user_and_name() {
    UserDto user = db.users().insertUser();
    UserTokenDto userToken = db.users().insertToken(user, t -> t.setName("name").setTokenHash("token"));

    UserTokenDto resultByLoginAndName = underTest.selectByUserAndName(db.getSession(), user, userToken.getName());
    assertThat(resultByLoginAndName.getUserUuid()).isEqualTo(user.getUuid());
    assertThat(resultByLoginAndName.getName()).isEqualTo(userToken.getName());
    assertThat(resultByLoginAndName.getCreatedAt()).isEqualTo(userToken.getCreatedAt());
    assertThat(resultByLoginAndName.getTokenHash()).isEqualTo(userToken.getTokenHash());

    assertThat(underTest.selectByUserAndName(db.getSession(), user, "unknown-name")).isNull();
  }

  @Test
  void delete_tokens_by_user() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.users().insertToken(user1);
    db.users().insertToken(user1);
    db.users().insertToken(user2);

    underTest.deleteByUser(dbSession, user1);
    db.commit();

    assertThat(underTest.selectByUser(dbSession, user1)).isEmpty();
    assertThat(underTest.selectByUser(dbSession, user2)).hasSize(1);
  }

  @Test
  void delete_token_by_user_and_name() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.users().insertToken(user1, t -> t.setName("name"));
    db.users().insertToken(user1, t -> t.setName("another-name"));
    db.users().insertToken(user2, t -> t.setName("name"));

    underTest.deleteByUserAndName(dbSession, user1, "name");

    assertThat(underTest.selectByUserAndName(dbSession, user1, "name")).isNull();
    assertThat(underTest.selectByUserAndName(dbSession, user1, "another-name")).isNotNull();
    assertThat(underTest.selectByUserAndName(dbSession, user2, "name")).isNotNull();
  }

  @Test
  void delete_tokens_by_projectKey() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.users().insertToken(user1, t -> t.setProjectUuid("projectUuid1"));
    db.users().insertToken(user1, t -> t.setProjectUuid("projectUuid2"));
    db.users().insertToken(user2, t -> t.setProjectUuid("projectUuid1"));

    underTest.deleteByProjectUuid(dbSession, "projectKey1", "projectUuid1");
    db.commit();

    assertThat(underTest.selectByUser(dbSession, user1)).hasSize(1);
    assertThat(underTest.selectByUser(dbSession, user2)).isEmpty();
  }

  @Test
  void count_tokens_by_user() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user, t -> t.setName("name"));
    db.users().insertToken(user, t -> t.setName("another-name"));

    Map<String, Integer> result = underTest.countTokensByUsers(dbSession, singletonList(user));

    assertThat(result).containsEntry(user.getUuid(), 2);
    assertThat(result.get("unknown-user_uuid")).isNull();
  }

  private void assertTokenStandardFields(UserTokenDto userToken, UserTokenDto userTokenFromDb) {
    assertThat(userTokenFromDb).isNotNull();
    assertThat(userTokenFromDb.getUuid()).isEqualTo(userToken.getUuid());
    assertThat(userTokenFromDb.getName()).isEqualTo(userToken.getName());
    assertThat(userTokenFromDb.getCreatedAt()).isEqualTo(userToken.getCreatedAt());
    assertThat(userTokenFromDb.getTokenHash()).isEqualTo(userToken.getTokenHash());
    assertThat(userTokenFromDb.getUserUuid()).isEqualTo(userToken.getUserUuid());
    assertThat(userTokenFromDb.getType()).isEqualTo(userToken.getType());
  }
}
