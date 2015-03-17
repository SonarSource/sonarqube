/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.db.migrations;

import org.junit.Test;
import org.sonar.core.persistence.dialect.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class AddColumnsBuilderTest {

  @Test
  public void add_columns_on_h2() throws Exception {
    assertThat(new AddColumnsBuilder(new H2(), "issues")
      .addColumn(new AddColumnsBuilder.ColumnDef()
        .setName("date_in_ms")
        .setType(AddColumnsBuilder.ColumnDef.Type.BIG_INTEGER)
        .setNullable(true))
      .addColumn(new AddColumnsBuilder.ColumnDef()
        .setName("name")
        .setType(AddColumnsBuilder.ColumnDef.Type.STRING)
        .setNullable(false)
        .setLimit(10))
      .build()).isEqualTo("ALTER TABLE issues ADD (date_in_ms BIGINT NULL, name VARCHAR (10) NOT NULL)");
  }

  @Test
  public void add_columns_on_mysql() throws Exception {
    assertThat(new AddColumnsBuilder(new MySql(), "issues")
      .addColumn(new AddColumnsBuilder.ColumnDef()
        .setName("date_in_ms")
        .setType(AddColumnsBuilder.ColumnDef.Type.BIG_INTEGER)
        .setNullable(true))
      .addColumn(new AddColumnsBuilder.ColumnDef()
        .setName("name")
        .setType(AddColumnsBuilder.ColumnDef.Type.STRING)
        .setNullable(false)
        .setLimit(10))
      .build()).isEqualTo("ALTER TABLE issues ADD (date_in_ms BIGINT NULL, name VARCHAR (10) NOT NULL)");
  }

  @Test
  public void add_columns_on_oracle() throws Exception {
    assertThat(new AddColumnsBuilder(new Oracle(), "issues")
      .addColumn(new AddColumnsBuilder.ColumnDef()
        .setName("date_in_ms")
        .setType(AddColumnsBuilder.ColumnDef.Type.BIG_INTEGER)
        .setNullable(true))
      .addColumn(new AddColumnsBuilder.ColumnDef()
        .setName("name")
        .setType(AddColumnsBuilder.ColumnDef.Type.STRING)
        .setNullable(false)
        .setLimit(10))
      .build()).isEqualTo("ALTER TABLE issues ADD (date_in_ms NUMBER (38) NULL, name VARCHAR (10) NOT NULL)");
  }

  @Test
  public void add_columns_on_postgresql() throws Exception {
    assertThat(new AddColumnsBuilder(new PostgreSql(), "issues")
      .addColumn(new AddColumnsBuilder.ColumnDef()
        .setName("date_in_ms")
        .setType(AddColumnsBuilder.ColumnDef.Type.BIG_INTEGER)
        .setNullable(true))
      .addColumn(new AddColumnsBuilder.ColumnDef()
        .setName("name")
        .setType(AddColumnsBuilder.ColumnDef.Type.STRING)
        .setNullable(false)
        .setLimit(10))
      .build()).isEqualTo("ALTER TABLE issues ADD COLUMN date_in_ms BIGINT NULL, ADD COLUMN name VARCHAR (10) NOT NULL");
  }

  @Test
  public void add_columns_on_mssql() throws Exception {
    assertThat(new AddColumnsBuilder(new MsSql(), "issues")
      .addColumn(new AddColumnsBuilder.ColumnDef()
        .setName("date_in_ms")
        .setType(AddColumnsBuilder.ColumnDef.Type.BIG_INTEGER)
        .setNullable(true))
      .addColumn(new AddColumnsBuilder.ColumnDef()
        .setName("name")
        .setType(AddColumnsBuilder.ColumnDef.Type.STRING)
        .setNullable(false)
        .setLimit(10))
      .build()).isEqualTo("ALTER TABLE issues ADD date_in_ms BIGINT NULL, name VARCHAR (10) NOT NULL");
  }

  @Test
  public void fail_when_column_name_is_in_upper_case() throws Exception {
    try {
      new AddColumnsBuilder.ColumnDef()
        .setName("DATE_IN_MS")
        .setType(AddColumnsBuilder.ColumnDef.Type.BIG_INTEGER)
        .setNullable(true);
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Column name should only contains lowercase and _ characters");
    }
  }

  @Test
  public void fail_when_column_name_contains_invalid_character() throws Exception {
    try {
      new AddColumnsBuilder.ColumnDef()
        .setName("date-in/ms")
        .setType(AddColumnsBuilder.ColumnDef.Type.BIG_INTEGER)
        .setNullable(true);
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Column name should only contains lowercase and _ characters");
    }
  }

}
