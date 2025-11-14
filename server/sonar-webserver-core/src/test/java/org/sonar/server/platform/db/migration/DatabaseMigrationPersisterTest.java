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
package org.sonar.server.platform.db.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.migrationlog.MigrationLogDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseMigrationPersisterTest {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final UuidFactory uuidFactory = UuidFactoryImpl.INSTANCE;

  private final DatabaseMigrationLoggerContext databaseMigrationLoggerContext = new DatabaseMigrationLoggerContext();

  private final DatabaseMigrationPersister underTest = new DatabaseMigrationPersister(uuidFactory, db.getDbClient(), databaseMigrationLoggerContext);

  @Test
  void persistLogsToDatabase() {
    databaseMigrationLoggerContext.addMigrationData("001", 45L, true, 100L, "2025.1");
    databaseMigrationLoggerContext.addMigrationData("002", 46L, false, 101L, "2025.1");
    databaseMigrationLoggerContext.addMigrationData("003", 47L, true, 102L, "2025.1");

    underTest.start();

    assertThat(db.migrationLogs().selectAll())
      .hasSize(3)
      .extracting(MigrationLogDto::getStep, MigrationLogDto::getDurationInMs, MigrationLogDto::isSuccess, MigrationLogDto::getStartedAt, MigrationLogDto::getTargetVersion)
      .containsExactlyInAnyOrder(
        tuple("001", 45L, true, 100L, "2025.1"),
        tuple("002", 46L, false, 101L, "2025.1"),
        tuple("003", 47L, true, 102L, "2025.1"));
  }

  @Test
  void persistLogsToDatabase_handlesException() {
    DbClient dbClientMock = mock(DbClient.class);
    DbSession dbSessionMock = mock(DbSession.class);
    when(dbClientMock.openSession(true)).thenReturn(dbSessionMock);
    doThrow(new RuntimeException("Simulated exception")).when(dbSessionMock).commit();

    new DatabaseMigrationPersister(uuidFactory, dbClientMock, databaseMigrationLoggerContext).start();

    assertThat(logTester.logs(Level.ERROR))
      .contains("Failed to log database migration");
  }
}
