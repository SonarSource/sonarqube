/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.server.platform.db.migration.def.ColumnDef;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.platform.db.migration.def.Validations.validateIndexName;
import static org.sonar.server.platform.db.migration.def.Validations.validateTableName;

public class CreateIndexBuilder {

  private final List<ColumnDef> columns = new ArrayList<>();
  private String tableName;
  private String indexName;
  private boolean unique = false;

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
   * The attributes used from {@link ColumnDef} are the name, the type
   * and the length (in case of VARCHAR). Other attributes are ignored.
   */
  public CreateIndexBuilder addColumn(ColumnDef column) {
    columns.add(requireNonNull(column, "Column cannot be null"));
    return this;
  }

  public List<String> build() {
    validateTableName(tableName);
    validateIndexName(indexName);
    checkArgument(!columns.isEmpty(), "at least one column must be specified");
    return singletonList(createSqlStatement());
  }

  private String createSqlStatement() {
    StringBuilder sql = new StringBuilder("CREATE ");
    if (unique) {
      sql.append("UNIQUE ");
    }
    sql.append("INDEX ");
    sql.append(indexName);
    sql.append(" ON ");
    sql.append(tableName);
    sql.append(" (");
    sql.append(columns.stream().map(ColumnDef::getName).collect(Collectors.joining(", ")));
    sql.append(")");
    return sql.toString();
  }
}
