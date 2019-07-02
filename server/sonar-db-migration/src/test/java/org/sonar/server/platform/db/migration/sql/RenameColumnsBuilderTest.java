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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.text.StrSubstitutor;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
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

@RunWith(Theories.class)
public class RenameColumnsBuilderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String NEW_COLUMN_NAME = "new_" + randomAlphabetic(6).toLowerCase();

  @DataPoints("database")
  public static final DatabaseAndResult[] DATABASES = {
    new DatabaseAndResult(new H2(), "ALTER TABLE ${table_name} ALTER COLUMN ${old_column_name} RENAME TO ${new_column_name}"),
    new DatabaseAndResult(new PostgreSql(), "ALTER TABLE ${table_name} RENAME COLUMN ${old_column_name} TO ${new_column_name}"),
    new DatabaseAndResult(new MsSql(), "EXEC sp_rename '${table_name}.${old_column_name}', '${new_column_name}', 'COLUMN'"),
    new DatabaseAndResult(new Oracle(), "ALTER TABLE ${table_name} RENAME COLUMN ${old_column_name} TO ${new_column_name}")
  };

  @DataPoints("columnDef")
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

  @DataPoints("illegalColumnName")
  public static final String[] ILLEGAL_COLUMN_NAME = {
    "",
    "AA",
    "A.A",
    "_A",
    "1A",
    "\uD801\uDC8B\uD801\uDC8C\uD801\uDC8D"
  };

  @Theory
  public void checkSQL_results(
    @FromDataPoints("database") DatabaseAndResult database,
    @FromDataPoints("columnDef") ColumnDef columnDef) {

    String oldColumnName = "old_" + randomAlphabetic(6).toLowerCase();
    String tableName = "table_" + randomAlphabetic(6).toLowerCase();

    List<String> result = new RenameColumnsBuilder(database.getDialect(), tableName)
      .renameColumn(oldColumnName, columnDef)
      .build();

    Map<String, String> parameters = new HashMap<>();
    parameters.put("table_name", tableName);
    parameters.put("old_column_name", oldColumnName);
    parameters.put("new_column_name", NEW_COLUMN_NAME);
    parameters.put("column_def", columnDef.generateSqlType(database.getDialect()));
    String expectedResult = StrSubstitutor.replace(database.getTemplateSql(), parameters);
    assertThat(result).containsExactlyInAnyOrder(expectedResult);
  }

  @Theory
  public void when_old_column_is_same_as_new_column_ISA_is_thrown (
    @FromDataPoints("database") DatabaseAndResult database,
    @FromDataPoints("columnDef") ColumnDef columnDef) {

    String tableName = "table_" + randomAlphabetic(6).toLowerCase();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Column names must be different");

    new RenameColumnsBuilder(database.getDialect(), tableName)
      .renameColumn(NEW_COLUMN_NAME, columnDef)
      .build();
  }

  @Theory
  public void when_new_column_contains_illegal_character_ISA_is_thrown (
    @FromDataPoints("database") DatabaseAndResult database,
    @FromDataPoints("columnDef") ColumnDef columnDef,
    @FromDataPoints("illegalColumnName") String illegalColumnName) {

    String tableName = "table_" + randomAlphabetic(6).toLowerCase();

    expectedException.expect(IllegalArgumentException.class);

    new RenameColumnsBuilder(database.getDialect(), tableName)
      .renameColumn(illegalColumnName, columnDef)
      .build();
  }

  private static class DatabaseAndResult {
    private final Dialect dialect;
    private final String templateSql;

    private DatabaseAndResult(Dialect dialect, String templateSql) {
      this.dialect = dialect;
      this.templateSql = templateSql;
    }

    public Dialect getDialect() {
      return dialect;
    }

    public String getTemplateSql() {
      return templateSql;
    }
  }
}
