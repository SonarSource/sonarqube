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

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Dialect;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.MAX_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

class S3StyleTableBuilderTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createEmpty();

  private static final String TABLE_NAME = "my_blobs";
  private static final String INDEX_NAME = "my_blobs_name";

  @Test
  void build_fails_when_withNameColumn_not_called() {
    Dialect dialect = db.database().getDialect();
    assertThatThrownBy(() -> new S3StyleTableBuilder(dialect, TABLE_NAME).build())
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("name column is required");
  }

  @Test
  void build_creates_table_with_expected_schema() throws SQLException {
    execute(new S3StyleTableBuilder(db.database().getDialect(), TABLE_NAME)
      .withNameColumn("name", UUID_SIZE, INDEX_NAME));

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_my_blobs", "uuid");
    db.assertColumnDefinition(TABLE_NAME, "uuid", Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, "name", Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, "data", Types.BLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, "metadata", Types.VARCHAR, MAX_SIZE, true);
    db.assertUniqueIndex(TABLE_NAME, INDEX_NAME, "name");
  }

  @Test
  void build_respects_overridden_column_names_and_metadata_limit() throws SQLException {
    execute(new S3StyleTableBuilder(db.database().getDialect(), TABLE_NAME)
      .withUuidColumn("id")
      .withNameColumn("object_key", 255, INDEX_NAME)
      .withDataColumn("payload")
      .withMetadataColumn("attrs", 1000));

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_my_blobs", "id");
    db.assertColumnDefinition(TABLE_NAME, "id", Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, "object_key", Types.VARCHAR, 255, false);
    db.assertColumnDefinition(TABLE_NAME, "payload", Types.BLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, "attrs", Types.VARCHAR, 1000, true);
    db.assertUniqueIndex(TABLE_NAME, INDEX_NAME, "object_key");
  }

  private void execute(S3StyleTableBuilder builder) throws SQLException {
    try (var connection = db.database().getDataSource().getConnection()) {
      for (String sql : builder.build()) {
        connection.createStatement().execute(sql);
      }
    }
  }
}
