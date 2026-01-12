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
package org.sonar.server.platform.db.migration.version.v202601;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.lang.String.format;

public class CreateUniqueIndexOnSnapshotsRootComponentUuidIsLast extends DdlChange {

  static final String TABLE_NAME = "snapshots";
  static final String INDEX_NAME = "uniq_snapshots_root_comp_uuid_islast";
  static final String COLUMN_NAME = "root_component_uuid";
  static final String ISLAST_COLUMN_NAME = "islast";

  public CreateUniqueIndexOnSnapshotsRootComponentUuidIsLast(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      createIndex(context, connection);
    }
  }

  /**
   * Creates a partial unique index on root_component_uuid WHERE islast = true.
   * This ensures only one snapshot can be marked as "islast" per root component.
   * PostgreSQL and MSSQL support partial/filtered indexes natively.
   * Oracle uses a function-based index with CASE to achieve the same result.
   * H2 doesn't support partial indexes and a regular unique index would break ITs, so no index is created.
   */
  private void createIndex(Context context, Connection connection) {
    if (DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)) {
      return;
    }

    switch (getDialect().getId()) {
      case H2.ID -> {
        // No index is created for H2 as it doesn't support partial indexes and a regular unique index would break ITs
      }
      case PostgreSql.ID -> context.execute(format("CREATE UNIQUE INDEX %s ON %s (%s) WHERE %s = true", INDEX_NAME, TABLE_NAME, COLUMN_NAME, ISLAST_COLUMN_NAME));
      case MsSql.ID -> context.execute(format("CREATE UNIQUE INDEX %s ON %s (%s) WHERE %s = 1", INDEX_NAME, TABLE_NAME, COLUMN_NAME, ISLAST_COLUMN_NAME));
      case Oracle.ID -> context.execute(format("CREATE UNIQUE INDEX %s ON %s (CASE WHEN %s = 1 THEN %s END)", INDEX_NAME, TABLE_NAME, ISLAST_COLUMN_NAME, COLUMN_NAME));
      default -> throw new IllegalArgumentException("Unsupported dialect id " + getDialect().getId());
    }
  }
}
