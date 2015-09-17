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
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;

public class StringColumnDefTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void build_string_column_def() throws Exception {
    StringColumnDef def = new StringColumnDef.Builder()
      .setColumnName("issues")
      .setLimit(10)
      .setIsNullable(true)
      .build();

    assertThat(def.getName()).isEqualTo("issues");
    assertThat(def.getColumnSize()).isEqualTo(10);
    assertThat(def.isNullable()).isTrue();
  }

  @Test
  public void generate_sql_type() throws Exception {
    StringColumnDef def = new StringColumnDef.Builder()
      .setColumnName("issues")
      .setLimit(10)
      .setIsNullable(true)
      .build();

    assertThat(def.generateSqlType(new H2())).isEqualTo("VARCHAR (10)");
    assertThat(def.generateSqlType(new PostgreSql())).isEqualTo("VARCHAR (10)");
    assertThat(def.generateSqlType(new MySql())).isEqualTo("VARCHAR (10)");
    assertThat(def.generateSqlType(new MsSql())).isEqualTo("VARCHAR (10)");
    assertThat(def.generateSqlType(new Oracle())).isEqualTo("VARCHAR (10)");
  }

  @Test
  public void fail_with_NPE_if_name_is_null() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Column name cannot be null");

    new StringColumnDef.Builder()
      .setColumnName(null);
  }

  @Test
  public void fail_with_NPE_if_no_name() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Column name cannot be null");

    new StringColumnDef.Builder()
      .build();
  }

  @Test
  public void fail_with_NPE_if_size_is_null() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Limit cannot be null");

    new StringColumnDef.Builder()
      .setColumnName("issues")
      .build();
  }
}
