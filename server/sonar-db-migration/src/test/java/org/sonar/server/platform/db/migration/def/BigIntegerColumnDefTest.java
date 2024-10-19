/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

public class BigIntegerColumnDefTest {
  @Test
  public void build_string_column_def() {
    BigIntegerColumnDef def = new BigIntegerColumnDef.Builder()
      .setColumnName("issues")
      .setIsNullable(true)
      .build();

    assertThat(def.getName()).isEqualTo("issues");
    assertThat(def.isNullable()).isTrue();
  }

  @Test
  public void build_string_column_def_with_default_values() {
    BigIntegerColumnDef def = new BigIntegerColumnDef.Builder()
      .setColumnName("issues")
      .build();

    assertThat(def.getName()).isEqualTo("issues");
    assertThat(def.isNullable()).isTrue();
  }

  @Test
  public void generate_sql_type() {
    BigIntegerColumnDef def = new BigIntegerColumnDef.Builder()
      .setColumnName("issues")
      .setIsNullable(true)
      .build();

    assertThat(def.generateSqlType(new H2())).isEqualTo("BIGINT");
    assertThat(def.generateSqlType(new PostgreSql())).isEqualTo("BIGINT");
    assertThat(def.generateSqlType(new MsSql())).isEqualTo("BIGINT");
    assertThat(def.generateSqlType(new Oracle())).isEqualTo("NUMBER (38)");
  }

  @Test
  public void fail_with_NPE_if_name_is_null() {
    assertThatThrownBy(() -> {
      new BigIntegerColumnDef.Builder()
        .setColumnName(null);
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Column name cannot be null");
  }

  @Test
  public void fail_with_NPE_if_no_name() {
    assertThatThrownBy(() -> {
      new BigIntegerColumnDef.Builder()
        .build();
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Column name cannot be null");
  }

}
