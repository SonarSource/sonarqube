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
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.db.DatabaseUtils.tableColumnExists;

/**
 * Uses a generated uppercase name because SQL Server does not support function-based indexes. This makes the unique
 * constraint case-insensitive on every supported database, independently of the database collation.
 */
public class EnforceUniquePermissionTemplateNames extends DdlChange {

  static final String TABLE_NAME = "permission_templates";
  static final String UPPER_CASE_NAME_COLUMN = "name_upper";
  static final String INDEX_NAME = "uniq_perm_templates_name";

  public EnforceUniquePermissionTemplateNames(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!tableColumnExists(connection, TABLE_NAME, UPPER_CASE_NAME_COLUMN)) {
        context.execute(createGeneratedColumnSql());
      }
      if (!DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)) {
        context.execute(new CreateIndexBuilder(getDialect())
          .setTable(TABLE_NAME)
          .setName(INDEX_NAME)
          .setUnique(true)
          .addColumn(UPPER_CASE_NAME_COLUMN, false)
          .build());
      }
    }
  }

  private String createGeneratedColumnSql() {
    return createGeneratedColumnSql(getDialect().getId());
  }

  static String createGeneratedColumnSql(String dialectId) {
    return switch (dialectId) {
      case H2.ID -> "ALTER TABLE permission_templates ADD name_upper VARCHAR(200) GENERATED ALWAYS AS (UPPER(name))";
      case PostgreSql.ID -> "ALTER TABLE permission_templates ADD name_upper VARCHAR(200) GENERATED ALWAYS AS (UPPER(name)) STORED";
      case Oracle.ID -> "ALTER TABLE permission_templates ADD name_upper VARCHAR2(100 CHAR) GENERATED ALWAYS AS (UPPER(name)) VIRTUAL";
      case MsSql.ID -> "ALTER TABLE permission_templates ADD name_upper AS UPPER(name)";
      default -> throw new IllegalArgumentException("Unsupported dialect id " + dialectId);
    };
  }
}
