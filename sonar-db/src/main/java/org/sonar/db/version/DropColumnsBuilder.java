/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.version;

import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

/**
 * Generate a SQL query to drop multiple columns from a table
 */
public class DropColumnsBuilder {

  private final Dialect dialect;
  private final String tableName;
  private final String[] columns;

  public DropColumnsBuilder(Dialect dialect, String tableName, String... columns) {
    this.tableName = tableName;
    this.dialect = dialect;
    this.columns = columns;
  }

  public String build() {
    StringBuilder sql = new StringBuilder().append("ALTER TABLE ").append(tableName).append(" ");
    switch (dialect.getId()) {
      case PostgreSql.ID:
      case MySql.ID:
        dropColumns(sql, "DROP COLUMN ");
        break;
      case MsSql.ID:
        sql.append("DROP COLUMN ");
        dropColumns(sql, "");
        break;
      case Oracle.ID:
        sql.append("DROP (");
        dropColumns(sql, "");
        sql.append(")");
        break;
      default:
        throw new IllegalStateException(String.format("Unsupported database '%s'", dialect.getId()));
    }
    return sql.toString();
  }

  private void dropColumns(StringBuilder sql, String columnPrefix) {
    for (int i = 0; i < columns.length; i++) {
      sql.append(columnPrefix);
      sql.append(columns[i]);
      if (i < columns.length - 1) {
        sql.append(", ");
      }
    }
  }

}
