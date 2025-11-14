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
package org.sonar.server.platform.db.migration.version.v202503;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.MigrationStep;

public class MigrateRemoveNonCanonicalScaEncounteredLicenses implements MigrationStep {
  static final String SELECT_BATCH_QUERY = """
    SELECT
      uuid
    FROM sca_encountered_licenses
    WHERE
      license_policy_id not like 'LicenseRef%'
      and license_policy_id like '%-with-%'
    """;

  static final String DELETE_BATCH_ENCOUNTERED_LICENSES = """
    DELETE FROM sca_encountered_licenses WHERE uuid IN (?)
    """;

  private final Database db;

  public MigrateRemoveNonCanonicalScaEncounteredLicenses(Database db) {
    this.db = db;
  }

  private static List<String> findBatchOfNonCanonical(Connection connection) throws SQLException {
    List<String> results = new ArrayList<>();

    try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BATCH_QUERY)) {
      preparedStatement.setMaxRows(999);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        while (resultSet.next()) {
          results.add(resultSet.getString(1));
        }
      }
    }

    return results;
  }

  private static void deleteBatch(Connection connection, List<String> nonCanonicalRowUuids) throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement(DELETE_BATCH_ENCOUNTERED_LICENSES)) {
      for (String uuid : nonCanonicalRowUuids) {
        preparedStatement.setString(1, uuid);
        preparedStatement.addBatch();
      }
      preparedStatement.executeBatch();
    }
  }

  private static void deleteBatchOfNonCanonical(Connection connection, List<String> nonCanonicalRowUuids) throws SQLException {
    deleteBatch(connection, nonCanonicalRowUuids);
  }

  @Override
  public void execute() throws SQLException {
    try (var connection = db.getDataSource().getConnection()) {
      List<String> nonCanonicalRowIds = findBatchOfNonCanonical(connection);
      while (!nonCanonicalRowIds.isEmpty()) {
        deleteBatchOfNonCanonical(connection, nonCanonicalRowIds);
        nonCanonicalRowIds = findBatchOfNonCanonical(connection);
      }
    }
  }
}
