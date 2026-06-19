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
import org.sonar.db.dialect.Dialect;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.platform.db.migration.def.BlobColumnDef.newBlobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.MAX_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Declarative table builder that mirrors an S3 bucket: objects addressed by a unique key, carrying a binary
 * body and a small set of user metadata
 */
public class S3StyleTableBuilder {

  private final Dialect dialect;
  private final String tableName;

  private String uuidColumn = "uuid";
  private String nameColumn = "name";
  private int nameLimit;
  private String nameUniqueIndexName;
  private String dataColumn = "data";
  private String metadataColumn = "metadata";
  private int metadataLimit = MAX_SIZE;

  public S3StyleTableBuilder(Dialect dialect, String tableName) {
    this.dialect = dialect;
    this.tableName = tableName;
  }

  /** The UUID v4 primary-key column. Defaults to {@code uuid}. */
  public S3StyleTableBuilder withUuidColumn(String column) {
    this.uuidColumn = requireNonNull(column, "uuid column can't be null");
    return this;
  }

  /** The object-key column (gets a unique, prefix-searchable index). Required. */
  public S3StyleTableBuilder withNameColumn(String column, int limit, String uniqueIndexName) {
    this.nameColumn = requireNonNull(column, "name column can't be null");
    this.nameLimit = limit;
    this.nameUniqueIndexName = requireNonNull(uniqueIndexName, "name unique index name can't be null");
    return this;
  }

  /** The binary body column. Defaults to {@code data}. */
  public S3StyleTableBuilder withDataColumn(String column) {
    this.dataColumn = requireNonNull(column, "data column can't be null");
    return this;
  }

  /** The user-metadata column (a JSON key/value map). Defaults to {@code metadata} sized {@code MAX_SIZE}. */
  public S3StyleTableBuilder withMetadataColumn(String column, int limit) {
    this.metadataColumn = requireNonNull(column, "metadata column can't be null");
    this.metadataLimit = limit;
    return this;
  }

  /** The {@code CREATE TABLE} statement(s) followed by the {@code CREATE UNIQUE INDEX} on the name column. */
  public List<String> build() {
    requireNonNull(nameUniqueIndexName, "name column is required (call withNameColumn)");
    CreateTableBuilder table = new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(uuidColumn).setLimit(UUID_SIZE).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(nameColumn).setLimit(nameLimit).setIsNullable(false).build())
      .addColumn(newBlobColumnDefBuilder().setColumnName(dataColumn).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(metadataColumn).setLimit(metadataLimit).setIsNullable(true).build());

    List<String> statements = new ArrayList<>(table.build());
    statements.addAll(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(nameUniqueIndexName)
      .setUnique(true)
      .addColumn(nameColumn, false)
      .build());
    return statements;
  }
}
