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
package org.sonar.server.platform.db.migration.def;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;


public class BooleanColumnDefTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void build_column_def() {
    BooleanColumnDef def = new BooleanColumnDef.Builder()
      .setColumnName("enabled")
      .setIsNullable(false)
      .setDefaultValue(true)
      .build();

    assertThat(def.getName()).isEqualTo("enabled");
    assertThat(def.isNullable()).isFalse();
    assertThat(def.getDefaultValue()).isEqualTo(true);
  }

  @Test
  public void build_column_def_with_only_required_attributes() {
    BooleanColumnDef def = new BooleanColumnDef.Builder()
      .setColumnName("enabled")
      .build();

    assertThat(def.getName()).isEqualTo("enabled");
    assertThat(def.isNullable()).isTrue();
    assertThat(def.getDefaultValue()).isNull();
  }

  @Test
  public void generate_sql_type() {
    BooleanColumnDef def = new BooleanColumnDef.Builder()
      .setColumnName("enabled")
      .setIsNullable(true)
      .build();

    assertThat(def.generateSqlType(new H2())).isEqualTo("BOOLEAN");
    assertThat(def.generateSqlType(new PostgreSql())).isEqualTo("BOOLEAN");
    assertThat(def.generateSqlType(new MsSql())).isEqualTo("BIT");
    assertThat(def.generateSqlType(new MySql())).isEqualTo("TINYINT(1)");
    assertThat(def.generateSqlType(new Oracle())).isEqualTo("NUMBER(1)");
  }

  @Test
  public void fail_with_NPE_if_name_is_null() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Column name cannot be null");

    new BooleanColumnDef.Builder().setColumnName(null);
  }

  @Test
  public void fail_with_NPE_if_no_name() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Column name cannot be null");

    new BooleanColumnDef.Builder().build();
  }


}
