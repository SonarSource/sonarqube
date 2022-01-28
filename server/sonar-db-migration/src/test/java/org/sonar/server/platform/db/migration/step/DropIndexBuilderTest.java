/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.step;

import java.util.List;
import org.junit.Test;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DropIndexBuilderTest {


  @Test
  public void drop_index_in_table() {
    verifySql(new H2(), "DROP INDEX IF EXISTS issues_key");
    verifySql(new MsSql(), "DROP INDEX issues_key ON issues");
    verifySql(new Oracle(), "DROP INDEX issues_key");
    verifySql(new PostgreSql(), "DROP INDEX IF EXISTS issues_key");
  }

  private static void verifySql(Dialect dialect, String expectedSql) {
    List<String> actual = new DropIndexBuilder(dialect)
      .setTable("issues")
      .setName("issues_key")
      .build();
    assertThat(actual).containsExactly(expectedSql);
  }

  @Test
  public void throw_NPE_if_table_name_is_missing() {
    assertThatThrownBy(() -> {
      new DropIndexBuilder(new H2())
        .setName("issues_key")
        .build();
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Table name can't be null");
  }

  @Test
  public void throw_IAE_if_table_name_is_not_valid() {
    assertThatThrownBy(() -> {
      new DropIndexBuilder(new H2())
        .setTable("(not valid)")
        .setName("issues_key")
        .build();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Table name must be lower case and contain only alphanumeric chars or '_', got '(not valid)'");
  }

  @Test
  public void throw_NPE_if_index_name_is_missing() {
    assertThatThrownBy(() -> {
      new DropIndexBuilder(new H2())
        .setTable("issues")
        .build();
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Index name can't be null");
  }

  @Test
  public void throw_IAE_if_index_name_is_not_valid() {
    assertThatThrownBy(() -> {
      new DropIndexBuilder(new H2())
        .setTable("issues")
        .setName("(not valid)")
        .build();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Index name must contain only alphanumeric chars or '_', got '(not valid)'");
  }
}
