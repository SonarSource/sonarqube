/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.db.migration.def.BlobColumnDef.newBlobColumnDefBuilder;

public class BlobColumnDefTest {

  private BlobColumnDef underTest = newBlobColumnDefBuilder().setColumnName("a").build();

  @Test
  public void builder_setColumnName_throws_IAE_if_name_is_not_lowercase() {
    BlobColumnDef.Builder builder = newBlobColumnDefBuilder();

    assertThatThrownBy(() -> builder.setColumnName("T"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Column name must be lower case and contain only alphanumeric chars or '_', got 'T'");
  }

  @Test
  public void builder_build_throws_NPE_if_no_name_was_set() {
    BlobColumnDef.Builder builder = newBlobColumnDefBuilder();

    assertThatThrownBy(builder::build)
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Column name cannot be null");
  }

  @Test
  public void blobColumDef_is_nullable_by_default() {
    assertThat(newBlobColumnDefBuilder().setColumnName("a").build().isNullable()).isTrue();
  }

  @Test
  public void builder_setNullable_sets_nullable_field_of_BlobColumnDef() {
    assertThat(newBlobColumnDefBuilder().setColumnName("a").setIsNullable(true).build().isNullable()).isTrue();
    assertThat(newBlobColumnDefBuilder().setColumnName("a").setIsNullable(false).build().isNullable()).isFalse();
  }

  @Test
  public void builder_setColumnName_sets_name_field_of_BlobColumnDef() {
    assertThat(newBlobColumnDefBuilder().setColumnName("a").build().getName()).isEqualTo("a");
  }

  @Test
  public void generateSqlType_for_MsSql() {
    assertThat(underTest.generateSqlType(new MsSql())).isEqualTo("VARBINARY(MAX)");
  }

  @Test
  public void generateSqlType_for_Oracle() {
    assertThat(underTest.generateSqlType(new Oracle())).isEqualTo("BLOB");
  }

  @Test
  public void generateSqlType_for_H2() {
    assertThat(underTest.generateSqlType(new H2())).isEqualTo("BLOB");
  }

  @Test
  public void generateSqlType_for_PostgreSql() {
    assertThat(underTest.generateSqlType(new PostgreSql())).isEqualTo("BYTEA");
  }

  @Test
  public void generateSqlType_thows_IAE_for_unknown_dialect() {
    Dialect dialect = mock(Dialect.class);
    when(dialect.getId()).thenReturn("AAA");

    assertThatThrownBy(() -> underTest.generateSqlType(dialect))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unsupported dialect id AAA");
  }
}
