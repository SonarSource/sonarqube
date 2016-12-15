/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.platform.db.migration.history;

import com.google.common.base.Throwables;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.schemamigration.SchemaMigrationMapper;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

import static com.google.common.base.Preconditions.checkState;

public class MigrationHistoryImpl implements MigrationHistory {
  private final DbClient dbClient;

  public MigrationHistoryImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    try (Connection connection = dbClient.getDatabase().getDataSource().getConnection()) {
      checkState(DatabaseUtils.tableExists(MigrationHistoryTable.NAME, connection), "Migration history table is missing");
    } catch (SQLException e) {
      Throwables.propagate(e);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

  @Override
  public Optional<Long> getLastMigrationNumber() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<Integer> versions = getMapper(dbSession).selectVersions();

      if (!versions.isEmpty()) {
        Collections.sort(versions);
        return Optional.of(versions.get(versions.size() - 1).longValue());
      }
      return Optional.empty();
    }
  }

  @Override
  public void done(RegisteredMigrationStep dbMigration) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      getMapper(dbSession).insert(String.valueOf(dbMigration.getMigrationNumber()));
      dbSession.commit();
    }
  }

  private static SchemaMigrationMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(SchemaMigrationMapper.class);
  }
}
