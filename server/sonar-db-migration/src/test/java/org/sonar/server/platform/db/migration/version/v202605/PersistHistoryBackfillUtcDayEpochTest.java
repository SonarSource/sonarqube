/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

class PersistHistoryBackfillUtcDayEpochTest {

  private static final long FIRST_NOW = Instant.parse("2026-09-09T23:59:59Z").toEpochMilli();
  private static final long SECOND_NOW = Instant.parse("2026-09-10T00:00:01Z").toEpochMilli();
  private static final long FIRST_DAY = Instant.parse("2026-09-09T00:00:00Z").toEpochMilli();

  private final TestSystem2 system2 = new TestSystem2().setNow(FIRST_NOW);

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PersistHistoryBackfillUtcDayEpoch.class);

  private final DataChange underTest = new PersistHistoryBackfillUtcDayEpoch(db.database(), system2);

  @Test
  void execute_persistsTheFirstUtcDayAndKeepsItAfterMidnight() throws SQLException {
    underTest.execute();
    system2.setNow(SECOND_NOW);
    underTest.execute();

    assertThat(db.select("select text_value from internal_properties where kee = '" + PersistHistoryBackfillUtcDayEpoch.FROZEN_EPOCH_PROPERTY + "'"))
      .extracting(row -> row.get("TEXT_VALUE"))
      .containsExactly(Long.toString(FIRST_DAY));
  }
}
