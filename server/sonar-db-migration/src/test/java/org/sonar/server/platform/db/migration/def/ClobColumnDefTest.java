/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.def;

import org.junit.Test;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClobColumnDefTest {

  private final ClobColumnDef underTest = new ClobColumnDef.Builder()
    .setColumnName("issues")
    .setIsNullable(true)
    .build();

  @Test
  public void build_string_column_def() {
    assertThat(underTest.getName()).isEqualTo("issues");
    assertThat(underTest.isNullable()).isTrue();
    assertThat(underTest.getDefaultValue()).isNull();
  }

  @Test
  public void build_string_column_def_with_only_required_attributes() {
    ClobColumnDef def = new ClobColumnDef.Builder()
      .setColumnName("issues")
      .build();

    assertThat(def.getName()).isEqualTo("issues");
    assertThat(def.isNullable()).isTrue();
    assertThat(def.getDefaultValue()).isNull();
  }

  @Test
  public void generate_sql_type_on_mssql() {
    assertThat(underTest.generateSqlType(new MsSql())).isEqualTo("NVARCHAR (MAX)");
  }

  @Test
  public void generate_sql_type_on_h2() {
    assertThat(underTest.generateSqlType(new H2())).isEqualTo("CLOB");
  }

  @Test
  public void generate_sql_type_on_oracle() {
    assertThat(underTest.generateSqlType(new Oracle())).isEqualTo("CLOB");
  }

  @Test
  public void generate_sql_type_on_postgre() {
    assertThat(underTest.generateSqlType(new PostgreSql())).isEqualTo("TEXT");
  }

  @Test
  public void fail_with_NPE_if_name_is_null() {
    assertThatThrownBy(() -> {
      new ClobColumnDef.Builder()
        .setColumnName(null);
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Column name cannot be null");
  }

  @Test
  public void fail_with_NPE_if_no_name() {
    assertThatThrownBy(() -> {
      new ClobColumnDef.Builder()
        .build();
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Column name cannot be null");
  }
}
