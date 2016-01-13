/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version;

import java.util.List;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.PostgreSql;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Generate a SQL query to add multiple columns on a table
 */
public class AddColumnsBuilder {

  private final Dialect dialect;
  private final String tableName;
  private List<ColumnDef> columnDefs = newArrayList();

  public AddColumnsBuilder(Dialect dialect, String tableName) {
    this.tableName = tableName;
    this.dialect = dialect;
  }

  public AddColumnsBuilder addColumn(ColumnDef columnDef) {
    columnDefs.add(columnDef);
    return this;
  }

  public String build() {
    if (columnDefs.isEmpty()) {
      throw new IllegalStateException("No column has been defined");
    }

    StringBuilder sql = new StringBuilder().append("ALTER TABLE ").append(tableName).append(" ");
    switch (dialect.getId()) {
      case PostgreSql.ID:
        addColumns(sql, "ADD COLUMN ");
        break;
      case MsSql.ID:
        sql.append("ADD ");
        addColumns(sql, "");
        break;
      default:
        sql.append("ADD (");
        addColumns(sql, "");
        sql.append(")");
    }
    return sql.toString();
  }

  private void addColumns(StringBuilder sql, String columnPrefix) {
    for (int i = 0; i < columnDefs.size(); i++) {
      sql.append(columnPrefix);
      addColumn(sql, columnDefs.get(i));
      if (i < columnDefs.size() - 1) {
        sql.append(", ");
      }
    }
  }

  private void addColumn(StringBuilder sql, ColumnDef columnDef) {
    sql.append(columnDef.getName()).append(" ").append(columnDef.generateSqlType(dialect));
    sql.append(columnDef.isNullable() ? " NULL" : " NOT NULL");
  }

}
