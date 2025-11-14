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
package org.sonar.db.migrationlog;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationLogDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private final MigrationLogDao underTest = new MigrationLogDao();

  @Test
  void insert() {
    String uuid = Uuids.createFast();
    underTest.insert(dbSession, new MigrationLogDto()
      .setUuid(uuid)
      .setStep("coverage")
      .setDurationInMs(123L)
      .setSuccess(true)
      .setStartedAt(1_000L)
      .setTargetVersion("2025.1"));

    List<MigrationLogDto> result = underTest.selectAll(dbSession);
    assertThat(result).hasSize(1);

    MigrationLogDto dto = result.get(0);
    assertThat(dto.getUuid()).isEqualTo(uuid);
    assertThat(dto.getStep()).isEqualTo("coverage");
    assertThat(dto.getDurationInMs()).isEqualTo(123L);
    assertThat(dto.isSuccess()).isTrue();
    assertThat(dto.getStartedAt()).isEqualTo(1_000L);
    assertThat(dto.getTargetVersion()).isEqualTo("2025.1");
  }

  @Test
  void selectAll() {
    List<MigrationLogDto> result = underTest.selectAll(dbSession);
    assertThat(result).isEmpty();

    String uuid1 = Uuids.createFast();
    String uuid2 = Uuids.createFast();
    underTest.insert(dbSession, new MigrationLogDto().setUuid(uuid1).setStep("coverage").setDurationInMs(123L).setSuccess(true).setStartedAt(1_000L).setTargetVersion("2025.1"));
    underTest.insert(dbSession, new MigrationLogDto().setUuid(uuid2).setStep("coverage").setDurationInMs(456L).setSuccess(false).setStartedAt(2_000L).setTargetVersion("2025.2"));

    result = underTest.selectAll(dbSession);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getUuid()).isEqualTo(uuid1);
    assertThat(result.get(1).getUuid()).isEqualTo(uuid2);
  }

}
