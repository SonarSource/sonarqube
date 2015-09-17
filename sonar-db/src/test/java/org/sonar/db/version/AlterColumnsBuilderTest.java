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
import static org.sonar.db.version.DecimalColumnDef.newDecimalColumnDefBuilder;
import static org.sonar.db.version.StringColumnDef.newStringColumnDefBuilder;

public class AlterColumnsBuilderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  static final String TABLE_NAME = "issues";

  @Test
  public void update_columns_on_h2() {
    assertThat(createSampleBuilder(new H2()).build())
      .containsOnly("ALTER TABLE issues ALTER COLUMN value DOUBLE", "ALTER TABLE issues ALTER COLUMN name VARCHAR (10)");
  }

  @Test
  public void update_columns_on_mssql() {
    assertThat(createSampleBuilder(new MsSql()).build())
      .containsOnly("ALTER TABLE issues ALTER COLUMN value DECIMAL (30,20)", "ALTER TABLE issues ALTER COLUMN name VARCHAR (10)");
  }

  @Test
  public void update_columns_on_postgres() {
    assertThat(createSampleBuilder(new PostgreSql()).build())
      .containsOnly("ALTER TABLE issues ALTER COLUMN value TYPE NUMERIC (30,20), ALTER COLUMN name TYPE VARCHAR (10)");
  }

  @Test
  public void update_columns_on_mysql() {
    assertThat(createSampleBuilder(new MySql()).build())
      .containsOnly("ALTER TABLE issues MODIFY COLUMN value DECIMAL (30,20), MODIFY COLUMN name VARCHAR (10)");
  }

  @Test
  public void update_columns_on_oracle() {
    assertThat(createSampleBuilder(new Oracle()).build())
      .containsOnly("ALTER TABLE issues MODIFY (value NUMERIC (30,20), name VARCHAR (10))");
  }

  @Test
  public void fail_with_ISE_if_no_column() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("No column has been defined");

    new AlterColumnsBuilder(new H2(), TABLE_NAME).build();
  }

  private AlterColumnsBuilder createSampleBuilder(Dialect dialect) {
    return new AlterColumnsBuilder(dialect, TABLE_NAME)
      .updateColumn(
        newDecimalColumnDefBuilder()
          .setColumnName("value")
          .setPrecision(30)
          .setScale(20)
          .build())
      .updateColumn(
        newStringColumnDefBuilder()
          .setColumnName("name")
          .setLimit(10)
          .build());
  }

}
