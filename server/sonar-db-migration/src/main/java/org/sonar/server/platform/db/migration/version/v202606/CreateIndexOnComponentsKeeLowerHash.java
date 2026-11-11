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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.db.DatabaseUtils.tableColumnExists;

/**
 * SQL Server has no function-based index, and {@code components.kee} is {@code NVARCHAR(1000)}, past the 1700-byte
 * index key limit. Index a 32-byte hash of {@code LOWER(kee)} so case-insensitive key lookup can seek.
 * PostgreSQL and Oracle already index {@code LOWER(kee)} directly.
 */
public class CreateIndexOnComponentsKeeLowerHash extends DdlChange {

  static final String TABLE_NAME = "components";
  static final String INDEX_NAME = "components_lower_kee";
  static final String COLUMN_NAME = "kee_lower_hash";
  static final String COMPUTED_COLUMN_SQL = "ALTER TABLE components ADD kee_lower_hash AS CONVERT(BINARY(32), HASHBYTES('SHA2_256', LOWER(kee)))";

  public CreateIndexOnComponentsKeeLowerHash(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (!MsSql.ID.equals(getDialect().getId())) {
      return;
    }
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!tableColumnExists(connection, TABLE_NAME, COLUMN_NAME)) {
        context.execute(COMPUTED_COLUMN_SQL);
      }
      if (!DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)) {
        context.execute(new CreateIndexBuilder(getDialect())
          .setTable(TABLE_NAME)
          .setName(INDEX_NAME)
          .addColumn(COLUMN_NAME)
          .build());
      }
    }
  }
}
