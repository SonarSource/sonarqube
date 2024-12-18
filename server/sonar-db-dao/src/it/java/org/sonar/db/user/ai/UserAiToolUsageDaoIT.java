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
package org.sonar.db.user.ai;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class UserAiToolUsageDaoIT {
  @RegisterExtension
  private final DbTester db = DbTester.create();

  private final DbSession dbSession = db.getSession();

  private final UserAiToolUsageDao underTest = db.getDbClient().userAiToolUsageDao();

  @Test
  void insert_shouldSaveSingleEntry() {
    UserAiToolUsageDto dto = new UserAiToolUsageDto()
      .setUuid("uuid")
      .setUserUuid("userUuid")
      .setActivatedAt(1L)
      .setLastActivityAt(2L);

    underTest.insert(dbSession, dto);

    assertThat(underTest.selectAll(dbSession))
      .extracting(UserAiToolUsageDto::getUuid, UserAiToolUsageDto::getUserUuid, UserAiToolUsageDto::getActivatedAt, UserAiToolUsageDto::getLastActivityAt)
      .containsExactly(tuple("uuid", "userUuid", 1L, 2L));
  }

  @Test
  void insert_shouldSaveMultipleEntry() {
    IntStream.range(0, 10).forEach(i -> {
      UserAiToolUsageDto dto = new UserAiToolUsageDto()
        .setUuid("uuid" + i)
        .setUserUuid("userUuid" + i)
        .setActivatedAt(1L + i)
        .setLastActivityAt(2L + i);
      underTest.insert(dbSession, dto);
    });

    assertThat(underTest.selectAll(dbSession))
      .hasSize(10);
  }

  @Test
  void select_whenLastActivityIsMissing_shouldReturnLastActivityEmpty() {
    UserAiToolUsageDto dto = new UserAiToolUsageDto()
      .setUuid("uuid")
      .setUserUuid("userUuid")
      .setActivatedAt(1L);

    underTest.insert(dbSession, dto);

    assertThat(underTest.selectAll(dbSession))
      .extracting(UserAiToolUsageDto::getUuid, UserAiToolUsageDto::getUserUuid, UserAiToolUsageDto::getActivatedAt, UserAiToolUsageDto::getLastActivityAt)
      .containsExactly(tuple("uuid", "userUuid", 1L, null));
  }
}
