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

public class AddColumnsBuilderTest {

  static final String TABLE_NAME = "issues";
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void add_columns_on_h2() {
    assertThat(createSampleBuilder(new H2()).build())
      .isEqualTo("ALTER TABLE issues ADD (date_in_ms BIGINT NULL, name VARCHAR (10) NOT NULL)");
  }

  @Test
  public void add_columns_on_mysql() {
    assertThat(createSampleBuilder(new MySql()).build())
      .isEqualTo("ALTER TABLE issues ADD (date_in_ms BIGINT NULL, name VARCHAR (10) NOT NULL)");
  }

  @Test
  public void add_columns_on_oracle() {
    assertThat(createSampleBuilder(new Oracle()).build())
      .isEqualTo("ALTER TABLE issues ADD (date_in_ms NUMBER (38) NULL, name VARCHAR (10) NOT NULL)");
  }

  @Test
  public void add_columns_on_postgresql() {
    assertThat(createSampleBuilder(new PostgreSql()).build())
      .isEqualTo("ALTER TABLE issues ADD COLUMN date_in_ms BIGINT NULL, ADD COLUMN name VARCHAR (10) NOT NULL");
  }

  @Test
  public void add_columns_on_mssql() {
    assertThat(createSampleBuilder(new MsSql()).build())
      .isEqualTo("ALTER TABLE issues ADD date_in_ms BIGINT NULL, name NVARCHAR (10) NOT NULL");
  }

  @Test
  public void fail_with_ISE_if_no_column() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("No column has been defined");

    new AddColumnsBuilder(new H2(), TABLE_NAME).build();
  }

  private AddColumnsBuilder createSampleBuilder(Dialect dialect) {
    return new AddColumnsBuilder(dialect, TABLE_NAME)
      .addColumn(new BigDecimalColumnDef.Builder().setColumnName("date_in_ms").setIsNullable(true).build())
      .addColumn(new VarcharColumnDef.Builder().setColumnName("name").setLimit(10).setIsNullable(false).build());
  }
}
