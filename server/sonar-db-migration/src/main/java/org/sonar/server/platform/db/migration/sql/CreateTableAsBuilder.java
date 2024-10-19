/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.def.ColumnDef;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.platform.db.migration.def.Validations.validateTableName;

/**
 * Creates a new table based on an existing table.
 * With Oracle, H2 and PSQL it uses the 'CREATE TABLE [...] AS' statement. This is not supported in SQL Server, so we use 'SELECT [...] INTO [new_table] FROM [old_table]'.
 * Note that indexes are not kept. Constraints are also not kept except for 'NOT NULL' in some dbs and under certain conditions. Some dbs also allow to specify 'NOT NULL'
 * constraint or even data type when specifying the new table.
 * For simplicity, we explicitly add NOT NULL constrains with separate statements for all DBs, since it's a fast operation.
 */
public class CreateTableAsBuilder {
  private final Dialect dialect;
  private final String tableName;
  private final String fromTableName;
  private final List<Column> columns = new ArrayList<>();

  public CreateTableAsBuilder(Dialect dialect, String tableName, String fromTableName) {
    this.dialect = requireNonNull(dialect, "dialect can't be null");
    this.tableName = validateTableName(tableName);
    this.fromTableName = validateTableName(fromTableName);
    checkArgument(!tableName.equals(fromTableName), "Table names must be different");
  }

  public CreateTableAsBuilder addColumn(ColumnDef column) {
    columns.add(new Column(column, null));
    return this;
  }

  public CreateTableAsBuilder addColumnWithCast(ColumnDef column, String castFrom) {
    columns.add(new Column(column, castFrom));
    return this;
  }

  public List<String> build() {
    checkState(!columns.isEmpty(), "Columns need to be specified");

    List<String> sql = new ArrayList<>();

    String select = columns.stream().map(this::toSelect).collect(Collectors.joining(", "));

    if (dialect.getId().equals(MsSql.ID)) {
      sql.add("SELECT " + select + " INTO " + tableName + " FROM " + fromTableName);
    } else {
      StringBuilder sb = new StringBuilder("CREATE TABLE " + tableName + " (");
      appendColumnNames(sb);
      sb.append(") AS (SELECT ").append(select).append(" FROM ").append(fromTableName).append(")");
      sql.add(sb.toString());
    }

    List<Column> notNullColumns = columns.stream().filter(c -> !c.definition().isNullable()).toList();
    for (Column c : notNullColumns) {
      sql.addAll(new AlterColumnsBuilder(dialect, tableName).updateColumn(c.definition()).build());
    }

    return sql;
  }

  private String toSelect(Column column) {
    if (column.castFrom() == null) {
      return column.definition().getName();
    }
    // Example: CAST (metric_id AS VARCHAR(40)) AS metric_uuid
    return "CAST (" + column.castFrom() + " AS " + column.definition().generateSqlType(dialect) + ") AS " + column.definition().getName();
  }

  private void appendColumnNames(StringBuilder res) {
    res.append(columns.stream().map(c -> c.definition().getName()).collect(Collectors.joining(", ")));
  }

  private static class Column {
    private final ColumnDef columnDef;
    private final String castFrom;

    public Column(ColumnDef columnDef, @Nullable String castFrom) {
      this.columnDef = columnDef;
      this.castFrom = castFrom;
    }

    private ColumnDef definition() {
      return columnDef;
    }

    private String castFrom() {
      return castFrom;
    }
  }
}
