/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.def.ColumnDef;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.platform.db.migration.def.Validations.validateIndexName;
import static org.sonar.server.platform.db.migration.def.Validations.validateTableName;

public class CreateIndexBuilder {

  private static final String COLUMN_CANNOT_BE_NULL = "Column cannot be null";
  private final List<NullableColumn> columns = new ArrayList<>();
  private final Dialect dialect;
  private String tableName;
  private String indexName;
  private boolean unique = false;

  public CreateIndexBuilder(Dialect dialect) {
    this.dialect = dialect;
  }

  /**
   * Required name of table on which index is created
   */
  public CreateIndexBuilder setTable(String s) {
    this.tableName = s;
    return this;
  }

  /**
   * Required name of index. Name must be unique among all the tables
   * of the schema.
   */
  public CreateIndexBuilder setName(String s) {
    this.indexName = s;
    return this;
  }

  /**
   * By default index is NOT UNIQUE (value {@code false}).
   */
  public CreateIndexBuilder setUnique(boolean b) {
    this.unique = b;
    return this;
  }

  /**
   * Add a column to the scope of index. Order of calls to this
   * method is important and is kept as-is when creating the index.
   * The attribute used from {@link ColumnDef} is the name.
   * Other attributes are ignored.
   */
  public CreateIndexBuilder addColumn(ColumnDef column) {
    requireNonNull(column, COLUMN_CANNOT_BE_NULL);
    columns.add(new NullableColumn(column.getName(), column.isNullable()));
    return this;
  }

  /**
   * Add a column to the scope of index. Order of calls to this
   * method is important and is kept as-is when creating the index.
   *
   */
  public CreateIndexBuilder addColumn(String column) {
    requireNonNull(column, COLUMN_CANNOT_BE_NULL);
    columns.add(new NullableColumn(column, null));
    return this;
  }

  public CreateIndexBuilder addColumn(String column, boolean isNullable) {
    requireNonNull(column, COLUMN_CANNOT_BE_NULL);
    columns.add(new NullableColumn(column, isNullable));
    return this;
  }

  public List<String> build() {
    validateTableName(tableName);
    validateIndexName(indexName);
    validateColumnsForUniqueIndex(unique, columns);
    checkArgument(!columns.isEmpty(), "at least one column must be specified");
    return singletonList(createSqlStatement());
  }

  private static void validateColumnsForUniqueIndex(boolean unique, List<NullableColumn> columns) {
    checkArgument(!unique || columns.stream().allMatch(c->c.isNullable() != null), "Nullability of column should be provided for unique indexes");
  }

  /**
   *
   */
  private String createSqlStatement() {
    StringBuilder sql = new StringBuilder("CREATE ");
    if (unique) {
      sql.append("UNIQUE ");
      if (dialect.supportsNullNotDistinct() && !PostgreSql.ID.equals(dialect.getId())) {
        sql.append("NULLS NOT DISTINCT ");
      }
    }
    sql.append("INDEX ");
    sql.append(indexName);
    sql.append(" ON ");
    sql.append(tableName);
    sql.append(" (");

    /*
     * Oldest versions of postgres don't support NULLS NOT DISTINCT, and their default behavior is NULLS DISTINCT.
     * To make sure we apply the same constraints as other DB vendors, we use coalesce to default to empty string, to ensure unicity constraint.
     * Other db vendors are not impacted since they fall back to NULLS NOT DISTINCT by default.
     */
    if (unique && !dialect.supportsNullNotDistinct() && PostgreSql.ID.equals(dialect.getId())) {
      sql.append(columns.stream()
        .map(c -> Boolean.TRUE.equals(c.isNullable()) ? "COALESCE(%s, '')".formatted(c.name()) : c.name())
        .collect(Collectors.joining(", ")));
    } else {
      sql.append(columns.stream()
        .map(NullableColumn::name)
        .collect(Collectors.joining(", ")));
    }

    sql.append(")");

    if (unique && dialect.supportsNullNotDistinct() && PostgreSql.ID.equals(dialect.getId())) {
      sql.append(" NULLS NOT DISTINCT");
    }
    return sql.toString();
  }

  private record NullableColumn(String name, @Nullable Boolean isNullable) {
  }
}
