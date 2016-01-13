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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.version.DecimalColumnDef.newDecimalColumnDefBuilder;
import static org.sonar.db.version.VarcharColumnDef.newVarcharColumnDefBuilder;

public class AlterColumnsTypeBuilderTest {

  static final String TABLE_NAME = "issues";
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void update_columns_on_h2() {
    assertThat(createSampleBuilder(new H2()).build())
      .containsOnly("ALTER TABLE issues ALTER COLUMN value DOUBLE", "ALTER TABLE issues ALTER COLUMN name VARCHAR (10)");
  }

  @Test
  public void update_not_nullable_column_on_h2() {
    assertThat(createNotNullableBuilder(new H2()).build())
      .containsOnly("ALTER TABLE issues ALTER COLUMN name VARCHAR (10) NOT NULL");
  }

  @Test
  public void update_columns_on_mssql() {
    assertThat(createSampleBuilder(new MsSql()).build())
      .containsOnly("ALTER TABLE issues ALTER COLUMN value DECIMAL (30,20)", "ALTER TABLE issues ALTER COLUMN name NVARCHAR (10) COLLATE Latin1_General_CS_AS");
  }

  @Test
  public void update_not_nullable_column_on_mssql() {
    assertThat(createNotNullableBuilder(new MsSql()).build())
      .containsOnly("ALTER TABLE issues ALTER COLUMN name NVARCHAR (10) COLLATE Latin1_General_CS_AS NOT NULL");
  }

  @Test
  public void update_columns_on_postgres() {
    assertThat(createSampleBuilder(new PostgreSql()).build())
      .containsOnly("ALTER TABLE issues ALTER COLUMN value TYPE NUMERIC (30,20), ALTER COLUMN name TYPE VARCHAR (10)");
  }

  @Test
  public void update_not_nullable_column_on_postgres() {
    assertThat(createNotNullableBuilder(new PostgreSql()).build())
      .containsOnly("ALTER TABLE issues ALTER COLUMN name TYPE VARCHAR (10) NOT NULL");
  }

  @Test
  public void update_columns_on_mysql() {
    assertThat(createSampleBuilder(new MySql()).build())
      .containsOnly("ALTER TABLE issues MODIFY COLUMN value DECIMAL (30,20), MODIFY COLUMN name VARCHAR (10)");
  }

  @Test
  public void update_not_nullable_column_on_mysql() {
    assertThat(createNotNullableBuilder(new MySql()).build())
      .containsOnly("ALTER TABLE issues MODIFY COLUMN name VARCHAR (10) NOT NULL");
  }

  @Test
  public void update_columns_on_oracle() {
    assertThat(createSampleBuilder(new Oracle()).build())
      .containsOnly("ALTER TABLE issues MODIFY (value NUMERIC (30,20), name VARCHAR (10))");
  }

  @Test
  public void not_nullable_column_are_ignored_on_oracle() {
    assertThat(createNotNullableBuilder(new Oracle()).build())
      .containsOnly("ALTER TABLE issues MODIFY (name VARCHAR (10))");
  }

  @Test
  public void fail_with_ISE_if_no_column() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("No column has been defined");

    new AlterColumnsTypeBuilder(new H2(), TABLE_NAME).build();
  }

  private AlterColumnsTypeBuilder createSampleBuilder(Dialect dialect) {
    return new AlterColumnsTypeBuilder(dialect, TABLE_NAME)
      .updateColumn(
        newDecimalColumnDefBuilder()
          .setColumnName("value")
          .setPrecision(30)
          .setScale(20)
          .setIsNullable(true)
          .build())
      .updateColumn(
        newVarcharColumnDefBuilder()
          .setColumnName("name")
          .setLimit(10)
          .setIsNullable(true)
          .build());
  }

  private AlterColumnsTypeBuilder createNotNullableBuilder(Dialect dialect) {
    return new AlterColumnsTypeBuilder(dialect, TABLE_NAME)
      .updateColumn(
      newVarcharColumnDefBuilder()
        .setColumnName("name")
        .setLimit(10)
        .setIsNullable(false)
        .build());
  }

}
