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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class CreateIndexOnComponentsLowerKee extends DdlChange {

  static final String TABLE_NAME = "components";
  static final String INDEX_NAME = "components_lower_kee";
  static final String COLUMN_NAME = "kee";

  public CreateIndexOnComponentsLowerKee(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      createIndex(context, connection);
    }
  }

  /**
   * Creates a function-based index on LOWER(kee) for PostgreSQL and Oracle.
   * MSSQL is not supported yet and will be handled in a future migration.
   * H2 creates a regular index for test compatibility.
   */
  private void createIndex(Context context, Connection connection) {
    if (DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)) {
      return;
    }

    String dialectId = getDialect().getId();
    if (H2.ID.equals(dialectId)) {
      // H2 doesn't support function-based indexes; just create regular index for tests only
      context.execute("CREATE INDEX " + INDEX_NAME + " ON " + TABLE_NAME + " (" + COLUMN_NAME + ")");
    } else if (PostgreSql.ID.equals(dialectId) || Oracle.ID.equals(dialectId)) {
      context.execute("CREATE INDEX " + INDEX_NAME + " ON " + TABLE_NAME + " (LOWER(" + COLUMN_NAME + "))");
    }
  }
}
