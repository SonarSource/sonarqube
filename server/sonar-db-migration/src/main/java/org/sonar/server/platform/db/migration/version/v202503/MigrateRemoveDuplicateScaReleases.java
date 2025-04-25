/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

public class MigrateRemoveDuplicateScaReleases implements MigrationStep {
  static final String SELECT_BATCH_QUERY = """
    WITH duplicate_releases AS (
        SELECT
            uuid,
            ROW_NUMBER() OVER (
                PARTITION BY component_uuid, package_url
                ORDER BY created_at ASC
            ) AS row_num
        FROM sca_releases
    )
    SELECT
        uuid
    FROM duplicate_releases
    WHERE row_num > 1
    """;

  static final String DELETE_BATCH_DEPENDENCIES_QUERY = """
    DELETE FROM sca_dependencies WHERE sca_release_uuid IN (?)
    """;

  static final String DELETE_BATCH_ISSUES_RELEASES_CHANGES_QUERY = """
    DELETE FROM sca_issue_rels_changes WHERE sca_issues_releases_uuid IN (SELECT uuid FROM sca_issues_releases WHERE sca_release_uuid IN (?))
    """;

  static final String DELETE_BATCH_ISSUES_RELEASES_QUERY = """
    DELETE FROM sca_issues_releases WHERE sca_release_uuid IN (?)
    """;

  static final String DELETE_BATCH_RELEASES_QUERY = """
    DELETE FROM sca_releases WHERE uuid IN (?)
    """;

  private final Database db;

  public MigrateRemoveDuplicateScaReleases(Database db) {
    this.db = db;
  }

  private static List<String> findBatchOfDuplicates(Connection connection) throws SQLException {
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

  private static void deleteBatch(Connection connection, String batchSql, List<String> duplicateReleaseUuids) throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement(batchSql)) {
      for (String uuid : duplicateReleaseUuids) {
        preparedStatement.setString(1, uuid);
        preparedStatement.addBatch();
      }
      preparedStatement.executeBatch();
    }
  }

  private static void deleteBatchOfDuplicates(Connection connection, List<String> duplicateRowUuids) throws SQLException {
    deleteBatch(connection, DELETE_BATCH_DEPENDENCIES_QUERY, duplicateRowUuids);
    deleteBatch(connection, DELETE_BATCH_ISSUES_RELEASES_CHANGES_QUERY, duplicateRowUuids);
    deleteBatch(connection, DELETE_BATCH_ISSUES_RELEASES_QUERY, duplicateRowUuids);
    deleteBatch(connection, DELETE_BATCH_RELEASES_QUERY, duplicateRowUuids);
  }

  @Override
  public void execute() throws SQLException {
    try (var connection = db.getDataSource().getConnection()) {
      List<String> duplicateRowUuids = findBatchOfDuplicates(connection);
      while (!duplicateRowUuids.isEmpty()) {
        deleteBatchOfDuplicates(connection, duplicateRowUuids);
        connection.commit();
        duplicateRowUuids = findBatchOfDuplicates(connection);
      }
    }
  }
}
