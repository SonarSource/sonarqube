/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Dialect;
import org.sonar.server.platform.db.migration.def.TinyIntColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BlobColumnDef.newBlobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.DecimalColumnDef.newDecimalColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;
import static org.sonar.server.platform.db.migration.sql.CreateTableBuilder.ColumnFlag.AUTO_INCREMENT;

class CreateTableBuilderIT {
  @RegisterExtension
  public final MigrationDbTester dbTester = MigrationDbTester.createEmpty();

  private Dialect dialect;
  private static int tableNameGenerator = 0;

  @BeforeEach
  void before() {
    dialect = dbTester.database().getDialect();
  }

  @Test
  void create_no_primary_key_table() {
    String tableName = createTableName();
    new CreateTableBuilder(dialect, tableName)
      .addColumn(newBooleanColumnDefBuilder().setColumnName("bool_col_1").build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName("bool_col_2").setIsNullable(false).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName("i_col_1").build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName("i_col_2").setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("bi_col_1").build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("bi_col_2").setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("clob_col_1").build())
      .addColumn(newClobColumnDefBuilder().setColumnName("clob_col_2").setIsNullable(false).build())
      .addColumn(newDecimalColumnDefBuilder().setColumnName("dec_col_1").build())
      .addColumn(newDecimalColumnDefBuilder().setColumnName("dec_col_2").setIsNullable(false).build())
      .addColumn(new TinyIntColumnDef.Builder().setColumnName("tiny_col_1").build())
      .addColumn(new TinyIntColumnDef.Builder().setColumnName("tiny_col_2").setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("varchar_col_1").setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("varchar_col_2").setLimit(40).setIsNullable(false).build())
      .addColumn(newBlobColumnDefBuilder().setColumnName("blob_col_1").build())
      .addColumn(newBlobColumnDefBuilder().setColumnName("blob_col_2").setIsNullable(false).build())
      .build()
      .forEach(dbTester::executeDdl);
    assertTableAndColumnsExists(tableName, "bool_col_1", "bool_col_2", "i_col_1", "i_col_2", "bi_col_1", "bi_col_2", "clob_col_1",
      "clob_col_2", "dec_col_1", "dec_col_2", "tiny_col_1", "tiny_col_2", "varchar_col_1", "varchar_col_2", "blob_col_1", "blob_col_2");
  }

  @Test
  void create_single_column_primary_key_table() {
    String tableName = createTableName();
    new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col_1").setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("varchar_col_2").setLimit(40).setIsNullable(false).build())
      .build()
      .forEach(dbTester::executeDdl);
    assertTableAndColumnsExists(tableName, "bg_col_1", "varchar_col_2");
  }

  @Test
  void create_multi_column_primary_key_table() {
    String tableName = createTableName();
    new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col_1").setIsNullable(false).build())
      .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col_2").setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("varchar_col_2").setLimit(40).setIsNullable(false).build())
      .build()
      .forEach(dbTester::executeDdl);
    assertTableAndColumnsExists(tableName, "bg_col_1", "bg_col_2", "varchar_col_2");
  }

  @Test
  void create_autoincrement_notnullable_integer_primary_key_table() {
    String tableName = createTableName();
    new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
      .addColumn(valColumnDef())
      .build()
      .forEach(dbTester::executeDdl);

    verifyAutoIncrementIsWorking(tableName);
  }

  @Test
  void create_autoincrement_notnullable_biginteger_primary_key_table() {
    String tableName = createTableName();
    new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
      .addColumn(valColumnDef())
      .build()
      .forEach(dbTester::executeDdl);

    verifyAutoIncrementIsWorking(tableName);
  }

  private static VarcharColumnDef valColumnDef() {
    return newVarcharColumnDefBuilder().setColumnName("val").setLimit(10).setIsNullable(false).build();
  }

  private void verifyAutoIncrementIsWorking(String tableName) {
    dbTester.executeInsert(tableName, "val", "toto");

    Map<String, Object> row = dbTester.selectFirst("select id as \"id\", val as \"val\" from " + tableName);
    assertThat(row.get("id")).isNotNull();
    assertThat(row).containsEntry("val", "toto");
  }

  private void assertTableAndColumnsExists(String tableName, String... columnNames) {
    List<Map<String, Object>> row = dbTester.select(String.format("select %s from %s", String.join(", ", columnNames), tableName));
    assertThat(row).isEmpty();
  }

  private CreateTableBuilder newCreateTableBuilder() {
    return new CreateTableBuilder(dialect, createTableName());
  }

  private static String createTableName() {
    return "table_" + tableNameGenerator++;
  }
}
