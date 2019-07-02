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
package org.sonar.server.platform.db.migration.sql;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateIndexBuilderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_index_on_single_column() {
    verifySql(new CreateIndexBuilder()
      .setTable("issues")
      .setName("issues_key")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(10).build()),
      "CREATE INDEX issues_key ON issues (kee)");
  }

  @Test
  public void create_unique_index_on_single_column() {
    verifySql(new CreateIndexBuilder()
      .setTable("issues")
      .setName("issues_key")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(10).build())
      .setUnique(true),
      "CREATE UNIQUE INDEX issues_key ON issues (kee)");
  }

  @Test
  public void create_index_on_multiple_columns() {
    verifySql(new CreateIndexBuilder()
      .setTable("rules")
      .setName("rules_key")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("repository").setLimit(10).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("rule_key").setLimit(50).build()),
      "CREATE INDEX rules_key ON rules (repository, rule_key)");
  }

  @Test
  public void create_unique_index_on_multiple_columns() {
    verifySql(new CreateIndexBuilder()
      .setTable("rules")
      .setName("rules_key")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("repository").setLimit(10).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("rule_key").setLimit(50).build())
      .setUnique(true),
      "CREATE UNIQUE INDEX rules_key ON rules (repository, rule_key)");
  }

  @Test
  public void index_length_is_not_specified_on_big_varchar_columns() {
    verifySql(new CreateIndexBuilder()
      .setTable("issues")
      .setName("issues_key")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(4000).build()),
      "CREATE INDEX issues_key ON issues (kee)");
  }

  @Test
  public void throw_NPE_if_table_is_missing() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Table name cannot be null");

    new CreateIndexBuilder()
      .setName("issues_key")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(10).build())
      .build();
  }

  @Test
  public void throw_NPE_if_index_name_is_missing() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Index name cannot be null");

    new CreateIndexBuilder()
      .setTable("issues")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(10).build())
      .build();
  }

  @Test
  public void throw_IAE_if_columns_are_missing() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("at least one column must be specified");

    new CreateIndexBuilder()
      .setTable("issues")
      .setName("issues_key")
      .build();
  }

  @Test
  public void throw_IAE_if_table_name_is_not_valid() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Table name must be lower case and contain only alphanumeric chars or '_', got '(not valid)'");

    new CreateIndexBuilder()
      .setTable("(not valid)")
      .setName("issues_key")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(10).build())
      .build();
  }

  @Test
  public void throw_NPE_when_adding_null_column() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Column cannot be null");

    new CreateIndexBuilder()
      .setTable("issues")
      .setName("issues_key")
      .addColumn(null)
      .build();
  }

  private static void verifySql(CreateIndexBuilder builder, String expectedSql) {
    List<String> actual = builder.build();
    assertThat(actual).containsExactly(expectedSql);
  }

}
