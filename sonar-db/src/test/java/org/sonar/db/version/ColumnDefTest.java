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
import static org.sonar.db.version.ColumnDef.Type.BIG_INTEGER;
import static org.sonar.db.version.ColumnDef.Type.STRING;

public class ColumnDefTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void create_column_def() throws Exception {
    ColumnDef def = new ColumnDef()
      .setName("date_in_ms")
      .setType(STRING)
      .setLimit(10)
      .setNullable(true);

    assertThat(def.getName()).isEqualTo("date_in_ms");
    assertThat(def.getType()).isEqualTo(STRING);
    assertThat(def.getLimit()).isEqualTo(10);
    assertThat(def.isNullable()).isTrue();
  }

  @Test
  public void convert_varchar_type_to_sql() throws Exception {
    assertThat(new ColumnDef().setType(STRING).getSqlType(new H2())).isEqualTo("VARCHAR");
    assertThat(new ColumnDef().setType(STRING).getSqlType(new Oracle())).isEqualTo("VARCHAR");
    assertThat(new ColumnDef().setType(STRING).getSqlType(new MsSql())).isEqualTo("VARCHAR");
    assertThat(new ColumnDef().setType(STRING).getSqlType(new MySql())).isEqualTo("VARCHAR");
  }

  @Test
  public void convert_big_integer_type_to_sql() throws Exception {
    assertThat(new ColumnDef().setType(BIG_INTEGER).getSqlType(new H2())).isEqualTo("BIGINT");
    assertThat(new ColumnDef().setType(BIG_INTEGER).getSqlType(new Oracle())).isEqualTo("NUMBER (38)");
    assertThat(new ColumnDef().setType(BIG_INTEGER).getSqlType(new MsSql())).isEqualTo("BIGINT");
    assertThat(new ColumnDef().setType(BIG_INTEGER).getSqlType(new MySql())).isEqualTo("BIGINT");
  }


  @Test
  public void fail_when_column_name_is_in_upper_case() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Column name should only contains lowercase and _ characters");

    new ColumnDef()
      .setName("DATE_IN_MS")
      .setType(BIG_INTEGER)
      .setNullable(true);
  }

  @Test
  public void fail_when_column_name_contains_invalid_character() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Column name should only contains lowercase and _ characters");

    new ColumnDef()
      .setName("date-in/ms")
      .setType(BIG_INTEGER)
      .setNullable(true);
  }
}
