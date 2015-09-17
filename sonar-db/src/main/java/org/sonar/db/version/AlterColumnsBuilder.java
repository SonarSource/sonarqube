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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Generate SQL queries to update multiple column types from a table.
 *
 * Note that it's not possible to change the nullable state of a column (Because on Oracle, sending a query to set a column to be nullable when it's already nullable will fail).
 */
public class AlterColumnsBuilder {

  private final Dialect dialect;
  private final String tableName;
  private final List<ColumnDef> columnDefs = newArrayList();

  public AlterColumnsBuilder(Dialect dialect, String tableName) {
    this.dialect = dialect;
    this.tableName = tableName;
  }

  public AlterColumnsBuilder updateColumn(ColumnDef columnDef) {
    columnDefs.add(columnDef);
    return this;
  }

  public List<String> build() {
    if (columnDefs.isEmpty()) {
      throw new IllegalStateException("No column has been defined");
    }
    switch (dialect.getId()) {
      case PostgreSql.ID:
        return createPostgresQuery();
      case MySql.ID:
        return createMySqlQuery();
      case Oracle.ID:
        return createOracleQuery();
      default:
        return createMySqlAndH2Queries();
    }
  }

  private List<String> createPostgresQuery() {
    StringBuilder sql = new StringBuilder("ALTER TABLE " + tableName + " ");
    addColumns(sql, "ALTER COLUMN ", "TYPE ");
    return Collections.singletonList(sql.toString());
  }

  private List<String> createMySqlQuery() {
    StringBuilder sql = new StringBuilder("ALTER TABLE " + tableName + " ");
    addColumns(sql, "MODIFY COLUMN ", "");
    return Collections.singletonList(sql.toString());
  }

  private List<String> createOracleQuery() {
    StringBuilder sql = new StringBuilder("ALTER TABLE " + tableName + " ").append("MODIFY (");
    addColumns(sql, "", "");
    sql.append(")");
    return Collections.singletonList(sql.toString());
  }

  private List<String> createMySqlAndH2Queries() {
    List<String> sqls = new ArrayList<>();
    for (ColumnDef columnDef : columnDefs) {
      StringBuilder defaultQuery = new StringBuilder("ALTER TABLE " + tableName + " ");
      defaultQuery.append("ALTER COLUMN ");
      addColumn(defaultQuery, columnDef, "");
      sqls.add(defaultQuery.toString());
    }
    return sqls;
  }

  private void addColumns(StringBuilder sql, String updateKeyword, String typePrefix) {
    for (Iterator<ColumnDef> columnDefIterator = columnDefs.iterator(); columnDefIterator.hasNext();) {
      sql.append(updateKeyword);
      addColumn(sql, columnDefIterator.next(), typePrefix);
      if (columnDefIterator.hasNext()) {
        sql.append(", ");
      }
    }
  }

  private void addColumn(StringBuilder sql, ColumnDef columnDef, String typePrefix) {
    sql.append(columnDef.getName())
      .append(" ")
      .append(typePrefix)
      .append(columnDef.generateSqlType(dialect));
  }

}
