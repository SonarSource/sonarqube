/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.history;

import com.google.common.base.Throwables;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.core.util.stream.MoreCollectors.toList;

public class MigrationHistoryImpl implements MigrationHistory {
  private static final String SCHEMA_MIGRATIONS_TABLE = "schema_migrations";

  private final Database database;
  private final MigrationHistoryMeddler migrationHistoryMeddler;

  public MigrationHistoryImpl(Database database, MigrationHistoryMeddler migrationHistoryMeddler) {
    this.database = database;
    this.migrationHistoryMeddler = migrationHistoryMeddler;
  }

  @Override
  public void start() {
    try (Connection connection = database.getDataSource().getConnection()) {
      checkState(DatabaseUtils.tableExists(MigrationHistoryTable.NAME, connection), "Migration history table is missing");
      migrationHistoryMeddler.meddle(this);
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
    try (Connection connection = database.getDataSource().getConnection()) {
      List<Long> versions = selectVersions(connection);

      if (!versions.isEmpty()) {
        return Optional.of(versions.get(versions.size() - 1));
      }
      return Optional.empty();
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to read content of table " + SCHEMA_MIGRATIONS_TABLE, e);
    }
  }

  @Override
  public void done(RegisteredMigrationStep dbMigration) {
    long migrationNumber = dbMigration.getMigrationNumber();
    try (Connection connection = database.getDataSource().getConnection();
      PreparedStatement statement = connection.prepareStatement("insert into schema_migrations(version) values (?)")) {

      statement.setString(1, String.valueOf(migrationNumber));
      statement.execute();
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    } catch (SQLException e) {
      throw new IllegalStateException(String.format("Failed to insert row with value %s in table %s", migrationNumber, SCHEMA_MIGRATIONS_TABLE), e);
    }
  }

  private static List<Long> selectVersions(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery("select version from " + SCHEMA_MIGRATIONS_TABLE)) {
      List<Long> res = new ArrayList<>();
      while (resultSet.next()) {
        res.add(resultSet.getLong(1));
      }
      return res.stream()
        .sorted(Comparator.naturalOrder())
        .collect(toList());
    }
  }
}
