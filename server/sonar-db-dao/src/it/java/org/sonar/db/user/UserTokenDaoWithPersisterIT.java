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
package org.sonar.db.user;

import java.security.SecureRandom;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserTokenNewValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonar.db.user.UserTokenTesting.newUserToken;

class UserTokenDaoWithPersisterIT {

  private final Random random = new SecureRandom();

  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final ArgumentCaptor<UserTokenNewValue> newValueCaptor = ArgumentCaptor.forClass(UserTokenNewValue.class);

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE, auditPersister);

  private final DbSession dbSession = db.getSession();
  private final DbClient dbClient = db.getDbClient();
  private final UserTokenDao underTest = dbClient.userTokenDao();

  @Test
  void insert_token_is_persisted() {
    UserTokenDto userToken = newUserToken()
      .setExpirationDate(random.nextLong(Long.MAX_VALUE))
      .setLastConnectionDate(random.nextLong(Long.MAX_VALUE));
    underTest.insert(db.getSession(), userToken, "login");

    verify(auditPersister).addUserToken(eq(db.getSession()), newValueCaptor.capture());
    UserTokenDto userTokenFromDb = underTest.selectByTokenHash(db.getSession(), userToken.getTokenHash());
    assertThat(userTokenFromDb).isNotNull();
    assertThat(userTokenFromDb.getUuid()).isEqualTo(userToken.getUuid());
    assertThat(userTokenFromDb.getName()).isEqualTo(userToken.getName());
    assertThat(userTokenFromDb.getCreatedAt()).isEqualTo(userToken.getCreatedAt());
    assertThat(userTokenFromDb.getTokenHash()).isEqualTo(userToken.getTokenHash());
    assertThat(userTokenFromDb.getUserUuid()).isEqualTo(userToken.getUserUuid());
    assertThat(userTokenFromDb.getType()).isEqualTo(userToken.getType());
    assertThat(userTokenFromDb.getExpirationDate()).isEqualTo(userToken.getExpirationDate());
    UserTokenNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(UserTokenNewValue::getTokenUuid, UserTokenNewValue::getTokenName, UserTokenNewValue::getUserUuid,
        UserTokenNewValue::getLastConnectionDate,
        UserTokenNewValue::getProjectKey, UserTokenNewValue::getType)
      .containsExactly(userToken.getUuid(), userToken.getName(), userToken.getUserUuid(), userToken.getLastConnectionDate(),
        userToken.getProjectKey(), userToken.getType());
    assertThat(newValue.toString())
      .contains("tokenUuid")
      .contains(DateUtils.formatDateTime(userToken.getLastConnectionDate()));
  }

  @Test
  void update_token_is_persisted() {
    UserDto user1 = db.users().insertUser();
    UserTokenDto userToken1 = db.users().insertToken(user1);

    assertThat(underTest.selectByTokenHash(dbSession, userToken1.getTokenHash()).getLastConnectionDate()).isNull();

    underTest.updateWithoutAudit(dbSession, userToken1.setLastConnectionDate(10_000_000_000L));
    underTest.update(dbSession, userToken1.setName("new_name"), user1.getLogin());

    verify(auditPersister).updateUserToken(eq(db.getSession()), newValueCaptor.capture());
    assertThat(newValueCaptor.getValue())
      .extracting(UserTokenNewValue::getTokenUuid, UserTokenNewValue::getTokenName, UserTokenNewValue::getUserUuid,
        UserTokenNewValue::getLastConnectionDate)
      .containsExactly(userToken1.getUuid(), "new_name", userToken1.getUserUuid(), userToken1.getLastConnectionDate());
  }

  @Test
  void delete_tokens_by_user_is_persisted() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.users().insertToken(user1);
    db.users().insertToken(user1);
    db.users().insertToken(user2);
    underTest.deleteByUser(dbSession, user1);
    db.commit();

    assertThat(underTest.selectByUser(dbSession, user1)).isEmpty();
    assertThat(underTest.selectByUser(dbSession, user2)).hasSize(1);
    verify(auditPersister).deleteUserToken(eq(db.getSession()), newValueCaptor.capture());
    assertThat(newValueCaptor.getValue())
      .extracting(UserTokenNewValue::getUserUuid, UserTokenNewValue::getUserLogin)
      .containsExactly(user1.getUuid(), user1.getLogin());
  }

  @Test
  void delete_tokens_by_user_without_affected_rows_is_not_persisted() {
    UserDto user1 = db.users().insertUser();

    underTest.deleteByUser(dbSession, user1);

    verify(auditPersister).addUser(any(), any());
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  void delete_token_by_user_and_name_is_persisted() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.users().insertToken(user1, t -> t.setName("name"));
    db.users().insertToken(user1, t -> t.setName("another-name"));
    db.users().insertToken(user2, t -> t.setName("name"));
    underTest.deleteByUserAndName(dbSession, user1, "name");

    assertThat(underTest.selectByUserAndName(dbSession, user1, "name")).isNull();
    assertThat(underTest.selectByUserAndName(dbSession, user1, "another-name")).isNotNull();
    assertThat(underTest.selectByUserAndName(dbSession, user2, "name")).isNotNull();
    verify(auditPersister).deleteUserToken(eq(db.getSession()), newValueCaptor.capture());
    assertThat(newValueCaptor.getValue())
      .extracting(UserTokenNewValue::getUserUuid, UserTokenNewValue::getUserLogin, UserTokenNewValue::getTokenName)
      .containsExactly(user1.getUuid(), user1.getLogin(), "name");
  }

  @Test
  void delete_token_by_user_and_name_without_affected_rows_is_not_persisted() {
    UserDto user1 = db.users().insertUser();

    underTest.deleteByUserAndName(dbSession, user1, "name");

    verify(auditPersister).addUser(any(), any());
    verifyNoMoreInteractions(auditPersister);
  }


  @Test
  void delete_token_by_projectKey_is_persisted() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.users().insertToken(user1, t -> t.setProjectUuid("projectToDelete"));
    db.users().insertToken(user1, t -> t.setProjectUuid("projectToKeep"));
    db.users().insertToken(user2, t -> t.setProjectUuid("projectToDelete"));

    underTest.deleteByProjectUuid(dbSession, "projectToDeleteKey", "projectToDelete");

    assertThat(underTest.selectByUser(dbSession, user1)).hasSize(1);
    assertThat(underTest.selectByUser(dbSession, user2)).isEmpty();
    verify(auditPersister).deleteUserToken(eq(db.getSession()), newValueCaptor.capture());
    assertThat(newValueCaptor.getValue())
      .extracting(UserTokenNewValue::getProjectKey, UserTokenNewValue::getType)
      .containsExactly("projectToDeleteKey", "PROJECT_ANALYSIS_TOKEN");
  }

  @Test
  void delete_token_by_projectKey_without_affected_rows_is_not_persisted() {
    UserDto user1 = db.users().insertUser();

    db.users().insertToken(user1, t -> t.setProjectKey("projectToKeep"));

    underTest.deleteByProjectUuid(dbSession, "projectToDeleteKey", "projectToDelete");

    assertThat(underTest.selectByUser(dbSession, user1)).hasSize(1);

    verify(auditPersister).addUser(any(), any());
    verify(auditPersister).addUserToken(any(), any());
    verifyNoMoreInteractions(auditPersister);
  }
}
