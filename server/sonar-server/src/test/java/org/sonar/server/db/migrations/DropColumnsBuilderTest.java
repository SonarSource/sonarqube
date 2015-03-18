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

public class DropColumnsBuilderTest {

  @Test
  public void drop_columns_on_mysql() throws Exception {
    assertThat(new DropColumnsBuilder(new MySql(), "issues", "date_in_ms", "name")
      .build()).isEqualTo("ALTER TABLE issues DROP COLUMN date_in_ms, DROP COLUMN name");
  }

  @Test
  public void drop_columns_on_oracle() throws Exception {
    assertThat(new DropColumnsBuilder(new Oracle(), "issues", "date_in_ms", "name")
      .build()).isEqualTo("ALTER TABLE issues DROP (date_in_ms, name)");
  }

  @Test
  public void drop_columns_on_postgresql() throws Exception {
    assertThat(new DropColumnsBuilder(new PostgreSql(), "issues", "date_in_ms", "name")
      .build()).isEqualTo("ALTER TABLE issues DROP COLUMN date_in_ms, DROP COLUMN name");
  }

  @Test
  public void drop_columns_on_mssql() throws Exception {
    assertThat(new DropColumnsBuilder(new MsSql(), "issues", "date_in_ms", "name")
      .build()).isEqualTo("ALTER TABLE issues DROP COLUMN date_in_ms, name");
  }

  @Test(expected = IllegalStateException.class)
  public void fail_to_drop_columns_on_h2() throws Exception {
    new DropColumnsBuilder(new H2(), "issues", "date_in_ms", "name")
      .build();
  }

}
