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
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TinyIntColumnDefTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void build_string_column_def() {
    TinyIntColumnDef def = new TinyIntColumnDef.Builder()
      .setColumnName("foo")
      .setIsNullable(true)
      .build();

    assertThat(def.getName()).isEqualTo("foo");
    assertThat(def.isNullable()).isTrue();
    assertThat(def.getDefaultValue()).isNull();
  }

  @Test
  public void generate_sql_type() {
    TinyIntColumnDef def = new TinyIntColumnDef.Builder()
      .setColumnName("foo")
      .setIsNullable(true)
      .build();

    assertThat(def.generateSqlType(new H2())).isEqualTo("TINYINT");
    assertThat(def.generateSqlType(new PostgreSql())).isEqualTo("SMALLINT");
    assertThat(def.generateSqlType(new MsSql())).isEqualTo("TINYINT");
    assertThat(def.generateSqlType(new MySql())).isEqualTo("TINYINT(2)");
    assertThat(def.generateSqlType(new Oracle())).isEqualTo("NUMBER(3)");
  }

  @Test
  public void fail_with_UOE_to_generate_sql_type_when_unknown_dialect() {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("Unknown dialect 'unknown'");

    TinyIntColumnDef def = new TinyIntColumnDef.Builder()
      .setColumnName("foo")
      .setIsNullable(true)
      .build();

    Dialect dialect = mock(Dialect.class);
    when(dialect.getId()).thenReturn("unknown");
    def.generateSqlType(dialect);
  }
}
