/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version;

import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.sonar.db.version.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.db.version.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.db.version.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.db.version.DecimalColumnDef.newDecimalColumnDefBuilder;
import static org.sonar.db.version.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateTableBuilderDbTesterTest {
  @ClassRule
  public static final DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Test
  public void create_no_primary_key_table() {
    String createTableStmt = new CreateTableBuilder(dbTester.getDbClient().getDatabase().getDialect(), "TABLE_1")
      .addColumn(newBooleanColumnDefBuilder().setColumnName("bool_col_1").build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName("bool_col_2").setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col_1").build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col_2").setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("clob_col_1").build())
      .addColumn(newClobColumnDefBuilder().setColumnName("clob_col_2").setIsNullable(false).build())
      .addColumn(newDecimalColumnDefBuilder().setColumnName("dec_col_1").build())
      .addColumn(newDecimalColumnDefBuilder().setColumnName("dec_col_2").setIsNullable(false).build())
      .addColumn(new TinyIntColumnDef.Builder().setColumnName("tiny_col_1").build())
      .addColumn(new TinyIntColumnDef.Builder().setColumnName("tiny_col_2").setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("varchar_col_1").setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("varchar_col_2").setLimit(40).setIsNullable(false).build())
      .build()
      .iterator().next();

    dbTester.executeDdl(createTableStmt);
  }

  @Test
  public void create_single_column_primary_key_table() {
    String createTableStmt = new CreateTableBuilder(dbTester.getDbClient().getDatabase().getDialect(), "TABLE_2")
      .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col_1").setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("varchar_col_2").setLimit(40).setIsNullable(false).build())
      .build()
      .iterator().next();

    dbTester.executeDdl(createTableStmt);
  }

  @Test
  public void create_multi_column_primary_key_table() {
    String createTableStmt = new CreateTableBuilder(dbTester.getDbClient().getDatabase().getDialect(), "TABLE_3")
      .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col_1").setIsNullable(false).build())
      .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col_2").setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("varchar_col_2").setLimit(40).setIsNullable(false).build())
      .build()
      .iterator().next();

    dbTester.executeDdl(createTableStmt);
  }
}
