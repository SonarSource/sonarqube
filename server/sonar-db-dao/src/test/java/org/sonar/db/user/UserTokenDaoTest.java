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
package org.sonar.db.user;

import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.UserTokenTesting.newUserToken;

public class UserTokenDaoTest {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbSession dbSession = db.getSession();

  private UserTokenDao underTest = db.getDbClient().userTokenDao();

  @Test
  public void insert_token() {
    UserTokenDto userToken = newUserToken();

    underTest.insert(db.getSession(), userToken);

    UserTokenDto userTokenFromDb = underTest.selectByTokenHash(db.getSession(), userToken.getTokenHash());
    assertThat(userTokenFromDb).isNotNull();
    assertThat(userTokenFromDb.getName()).isEqualTo(userToken.getName());
    assertThat(userTokenFromDb.getCreatedAt()).isEqualTo(userToken.getCreatedAt());
    assertThat(userTokenFromDb.getTokenHash()).isEqualTo(userToken.getTokenHash());
    assertThat(userTokenFromDb.getUserUuid()).isEqualTo(userToken.getUserUuid());
  }

  @Test
  public void update_last_connection_date() {
    UserDto user1 = db.users().insertUser();
    UserTokenDto userToken1 = db.users().insertToken(user1);
    UserTokenDto userToken2 = db.users().insertToken(user1);
    assertThat(underTest.selectByTokenHash(dbSession, userToken1.getTokenHash()).getLastConnectionDate()).isNull();

    underTest.update(dbSession, userToken1.setLastConnectionDate(10_000_000_000L));

    UserTokenDto userTokenReloaded = underTest.selectByTokenHash(dbSession, userToken1.getTokenHash());
    assertThat(userTokenReloaded.getLastConnectionDate()).isEqualTo(10_000_000_000L);
    assertThat(userTokenReloaded.getTokenHash()).isEqualTo(userToken1.getTokenHash());
    assertThat(userTokenReloaded.getCreatedAt()).isEqualTo(userToken1.getCreatedAt());
  }

  @Test
  public void select_by_token_hash() {
    UserDto user = db.users().insertUser();
    String tokenHash = "123456789";
    db.users().insertToken(user, t -> t.setTokenHash(tokenHash));

    UserTokenDto result = underTest.selectByTokenHash(db.getSession(), tokenHash);

    assertThat(result).isNotNull();
  }

  @Test
  public void select_by_user_and_name() {
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
  public void delete_tokens_by_user() {
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
  public void delete_token_by_user_and_name() {
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
  public void count_tokens_by_user() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user, t -> t.setName("name"));
    db.users().insertToken(user, t -> t.setName("another-name"));

    Map<String, Integer> result = underTest.countTokensByUsers(dbSession, singletonList(user));

    assertThat(result.get(user.getUuid())).isEqualTo(2);
    assertThat(result.get("unknown-user_uuid")).isNull();
  }
}
