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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.migrationlog.MigrationLogDto;

@ServerSide
public class DatabaseMigrationPersister implements Startable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMigrationPersister.class);

  private final UuidFactory uuidFactory;
  private final DbClient dbClient;
  private final DatabaseMigrationLoggerContext migrationHolder;

  public DatabaseMigrationPersister(UuidFactory uuidFactory, DbClient dbClient, DatabaseMigrationLoggerContext migrationHolder) {
    this.uuidFactory = uuidFactory;
    this.dbClient = dbClient;
    this.migrationHolder = migrationHolder;
  }

  @Override
  public void start() {
    persistLogsToDatabase();
  }

  private void persistLogsToDatabase() {
    try (DbSession session = dbClient.openSession(true)) {
      migrationHolder.getMigrationData().forEach(data -> {
        MigrationLogDto dto = new MigrationLogDto()
          .setUuid(uuidFactory.create())
          .setStep(data.step())
          .setDurationInMs(data.durationInMs())
          .setSuccess(data.success())
          .setStartedAt(data.startedAt())
          .setTargetVersion(data.targetVersion());
        dbClient.migrationLogDao().insert(session, dto);
      });

      session.commit();
    } catch (Exception e) {
      LOGGER.error("Failed to log database migration", e);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

}
