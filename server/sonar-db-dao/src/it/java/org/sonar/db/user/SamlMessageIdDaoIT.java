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

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

class SamlMessageIdDaoIT {

  private static final long NOW = 1_000_000_000L;

  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);
  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final DbSession dbSession = db.getSession();
  private final UuidFactory uuidFactory = new SequenceUuidFactory();

  private final SamlMessageIdDao underTest = new SamlMessageIdDao(system2, uuidFactory);

  @Test
  void selectByMessageId() {
    SamlMessageIdDto dto = new SamlMessageIdDto()
      .setMessageId("ABCD")
      .setExpirationDate(15_000_000_000L);
    underTest.insert(dbSession, dto);

    Optional<SamlMessageIdDto> result = underTest.selectByMessageId(dbSession, dto.getMessageId());

    assertThat(result).isPresent();
    assertThat(result.get().getMessageId()).isEqualTo("ABCD");
    assertThat(result.get().getExpirationDate()).isEqualTo(15_000_000_000L);
    assertThat(result.get().getCreatedAt()).isEqualTo(NOW);
  }

  @Test
  void uuid_created_at_and_updated_at_are_ignored_during_insert() {
    SamlMessageIdDto dto = new SamlMessageIdDto()
      .setMessageId("ABCD")
      .setExpirationDate(15_000_000_000L)
      // Following fields should be ignored
      .setUuid("SHOULD_NOT_BE_USED")
      .setCreatedAt(8_000_000_000L);
    underTest.insert(dbSession, dto);

    Optional<SamlMessageIdDto> result = underTest.selectByMessageId(dbSession, dto.getMessageId());

    assertThat(result).isPresent();
    assertThat(result.get().getUuid()).isNotEqualTo("SHOULD_NOT_BE_USED");
    assertThat(result.get().getCreatedAt()).isEqualTo(NOW);
  }

  @Test
  void deleteExpired() {
    SamlMessageIdDto expiredSamlMessageId1 = underTest.insert(dbSession, new SamlMessageIdDto()
      .setMessageId("MESSAGE_1")
      .setExpirationDate(NOW - 2_000_000_000L));
    SamlMessageIdDto expiredSamlMessageId2 = underTest.insert(dbSession, new SamlMessageIdDto()
      .setMessageId("MESSAGE_2")
      .setExpirationDate(NOW - 2_000_000_000L));
    SamlMessageIdDto validSamlMessageId = underTest.insert(dbSession, new SamlMessageIdDto()
      .setMessageId("MESSAGE_3")
      .setExpirationDate(NOW + 1_000_000_000L));

    int result = underTest.deleteExpired(dbSession);

    assertThat(underTest.selectByMessageId(dbSession, expiredSamlMessageId1.getMessageId())).isNotPresent();
    assertThat(underTest.selectByMessageId(dbSession, expiredSamlMessageId2.getMessageId())).isNotPresent();
    assertThat(underTest.selectByMessageId(dbSession, validSamlMessageId.getMessageId())).isPresent();
    assertThat(result).isEqualTo(2);
  }
}
