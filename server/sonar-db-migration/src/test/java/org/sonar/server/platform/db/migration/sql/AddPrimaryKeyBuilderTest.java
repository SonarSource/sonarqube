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
package org.sonar.server.platform.db.migration.sql;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AddPrimaryKeyBuilderTest {

  private static final String TABLE_NAME = "issues";

  @Test
  public void generate() {
    String sql = new AddPrimaryKeyBuilder(TABLE_NAME, "id").build();

    assertThat(sql).isEqualTo("ALTER TABLE issues ADD CONSTRAINT pk_issues PRIMARY KEY (id)");
  }

  @Test
  public void fail_when_table_name_is_invalid() {
    assertThatThrownBy(() -> new AddPrimaryKeyBuilder("abcdefghijklmnopqrstuvwxyz", "id"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_primary_key_column_is_invalid() {
    AddPrimaryKeyBuilder builder = new AddPrimaryKeyBuilder("my_table", null);
    assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalStateException.class);
  }
}
