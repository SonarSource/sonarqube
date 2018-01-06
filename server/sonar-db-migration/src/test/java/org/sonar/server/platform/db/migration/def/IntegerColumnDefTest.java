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
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;

public class IntegerColumnDefTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private IntegerColumnDef underTest = newIntegerColumnDefBuilder().setColumnName("a").build();

  @Test
  public void builder_setColumnName_throws_IAE_if_name_is_not_lowercase() {
    IntegerColumnDef.Builder builder = newIntegerColumnDefBuilder();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Column name must be lower case and contain only alphanumeric chars or '_', got 'T'");
    builder.setColumnName("T");
  }

  @Test
  public void builder_build_throws_NPE_if_no_name_was_set() {
    IntegerColumnDef.Builder builder = newIntegerColumnDefBuilder();

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Column name cannot be null");

    builder.build();
  }

  @Test
  public void integerColumDef_is_nullable_by_default() {
    assertThat(newIntegerColumnDefBuilder().setColumnName("a").build().isNullable()).isTrue();
  }

  @Test
  public void builder_setNullable_sets_nullable_field_of_IntegerColumnDef() {
    assertThat(newIntegerColumnDefBuilder().setColumnName("a").setIsNullable(true).build().isNullable()).isTrue();
    assertThat(newIntegerColumnDefBuilder().setColumnName("a").setIsNullable(false).build().isNullable()).isFalse();
  }

  @Test
  public void builder_setColumnName_sets_name_field_of_IntegerColumnDef() {
    assertThat(newIntegerColumnDefBuilder().setColumnName("a").build().getName()).isEqualTo("a");
  }

  @Test
  public void builder_setDefaultValue_sets_default_value_field_of_IntegerColumnDef() {
    assertThat(newIntegerColumnDefBuilder().setColumnName("a").setDefaultValue(42).build().getDefaultValue()).isEqualTo(42);
  }

  @Test
  public void default_value_is_null_by_default() {
    assertThat(newIntegerColumnDefBuilder().setColumnName("a").build().getDefaultValue()).isNull();
  }

  @Test
  public void generateSqlType_for_MsSql() {
    assertThat(underTest.generateSqlType(new MsSql())).isEqualTo("INT");
  }

  @Test
  public void generateSqlType_for_MySql() {
    assertThat(underTest.generateSqlType(new MySql())).isEqualTo("INTEGER");
  }

  @Test
  public void generateSqlType_for_Oracle() {
    assertThat(underTest.generateSqlType(new Oracle())).isEqualTo("NUMBER(38,0)");
  }

  @Test
  public void generateSqlType_for_H2() {
    assertThat(underTest.generateSqlType(new H2())).isEqualTo("INTEGER");
  }

  @Test
  public void generateSqlType_for_PostgreSql() {
    assertThat(underTest.generateSqlType(new PostgreSql())).isEqualTo("INTEGER");
  }

  @Test
  public void generateSqlType_thows_IAE_for_unknown_dialect() {
    Dialect dialect = mock(Dialect.class);
    when(dialect.getId()).thenReturn("AAA");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unsupported dialect id AAA");

    underTest.generateSqlType(dialect);
  }

}
