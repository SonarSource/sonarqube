/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.Test;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;

public class DropColumnsBuilderTest {

  @Test
  public void drop_columns_on_mysql() {
    assertThat(new DropColumnsBuilder(new MySql(), "issues", "date_in_ms", "name").build())
      .containsOnly("ALTER TABLE issues DROP COLUMN date_in_ms, DROP COLUMN name");
  }

  @Test
  public void drop_columns_on_oracle() {
    assertThat(new DropColumnsBuilder(new Oracle(), "issues", "date_in_ms", "name").build())
      .containsOnly("ALTER TABLE issues SET UNUSED (date_in_ms, name)");
  }

  @Test
  public void drop_columns_on_postgresql() {
    assertThat(new DropColumnsBuilder(new PostgreSql(), "issues", "date_in_ms", "name").build())
      .containsOnly("ALTER TABLE issues DROP COLUMN date_in_ms, DROP COLUMN name");
  }

  @Test
  public void drop_columns_on_mssql() {
    assertThat(new DropColumnsBuilder(new MsSql(), "issues", "date_in_ms", "name").build())
      .containsOnly("ALTER TABLE issues DROP COLUMN date_in_ms, name");
  }

  @Test
  public void drop_columns_on_h2() {
    assertThat(new DropColumnsBuilder(new H2(), "issues", "date_in_ms", "name").build())
      .containsOnly(
        "ALTER TABLE issues DROP COLUMN date_in_ms",
        "ALTER TABLE issues DROP COLUMN name");
  }

}
