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

public class CreateIndexOnIssuesDeferralDate extends DdlChange {
  static final String TABLE_NAME = "issues";
  static final String INDEX_NAME = "issues_deferral_date";
  static final String COLUMN_NAME = "deferral_date";
  static final String INCLUDE_COLUMNS = "project_uuid, kee, status";

  public CreateIndexOnIssuesDeferralDate(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      createIndex(context, connection);
    }
  }

  private void createIndex(Context context, Connection connection) {
    if (DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)) {
      return;
    }
    switch (getDialect().getId()) {
      // Partial index: only rows with a pending deferral are stored, so INCLUDE columns stay cheap.
      case PostgreSql.ID, MsSql.ID -> context.execute(
        format("CREATE INDEX %s ON %s (%s) INCLUDE (%s) WHERE %s IS NOT NULL",
          INDEX_NAME, TABLE_NAME, COLUMN_NAME, INCLUDE_COLUMNS, COLUMN_NAME));
      // Oracle B-tree omits rows where all indexed columns are NULL, so a plain single-column
      // index is already partial there. Do not add status/project_uuid as key columns - that would
      // make every row non-NULL again and defeat it. H2 does index NULL keys, so the index is not
      // partial there; H2 is only used for tests and the schema dump, so that is acceptable.
      case Oracle.ID, H2.ID -> context.execute(
        format("CREATE INDEX %s ON %s (%s)", INDEX_NAME, TABLE_NAME, COLUMN_NAME));
      default -> throw new IllegalArgumentException("Unsupported dialect id " + getDialect().getId());
    }
  }
}
