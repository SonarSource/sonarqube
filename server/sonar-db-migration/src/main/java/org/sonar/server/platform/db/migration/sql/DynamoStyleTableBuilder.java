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
package org.sonar.server.platform.db.migration.sql;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.db.dialect.Dialect;
import org.sonar.server.platform.db.migration.def.ColumnDef;

import static java.util.Objects.requireNonNull;

/**
 * Declarative table builder that mirrors a DynamoDB table definition
 */
public class DynamoStyleTableBuilder {

  private final Dialect dialect;
  private final String tableName;
  private final List<ColumnDef> attributes = new ArrayList<>();
  private final List<SecondaryIndex> secondaryIndexes = new ArrayList<>();
  private ColumnDef partitionKey;
  @Nullable
  private ColumnDef sortKey;

  public DynamoStyleTableBuilder(Dialect dialect, String tableName) {
    this.dialect = dialect;
    this.tableName = tableName;
  }

  /** The DynamoDB HASH key. Required. */
  public DynamoStyleTableBuilder withPartitionKey(ColumnDef column) {
    this.partitionKey = requireNonNull(column, "partition key column can't be null");
    return this;
  }

  /** The DynamoDB RANGE key. Optional. */
  public DynamoStyleTableBuilder withSortKey(ColumnDef column) {
    this.sortKey = requireNonNull(column, "sort key column can't be null");
    return this;
  }

  /** A non-key attribute, i.e. a plain column. */
  public DynamoStyleTableBuilder withAttribute(ColumnDef column) {
    this.attributes.add(requireNonNull(column, "attribute column can't be null"));
    return this;
  }

  /** A global secondary index keyed by its own partition column. */
  public DynamoStyleTableBuilder withGlobalSecondaryIndex(String indexName, String partitionColumn) {
    return withGlobalSecondaryIndex(indexName, partitionColumn, null);
  }

  /** A global secondary index keyed by its own partition column and an optional sort column. */
  public DynamoStyleTableBuilder withGlobalSecondaryIndex(String indexName, String partitionColumn, @Nullable String sortColumn) {
    this.secondaryIndexes.add(new SecondaryIndex(indexName, keyColumns(partitionColumn, sortColumn)));
    return this;
  }

  /** A local secondary index: shares the table's partition key, with an alternate sort column. */
  public DynamoStyleTableBuilder withLocalSecondaryIndex(String indexName, String sortColumn) {
    requireNonNull(partitionKey, "partition key must be set before declaring a local secondary index");
    this.secondaryIndexes.add(new SecondaryIndex(indexName, keyColumns(partitionKey.getName(), sortColumn)));
    return this;
  }

  /** The {@code CREATE TABLE} statement(s) followed by one {@code CREATE INDEX} per secondary index. */
  public List<String> build() {
    requireNonNull(partitionKey, "partition key is required");
    CreateTableBuilder table = new CreateTableBuilder(dialect, tableName).addPkColumn(partitionKey);
    if (sortKey != null) {
      table.addPkColumn(sortKey);
    }
    attributes.forEach(table::addColumn);

    List<String> statements = new ArrayList<>(table.build());
    for (SecondaryIndex index : secondaryIndexes) {
      CreateIndexBuilder indexBuilder = new CreateIndexBuilder(dialect)
        .setTable(tableName)
        .setName(index.name())
        .setUnique(false);
      // DynamoDB key attributes are always present, so index key columns are non-nullable.
      index.columns().forEach(column -> indexBuilder.addColumn(column, false));
      statements.addAll(indexBuilder.build());
    }
    return statements;
  }

  private static List<String> keyColumns(String partitionColumn, @Nullable String sortColumn) {
    return sortColumn == null ? List.of(partitionColumn) : List.of(partitionColumn, sortColumn);
  }

  private record SecondaryIndex(String name, List<String> columns) {
  }
}
