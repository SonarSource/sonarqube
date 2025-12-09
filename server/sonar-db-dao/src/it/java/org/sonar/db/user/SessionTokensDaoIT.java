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
package org.sonar.db.user;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTokensDaoIT {

  private static final long NOW = 1_000_000_000L;

  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);
  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final DbSession dbSession = db.getSession();
  private final UuidFactory uuidFactory = new SequenceUuidFactory();

  private final SessionTokensDao underTest = new SessionTokensDao(system2, uuidFactory);

  @Test
  void selectByUuid() {
    SessionTokenDto dto = new SessionTokenDto()
      .setUserUuid("ABCD")
      .setExpirationDate(15_000_000_000L);
    underTest.insert(dbSession, dto);

    Optional<SessionTokenDto> result = underTest.selectByUuid(dbSession, dto.getUuid());

    assertThat(result).isPresent();
    assertThat(result.get().getUserUuid()).isEqualTo("ABCD");
    assertThat(result.get().getExpirationDate()).isEqualTo(15_000_000_000L);
    assertThat(result.get().getCreatedAt()).isEqualTo(NOW);
    assertThat(result.get().getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  void uuid_created_at_and_updated_at_are_ignored_during_insert() {
    SessionTokenDto dto = new SessionTokenDto()
      .setUserUuid("ABCD")
      .setExpirationDate(15_000_000_000L)
      // Following fields should be ignored
      .setUuid("SHOULD_NOT_BE_USED")
      .setCreatedAt(8_000_000_000L)
      .setUpdatedAt(9_000_000_000L);
    underTest.insert(dbSession, dto);

    Optional<SessionTokenDto> result = underTest.selectByUuid(dbSession, dto.getUuid());

    assertThat(result).isPresent();
    assertThat(result.get().getUuid()).isNotEqualTo("SHOULD_NOT_BE_USED");
    assertThat(result.get().getCreatedAt()).isEqualTo(NOW);
    assertThat(result.get().getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  void update() {
    SessionTokenDto dto = new SessionTokenDto()
      .setUserUuid("ABCD")
      .setExpirationDate(15_000_000_000L);
    underTest.insert(dbSession, dto);
    system2.setNow(NOW + 10_000_000_000L);
    underTest.update(dbSession, dto
      .setExpirationDate(45_000_000_000L));

    Optional<SessionTokenDto> result = underTest.selectByUuid(dbSession, dto.getUuid());

    assertThat(result.get().getExpirationDate()).isEqualTo(45_000_000_000L);
    assertThat(result.get().getCreatedAt()).isEqualTo(NOW);
    assertThat(result.get().getUpdatedAt()).isEqualTo(NOW + 10_000_000_000L);
  }

  @Test
  void only_update_fields_that_makes_sense() {
    SessionTokenDto dto = new SessionTokenDto()
      .setUserUuid("ABCD")
      .setExpirationDate(15_000_000_000L);
    underTest.insert(dbSession, dto);
    system2.setNow(NOW + 10_000_000_000L);
    underTest.update(dbSession, dto
      .setExpirationDate(45_000_000_000L)
      // Following fields are ignored
      .setUserUuid("ANOTHER USER UUID")
      .setCreatedAt(NOW - 10_000_000_000L)
    );

    Optional<SessionTokenDto> result = underTest.selectByUuid(dbSession, dto.getUuid());

    assertThat(result.get().getExpirationDate()).isEqualTo(45_000_000_000L);
    assertThat(result.get().getUserUuid()).isEqualTo("ABCD");
    assertThat(result.get().getCreatedAt()).isEqualTo(NOW);
    assertThat(result.get().getUpdatedAt()).isEqualTo(NOW + 10_000_000_000L);
  }

  @Test
  void deleteByUuid() {
    UserDto user = db.users().insertUser();
    SessionTokenDto sessionToken1 = db.users().insertSessionToken(user);
    SessionTokenDto sessionToken2 = db.users().insertSessionToken(user);
    UserDto anotherUser = db.users().insertUser();
    SessionTokenDto anotherSessionToken = db.users().insertSessionToken(anotherUser);

    underTest.deleteByUuid(dbSession, sessionToken1.getUuid());

    assertThat(underTest.selectByUuid(dbSession, sessionToken1.getUuid())).isNotPresent();
    assertThat(underTest.selectByUuid(dbSession, sessionToken2.getUuid())).isPresent();
    assertThat(underTest.selectByUuid(dbSession, anotherSessionToken.getUuid())).isPresent();
  }

  @Test
  void deleteByUser() {
    UserDto user = db.users().insertUser();
    SessionTokenDto sessionToken = db.users().insertSessionToken(user);
    // Creation another session token linked on another user, it should not be removed
    UserDto anotherUser = db.users().insertUser();
    SessionTokenDto anotherSessionToken = db.users().insertSessionToken(anotherUser);

    underTest.deleteByUser(dbSession, user);

    assertThat(underTest.selectByUuid(dbSession, sessionToken.getUuid())).isNotPresent();
    assertThat(underTest.selectByUuid(dbSession, anotherSessionToken.getUuid())).isPresent();
  }

  @Test
  void deleteExpired() {
    UserDto user = db.users().insertUser();
    SessionTokenDto expiredSessionToken1 = db.users().insertSessionToken(user, st -> st.setExpirationDate(NOW - 1_000_000_000L));
    SessionTokenDto expiredSessionToken2 = db.users().insertSessionToken(user, st -> st.setExpirationDate(NOW - 1_000_000_000L));
    SessionTokenDto validSessionToken = db.users().insertSessionToken(user);

    int result = underTest.deleteExpired(dbSession);

    assertThat(underTest.selectByUuid(dbSession, expiredSessionToken1.getUuid())).isNotPresent();
    assertThat(underTest.selectByUuid(dbSession, expiredSessionToken2.getUuid())).isNotPresent();
    assertThat(underTest.selectByUuid(dbSession, validSessionToken.getUuid())).isPresent();
    assertThat(result).isEqualTo(2);
  }
}
