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
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.lang.String.format;

/**
 * Index supporting {@code IssueMapper.resetFlagFromSonarQubeUpdate}, which looks for the few issues where
 * {@code from_sonarqube_update} is true and otherwise has to scan the whole {@code issues} table.
 *
 * <p>Only the rows where the flag is true are indexed. PostgreSQL and SQL Server use a partial index. Oracle has none,
 * so it uses a function-based index that yields NULL, and therefore no entry, for the other rows; the query must use
 * the same {@code CASE} expression to be served by it. H2 has neither, so no index is created.</p>
 */
public class CreateIndexOnIssuesFromSonarQubeUpdate extends DdlChange {

  public static final String ORACLE_INDEX_EXPRESSION = "CASE WHEN from_sonarqube_update = 1 THEN 1 END";

  static final String TABLE_NAME = "issues";
  static final String INDEX_NAME = "issues_from_sq_update";
  static final String COLUMN_NAME = "from_sonarqube_update";

  public CreateIndexOnIssuesFromSonarQubeUpdate(Database db) {
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
      case H2.ID -> {
        // No index is created for H2 as it supports neither partial nor function-based indexes
      }
      case PostgreSql.ID -> context.execute(format("CREATE INDEX %s ON %s (%s) WHERE %s = true", INDEX_NAME, TABLE_NAME, COLUMN_NAME, COLUMN_NAME));
      case MsSql.ID -> context.execute(format("CREATE INDEX %s ON %s (%s) WHERE %s = 1", INDEX_NAME, TABLE_NAME, COLUMN_NAME, COLUMN_NAME));
      case Oracle.ID -> context.execute(format("CREATE INDEX %s ON %s (%s)", INDEX_NAME, TABLE_NAME, ORACLE_INDEX_EXPRESSION));
      default -> throw new IllegalArgumentException("Unsupported dialect id " + getDialect().getId());
    }
  }
}
