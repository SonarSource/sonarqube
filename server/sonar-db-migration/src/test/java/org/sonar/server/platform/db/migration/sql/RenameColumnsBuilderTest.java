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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.text.StrSubstitutor;
import org.junit.Test;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.BlobColumnDef;
import org.sonar.server.platform.db.migration.def.BooleanColumnDef;
import org.sonar.server.platform.db.migration.def.ClobColumnDef;
import org.sonar.server.platform.db.migration.def.ColumnDef;
import org.sonar.server.platform.db.migration.def.DecimalColumnDef;
import org.sonar.server.platform.db.migration.def.IntegerColumnDef;
import org.sonar.server.platform.db.migration.def.TinyIntColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RenameColumnsBuilderTest {
  private static final String NEW_COLUMN_NAME = "new_" + randomAlphabetic(6).toLowerCase();

  private static final DatabaseAndResult[] DATABASES = {
    new DatabaseAndResult(new H2(), "ALTER TABLE ${table_name} ALTER COLUMN ${old_column_name} RENAME TO ${new_column_name}"),
    new DatabaseAndResult(new PostgreSql(), "ALTER TABLE ${table_name} RENAME COLUMN ${old_column_name} TO ${new_column_name}"),
    new DatabaseAndResult(new MsSql(), "EXEC sp_rename '${table_name}.${old_column_name}', '${new_column_name}', 'COLUMN'"),
    new DatabaseAndResult(new Oracle(), "ALTER TABLE ${table_name} RENAME COLUMN ${old_column_name} TO ${new_column_name}")
  };

  public static final ColumnDef[] COLUMN_DEFS = {
    BigIntegerColumnDef.newBigIntegerColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(false).build(),
    BigIntegerColumnDef.newBigIntegerColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(true).build(),
    BlobColumnDef.newBlobColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(false).build(),
    BlobColumnDef.newBlobColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(true).build(),
    BooleanColumnDef.newBooleanColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(false).build(),
    BooleanColumnDef.newBooleanColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(true).build(),
    ClobColumnDef.newClobColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(false).build(),
    ClobColumnDef.newClobColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(true).build(),
    DecimalColumnDef.newDecimalColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(false).build(),
    DecimalColumnDef.newDecimalColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(true).build(),
    IntegerColumnDef.newIntegerColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(false).build(),
    IntegerColumnDef.newIntegerColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(true).build(),
    TinyIntColumnDef.newTinyIntColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(false).build(),
    TinyIntColumnDef.newTinyIntColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(true).build(),
    VarcharColumnDef.newVarcharColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(false).setLimit(10).build(),
    VarcharColumnDef.newVarcharColumnDefBuilder().setColumnName(NEW_COLUMN_NAME).setIsNullable(true).setLimit(10).build(),
  };

  public static final String[] ILLEGAL_COLUMN_NAME = {
    "",
    "AA",
    "A.A",
    "_A",
    "1A",
    "\uD801\uDC8B\uD801\uDC8C\uD801\uDC8D"
  };

  @Test
  public void run_checkSQL_results() {
    for (DatabaseAndResult database : DATABASES) {
      for (ColumnDef columnDef : COLUMN_DEFS) {
        checkSQL_results(database, columnDef);
      }
    }
  }

  private void checkSQL_results(
    DatabaseAndResult database,
    ColumnDef columnDef) {

    String oldColumnName = "old_" + randomAlphabetic(6).toLowerCase();
    String tableName = "table_" + randomAlphabetic(6).toLowerCase();

    List<String> result = new RenameColumnsBuilder(database.dialect(), tableName)
      .renameColumn(oldColumnName, columnDef)
      .build();

    Map<String, String> parameters = new HashMap<>();
    parameters.put("table_name", tableName);
    parameters.put("old_column_name", oldColumnName);
    parameters.put("new_column_name", NEW_COLUMN_NAME);
    parameters.put("column_def", columnDef.generateSqlType(database.dialect()));
    String expectedResult = StrSubstitutor.replace(database.templateSql(), parameters);
    assertThat(result).containsExactlyInAnyOrder(expectedResult);
  }

  @Test
  public void run_when_old_column_is_same_as_new_column_ISA_is_thrown() {
    for (DatabaseAndResult database : DATABASES) {
      for (ColumnDef columnDef : COLUMN_DEFS) {
        when_old_column_is_same_as_new_column_ISA_is_thrown(database, columnDef);
      }
    }
  }

  private void when_old_column_is_same_as_new_column_ISA_is_thrown(
    DatabaseAndResult database,
    ColumnDef columnDef) {

    String tableName = "table_" + randomAlphabetic(6).toLowerCase();

    RenameColumnsBuilder renameColumnsBuilder = new RenameColumnsBuilder(database.dialect(), tableName)
      .renameColumn(NEW_COLUMN_NAME, columnDef);
    assertThatThrownBy(renameColumnsBuilder::build)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Column names must be different");
  }

  @Test
  public void run_when_new_column_contains_illegal_character_ISA_is_thrown() {
    for (DatabaseAndResult database : DATABASES) {
      for (ColumnDef columnDef : COLUMN_DEFS) {
        for (String illegalColumnName : ILLEGAL_COLUMN_NAME) {
          when_new_column_contains_illegal_character_ISA_is_thrown(database, columnDef, illegalColumnName);
        }
      }
    }
  }

  private void when_new_column_contains_illegal_character_ISA_is_thrown(
    DatabaseAndResult database,
    ColumnDef columnDef,
    String illegalColumnName) {

    String tableName = "table_" + randomAlphabetic(6).toLowerCase();

    RenameColumnsBuilder renameColumnsBuilder = new RenameColumnsBuilder(database.dialect(), tableName)
      .renameColumn(illegalColumnName, columnDef);

    assertThatThrownBy(renameColumnsBuilder::build)
      .isInstanceOf(IllegalArgumentException.class);
  }

  private record DatabaseAndResult(Dialect dialect, String templateSql) {
  }
}
