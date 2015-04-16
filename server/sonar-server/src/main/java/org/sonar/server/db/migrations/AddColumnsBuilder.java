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
package org.sonar.server.db.migrations;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.sonar.core.persistence.dialect.Dialect;
import org.sonar.core.persistence.dialect.MsSql;
import org.sonar.core.persistence.dialect.Oracle;
import org.sonar.core.persistence.dialect.PostgreSql;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;

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
    sql.append(columnDef.getName()).append(" ").append(typeToSql(columnDef));
    Integer limit = columnDef.getLimit();
    if (limit != null) {
      sql.append(" (").append(Integer.toString(limit)).append(")");
    }
    sql.append(columnDef.isNullable() ? " NULL" : " NOT NULL");
  }

  private String typeToSql(ColumnDef columnDef) {
    switch (columnDef.getType()) {
      case STRING:
        return "VARCHAR";
      case BIG_INTEGER:
        return !dialect.getId().equals(Oracle.ID) ? "BIGINT" : "NUMBER (38)";
      default:
        throw new IllegalArgumentException("Unsupported type : " + columnDef.getType());
    }
  }

  public static class ColumnDef {
    private String name;
    private Type type;
    private boolean isNullable;
    private Integer limit;

    public enum Type {
      STRING, BIG_INTEGER
    }

    public ColumnDef setNullable(boolean isNullable) {
      this.isNullable = isNullable;
      return this;
    }

    public ColumnDef setLimit(@Nullable Integer limit) {
      this.limit = limit;
      return this;
    }

    public ColumnDef setName(String name) {
      Preconditions.checkArgument(CharMatcher.JAVA_LOWER_CASE.or(CharMatcher.anyOf("_")).matchesAllOf(name), "Column name should only contains lowercase and _ characters");
      this.name = name;
      return this;
    }

    public ColumnDef setType(Type type) {
      this.type = type;
      return this;
    }

    public boolean isNullable() {
      return isNullable;
    }

    @CheckForNull
    public Integer getLimit() {
      return limit;
    }

    public String getName() {
      return name;
    }

    public Type getType() {
      return type;
    }
  }
}
