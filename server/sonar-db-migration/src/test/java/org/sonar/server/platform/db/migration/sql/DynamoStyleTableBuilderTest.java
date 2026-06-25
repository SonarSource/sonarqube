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

import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

class DynamoStyleTableBuilderTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createEmpty();

  @Test
  void build_partitionKeyOnly_createsTableWithSingleColumnPrimaryKey() {
    execute(new DynamoStyleTableBuilder(db.database().getDialect(), "my_table")
      .withPartitionKey(newVarcharColumnDefBuilder().setColumnName("pk").setLimit(UUID_SIZE).setIsNullable(false).build()));

    db.assertTableExists("my_table");
    db.assertPrimaryKey("my_table", null, "pk");
    db.assertColumnDefinition("my_table", "pk", Types.VARCHAR, UUID_SIZE, false);
  }

  @Test
  void build_withSortKey_createsTableWithCompositePrimaryKey() {
    execute(new DynamoStyleTableBuilder(db.database().getDialect(), "my_table")
      .withPartitionKey(newVarcharColumnDefBuilder().setColumnName("pk").setLimit(UUID_SIZE).setIsNullable(false).build())
      .withSortKey(newVarcharColumnDefBuilder().setColumnName("sk").setLimit(UUID_SIZE).setIsNullable(false).build()));

    db.assertTableExists("my_table");
    db.assertPrimaryKey("my_table", null, "pk", "sk");
  }

  @Test
  void build_withAttribute_createsColumnOutsidePrimaryKey() {
    execute(new DynamoStyleTableBuilder(db.database().getDialect(), "my_table")
      .withPartitionKey(newVarcharColumnDefBuilder().setColumnName("pk").setLimit(UUID_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName("data").setLimit(255).setIsNullable(true).build()));

    db.assertColumnDefinition("my_table", "pk", Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition("my_table", "data", Types.VARCHAR, 255, true);
    // data is not part of the PK
    db.assertPrimaryKey("my_table", null, "pk");
  }

  @Test
  void build_withGlobalSecondaryIndex_createsIndexOnAttribute() {
    execute(new DynamoStyleTableBuilder(db.database().getDialect(), "my_table")
      .withPartitionKey(newVarcharColumnDefBuilder().setColumnName("pk").setLimit(UUID_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName("uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
      .withGlobalSecondaryIndex("my_table_uuid", "uuid"));

    db.assertTableExists("my_table");
    db.assertIndex("my_table", "my_table_uuid", "uuid");
  }

  @Test
  void build_withCompoundGlobalSecondaryIndex_createsIndexOnBothColumns() {
    execute(new DynamoStyleTableBuilder(db.database().getDialect(), "my_table")
      .withPartitionKey(newVarcharColumnDefBuilder().setColumnName("pk").setLimit(UUID_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName("gsi_pk").setLimit(UUID_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName("gsi_sk").setLimit(UUID_SIZE).setIsNullable(false).build())
      .withGlobalSecondaryIndex("my_table_gsi", "gsi_pk", "gsi_sk"));

    db.assertIndex("my_table", "my_table_gsi", "gsi_pk", "gsi_sk");
  }

  @Test
  void build_withLocalSecondaryIndex_createsIndexWithPartitionKeyAndAltSortColumn() {
    execute(new DynamoStyleTableBuilder(db.database().getDialect(), "my_table")
      .withPartitionKey(newVarcharColumnDefBuilder().setColumnName("pk").setLimit(UUID_SIZE).setIsNullable(false).build())
      .withSortKey(newVarcharColumnDefBuilder().setColumnName("sk").setLimit(UUID_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName("alt_sk").setLimit(UUID_SIZE).setIsNullable(false).build())
      .withLocalSecondaryIndex("my_table_lsi", "alt_sk"));

    // LSI shares the table's partition key as its first key column
    db.assertIndex("my_table", "my_table_lsi", "pk", "alt_sk");
  }

  @Test
  void build_withMultipleIndexes_createsEachIndex() {
    execute(new DynamoStyleTableBuilder(db.database().getDialect(), "my_table")
      .withPartitionKey(newVarcharColumnDefBuilder().setColumnName("pk").setLimit(UUID_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName("col_a").setLimit(UUID_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName("col_b").setLimit(UUID_SIZE).setIsNullable(false).build())
      .withGlobalSecondaryIndex("idx_a", "col_a")
      .withGlobalSecondaryIndex("idx_b", "col_b"));

    db.assertIndex("my_table", "idx_a", "col_a");
    db.assertIndex("my_table", "idx_b", "col_b");
  }

  @Test
  void build_withoutPartitionKey_throwsNPE() {
    var builder = new DynamoStyleTableBuilder(db.database().getDialect(), "my_table");
    assertThatNullPointerException().isThrownBy(builder::build);
  }

  @Test
  void withLocalSecondaryIndex_beforePartitionKey_throwsNPE() {
    var builder = new DynamoStyleTableBuilder(db.database().getDialect(), "my_table");
    assertThatThrownBy(() -> builder.withLocalSecondaryIndex("idx", "col"))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  void withPartitionKey_null_throwsNPE() {
    assertThatNullPointerException()
      .isThrownBy(() -> new DynamoStyleTableBuilder(db.database().getDialect(), "my_table").withPartitionKey(null));
  }

  @Test
  void withSortKey_null_throwsNPE() {
    assertThatNullPointerException()
      .isThrownBy(() -> new DynamoStyleTableBuilder(db.database().getDialect(), "my_table").withSortKey(null));
  }

  @Test
  void withAttribute_null_throwsNPE() {
    assertThatNullPointerException()
      .isThrownBy(() -> new DynamoStyleTableBuilder(db.database().getDialect(), "my_table").withAttribute(null));
  }

  private void execute(DynamoStyleTableBuilder builder) {
    builder.build().forEach(db::executeDdl);
  }
}
