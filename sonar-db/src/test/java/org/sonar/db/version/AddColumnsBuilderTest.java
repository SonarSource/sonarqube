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
import static org.sonar.db.version.ColumnDef.Type.BIG_INTEGER;
import static org.sonar.db.version.ColumnDef.Type.STRING;

public class AddColumnsBuilderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  static final String TABLE_NAME = "issues";

  static final H2 H2_DIALECT = new H2();
  static final MySql MYSQL_DIALECT = new MySql();
  static final Oracle ORACLE_DIALECT = new Oracle();
  static final PostgreSql POSTGRES_DIALECT = new PostgreSql();
  static final MsSql MSSQL_DIALECT = new MsSql();

  @Test
  public void add_columns_on_h2() {
    assertThat(createSampleBuilder(H2_DIALECT).build())
      .isEqualTo("ALTER TABLE issues ADD (date_in_ms BIGINT NULL, name VARCHAR (10) NOT NULL)");
  }

  @Test
  public void add_columns_on_mysql() {
    assertThat(createSampleBuilder(MYSQL_DIALECT).build())
      .isEqualTo("ALTER TABLE issues ADD (date_in_ms BIGINT NULL, name VARCHAR (10) NOT NULL)");
  }

  @Test
  public void add_columns_on_oracle() {
    assertThat(createSampleBuilder(ORACLE_DIALECT).build())
      .isEqualTo("ALTER TABLE issues ADD (date_in_ms NUMBER (38) NULL, name VARCHAR (10) NOT NULL)");
  }

  @Test
  public void add_columns_on_postgresql() {
    assertThat(createSampleBuilder(POSTGRES_DIALECT).build())
      .isEqualTo("ALTER TABLE issues ADD COLUMN date_in_ms BIGINT NULL, ADD COLUMN name VARCHAR (10) NOT NULL");
  }

  @Test
  public void add_columns_on_mssql() {
    assertThat(createSampleBuilder(MSSQL_DIALECT).build())
      .isEqualTo("ALTER TABLE issues ADD date_in_ms BIGINT NULL, name VARCHAR (10) NOT NULL");
  }

  @Test
  public void fail_with_ISE_if_no_column() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("No column has been defined");

    new AddColumnsBuilder(H2_DIALECT, TABLE_NAME).build();
  }

  private AddColumnsBuilder createSampleBuilder(Dialect dialect) {
    return new AddColumnsBuilder(dialect, TABLE_NAME)
      .addColumn(new ColumnDef()
        .setName("date_in_ms")
        .setType(BIG_INTEGER)
        .setNullable(true))
      .addColumn(new ColumnDef()
        .setName("name")
        .setType(STRING)
        .setNullable(false)
        .setLimit(10));
  }

}
