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
 * This class should only be used to change the type of a column, as the nullable state is only used to not loose the nullable  information.
 * Indeed, on MsSQL, not setting the nullable state will set the column to NULL, but on Oracle setting a column NOT NULL when it's already NOT NULL will fail).
 *
 * The nullable information will then be ignored on Oracle.
 *
 */
public class AlterColumnsTypeBuilder {

  private final Dialect dialect;
  private final String tableName;
  private final List<ColumnDef> columnDefs = newArrayList();

  public AlterColumnsTypeBuilder(Dialect dialect, String tableName) {
    this.dialect = dialect;
    this.tableName = tableName;
  }

  public AlterColumnsTypeBuilder updateColumn(ColumnDef columnDef) {
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
        return createMsSqlAndH2Queries();
    }
  }

  private List<String> createPostgresQuery() {
    StringBuilder sql = new StringBuilder("ALTER TABLE " + tableName + " ");
    addColumns(sql, "ALTER COLUMN ", "TYPE ", true);
    return Collections.singletonList(sql.toString());
  }

  private List<String> createMySqlQuery() {
    StringBuilder sql = new StringBuilder("ALTER TABLE " + tableName + " ");
    addColumns(sql, "MODIFY COLUMN ", "", true);
    return Collections.singletonList(sql.toString());
  }

  private List<String> createOracleQuery() {
    StringBuilder sql = new StringBuilder("ALTER TABLE " + tableName + " ").append("MODIFY (");
    addColumns(sql, "", "", false);
    sql.append(")");
    return Collections.singletonList(sql.toString());
  }

  private List<String> createMsSqlAndH2Queries() {
    List<String> sqls = new ArrayList<>();
    for (ColumnDef columnDef : columnDefs) {
      StringBuilder defaultQuery = new StringBuilder("ALTER TABLE " + tableName + " ");
      defaultQuery.append("ALTER COLUMN ");
      addColumn(defaultQuery, columnDef, "", true);
      sqls.add(defaultQuery.toString());
    }
    return sqls;
  }

  private void addColumns(StringBuilder sql, String updateKeyword, String typePrefix, boolean addNotNullableProperty) {
    for (Iterator<ColumnDef> columnDefIterator = columnDefs.iterator(); columnDefIterator.hasNext();) {
      sql.append(updateKeyword);
      addColumn(sql, columnDefIterator.next(), typePrefix, addNotNullableProperty);
      if (columnDefIterator.hasNext()) {
        sql.append(", ");
      }
    }
  }

  private void addColumn(StringBuilder sql, ColumnDef columnDef, String typePrefix, boolean addNotNullableProperty) {
    sql.append(columnDef.getName())
      .append(" ")
      .append(typePrefix)
      .append(columnDef.generateSqlType(dialect));
    if (!columnDef.isNullable() && addNotNullableProperty) {
      sql.append(" NOT NULL");
    }
  }

}
