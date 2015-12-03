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

import static org.assertj.core.api.Assertions.assertThat;

public class ClobColumnDefTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void build_string_column_def() throws Exception {
    ClobColumnDef def = new ClobColumnDef.Builder()
      .setColumnName("issues")
      .setIsNullable(true)
      .build();

    assertThat(def.getName()).isEqualTo("issues");
    assertThat(def.isNullable()).isTrue();
  }

  @Test
  public void build_string_column_def_with_default_values() throws Exception {
    ClobColumnDef def = new ClobColumnDef.Builder()
      .setColumnName("issues")
      .build();

    assertThat(def.getName()).isEqualTo("issues");
    assertThat(def.isNullable()).isTrue();
  }

  @Test
  public void generate_sql_type() throws Exception {
    ClobColumnDef def = new ClobColumnDef.Builder()
      .setColumnName("issues")
      .setIsNullable(true)
      .build();

    assertThat(def.generateSqlType(new MsSql())).isEqualTo("NVARCHAR (MAX) COLLATE Latin1_General_CS_AS");
  }

  @Test
  public void fail_with_UOE_to_generate_sql_on_h2() throws Exception {
    ClobColumnDef def = new ClobColumnDef.Builder()
      .setColumnName("issues")
      .setIsNullable(true)
      .build();

    thrown.expect(UnsupportedOperationException.class);

    def.generateSqlType(new H2());
  }

  @Test
  public void fail_with_UOE_to_generate_sql_on_mysql() throws Exception {
    ClobColumnDef def = new ClobColumnDef.Builder()
      .setColumnName("issues")
      .setIsNullable(true)
      .build();

    thrown.expect(UnsupportedOperationException.class);

    def.generateSqlType(new MySql());
  }

  @Test
  public void fail_with_UOE_to_generate_sql_on_oracle() throws Exception {
    ClobColumnDef def = new ClobColumnDef.Builder()
      .setColumnName("issues")
      .setIsNullable(true)
      .build();

    thrown.expect(UnsupportedOperationException.class);

    def.generateSqlType(new Oracle());
  }

  @Test
  public void fail_with_NPE_if_name_is_null() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Column name cannot be null");

    new ClobColumnDef.Builder()
      .setColumnName(null);
  }

  @Test
  public void fail_with_NPE_if_no_name() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Column name cannot be null");

    new ClobColumnDef.Builder()
      .build();
  }
}
