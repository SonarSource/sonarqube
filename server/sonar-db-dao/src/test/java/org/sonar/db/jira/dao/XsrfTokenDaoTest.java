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
package org.sonar.db.jira.dao;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.jira.dto.XsrfTokenDto;

import static org.assertj.core.api.Assertions.assertThat;

class XsrfTokenDaoTest {

  private static final String TOKEN_ID = UUID.randomUUID().toString();
  private static final String TOKEN_ID_2 = UUID.randomUUID().toString();
  private static final String USER_ID = UUID.randomUUID().toString();
  private static final String USER_ID_2 = UUID.randomUUID().toString();
  private static final XsrfTokenDto XSRF_TOKEN = new XsrfTokenDto().setId(TOKEN_ID).setUserUuid(USER_ID);

  private final System2 system2 = new TestSystem2().setNow(1000L);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final DbSession dbSession = db.getSession();
  private final XsrfTokenDao underTest = db.getDbClient().xsrfTokenDao();

  @Test
  void insert_shouldPersistToken() {
    var result = underTest.insert(dbSession, XSRF_TOKEN);

    assertThat(result.getId()).isEqualTo(TOKEN_ID);
    assertThat(result.getUserUuid()).isEqualTo(USER_ID);
    assertThat(result.getCreatedAt()).isEqualTo(1000L);
    assertThat(result.getUpdatedAt()).isEqualTo(1000L);
  }

  @Test
  void selectByIdAndUserUuid_shouldReturnToken_whenExists() {
    underTest.insert(dbSession, XSRF_TOKEN);

    var result = underTest.selectByIdAndUserUuid(dbSession, TOKEN_ID, USER_ID);

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(TOKEN_ID);
    assertThat(result.get().getUserUuid()).isEqualTo(USER_ID);
  }

  @Test
  void selectByIdAndUserUuid_shouldReturnEmpty_whenTokenDoesNotExist() {
    var result = underTest.selectByIdAndUserUuid(dbSession, TOKEN_ID, USER_ID);

    assertThat(result).isEmpty();
  }

  @Test
  void selectByIdAndUserUuid_shouldReturnEmpty_whenUserUuidDoesNotMatch() {
    underTest.insert(dbSession, XSRF_TOKEN);

    var result = underTest.selectByIdAndUserUuid(dbSession, TOKEN_ID, USER_ID_2);

    assertThat(result).isEmpty();
  }

  @Test
  void deleteByUserUuid_shouldDeleteToken() {
    underTest.insert(dbSession, new XsrfTokenDto().setId(TOKEN_ID).setUserUuid(USER_ID));
    underTest.insert(dbSession, new XsrfTokenDto().setId(TOKEN_ID_2).setUserUuid(USER_ID));

    var deleted = underTest.deleteByUserUuid(dbSession, USER_ID);

    assertThat(deleted).isEqualTo(2);
    assertThat(underTest.selectByIdAndUserUuid(dbSession, TOKEN_ID, USER_ID)).isEmpty();
    assertThat(underTest.selectByIdAndUserUuid(dbSession, TOKEN_ID_2, USER_ID)).isEmpty();
  }

  @Test
  void deleteByUserUuid_shouldReturnZero_whenNoTokensExist() {
    var deleted = underTest.deleteByUserUuid(dbSession, USER_ID);

    assertThat(deleted).isZero();
  }

  @Test
  void deleteByUserUuid_shouldOnlyDeleteTokensForSpecificUser() {
    underTest.insert(dbSession, new XsrfTokenDto().setId(TOKEN_ID).setUserUuid(USER_ID));
    underTest.insert(dbSession, new XsrfTokenDto().setId(TOKEN_ID_2).setUserUuid(USER_ID_2));

    var deleted = underTest.deleteByUserUuid(dbSession, USER_ID);

    assertThat(deleted).isEqualTo(1);
    assertThat(underTest.selectByIdAndUserUuid(dbSession, TOKEN_ID, USER_ID)).isEmpty();
    assertThat(underTest.selectByIdAndUserUuid(dbSession, TOKEN_ID_2, USER_ID_2)).isPresent();
  }
}

