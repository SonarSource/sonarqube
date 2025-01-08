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

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DeduplicateTableBuilderTest {

  @Test
  public void build_shouldReturnExpectedSql() {
    List<String> queries = new DeduplicateTableBuilder("example_table")
      .addReferenceColumn("reference_column1")
      .addReferenceColumn("reference_column2")
      .setIdentityColumn("identity_column")
      .build();

    Assertions.assertThat(queries)
      .containsExactly("delete from example_table where identity_column not in (select min(identity_column) from example_table group by reference_column1, reference_column2)");
  }

  @Test
  public void build_shouldThrowException_whenIdentityColumnUndefined() {
    DeduplicateTableBuilder builder = new DeduplicateTableBuilder("example_table")
      .addReferenceColumn("reference_column1")
      .addReferenceColumn("reference_column2");

    Assertions.assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class)
      .hasMessage("Column name cannot be null");
  }

  @Test
  public void build_shouldThrowException_whenReferenceColumnUndefined() {
    DeduplicateTableBuilder builder = new DeduplicateTableBuilder("example_table")
      .setIdentityColumn("identity_column");

    Assertions.assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class)
      .hasMessage("At least one reference column must be specified");
  }

}
