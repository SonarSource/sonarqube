/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DecimalColumnDefTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void build_string_column_def() {
    DecimalColumnDef def = new DecimalColumnDef.Builder()
      .setColumnName("issues")
      .setPrecision(30)
      .setScale(20)
      .setIsNullable(true)
      .build();

    assertThat(def.getName()).isEqualTo("issues");
    assertThat(def.getPrecision()).isEqualTo(30);
    assertThat(def.getScale()).isEqualTo(20);
    assertThat(def.isNullable()).isTrue();
    assertThat(def.getDefaultValue()).isNull();
  }

  @Test
  public void fail_with_NPE_if_name_is_null() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Column name cannot be null");

    new DecimalColumnDef.Builder()
      .setColumnName(null);
  }

  @Test
  public void fail_with_NPE_if_no_name() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Column name cannot be null");

    new DecimalColumnDef.Builder()
      .build();
  }

  @Test
  public void default_precision_is_38() {
    DecimalColumnDef def = new DecimalColumnDef.Builder()
      .setColumnName("issues")
      .setScale(20)
      .setIsNullable(true)
      .build();

    assertThat(def.getPrecision()).isEqualTo(38);
  }

  @Test
  public void default_precision_is_20() {
    DecimalColumnDef def = new DecimalColumnDef.Builder()
      .setColumnName("issues")
      .setPrecision(30)
      .setIsNullable(true)
      .build();

    assertThat(def.getScale()).isEqualTo(20);
  }

  @Test
  public void create_builder_with_only_required_attributes() {
    DecimalColumnDef def = new DecimalColumnDef.Builder()
      .setColumnName("issues")
      .build();

    assertThat(def.getPrecision()).isEqualTo(38);
    assertThat(def.getScale()).isEqualTo(20);
    assertThat(def.isNullable()).isTrue();
    assertThat(def.getDefaultValue()).isNull();
  }

  @Test
  public void generate_sql_type() {
    DecimalColumnDef def = new DecimalColumnDef.Builder()
      .setColumnName("issues")
      .setPrecision(30)
      .setScale(20)
      .setIsNullable(true)
      .build();

    assertThat(def.generateSqlType(new H2())).isEqualTo("DOUBLE");
    assertThat(def.generateSqlType(new PostgreSql())).isEqualTo("NUMERIC (30,20)");
    assertThat(def.generateSqlType(new MsSql())).isEqualTo("DECIMAL (30,20)");
    assertThat(def.generateSqlType(new Oracle())).isEqualTo("NUMERIC (30,20)");
  }

  @Test
  public void fail_with_UOE_to_generate_sql_type_when_unknown_dialect() {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("Unknown dialect 'unknown'");

    DecimalColumnDef def = new DecimalColumnDef.Builder()
      .setColumnName("issues")
      .setPrecision(30)
      .setScale(20)
      .setIsNullable(true)
      .build();

    Dialect dialect = mock(Dialect.class);
    when(dialect.getId()).thenReturn("unknown");
    def.generateSqlType(dialect);
  }

}
