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
package org.sonar.server.platform.db.migration.sql;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateIndexBuilderTest {
  @Test
  public void create_index_on_single_column() {
    verifySql(new CreateIndexBuilder(new Oracle())
        .setTable("issues")
        .setName("issues_key")
        .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(10).build()),
      "CREATE INDEX issues_key ON issues (kee)");
  }

  @Test
  public void create_unique_index_on_single_column() {
    verifySql(new CreateIndexBuilder(new Oracle())
        .setTable("issues")
        .setName("issues_key")
        .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(10).build())
        .setUnique(true),
      "CREATE UNIQUE INDEX issues_key ON issues (kee)");
  }

  @Test
  public void create_index_on_multiple_columns() {
    verifySql(new CreateIndexBuilder(new Oracle())
        .setTable("rules")
        .setName("rules_key")
        .addColumn(newVarcharColumnDefBuilder().setColumnName("repository").setLimit(10).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("rule_key").setLimit(50).build()),
      "CREATE INDEX rules_key ON rules (repository, rule_key)");
  }

  @Test
  public void create_unique_index_on_multiple_columns() {
    verifySql(new CreateIndexBuilder(new Oracle())
        .setTable("rules")
        .setName("rules_key")
        .addColumn(newVarcharColumnDefBuilder().setColumnName("repository").setLimit(10).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("rule_key").setLimit(50).build())
        .setUnique(true),
      "CREATE UNIQUE INDEX rules_key ON rules (repository, rule_key)");
  }

  @Test
  public void build_whenDialectH2_shouldHaveNullsNotDistinctClause() {
    verifySql(new CreateIndexBuilder(new H2())
        .setTable("rules")
        .setName("rules_key")
        .addColumn(newVarcharColumnDefBuilder().setColumnName("repository").setLimit(10).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("rule_key").setLimit(50).build())
        .setUnique(true),
      "CREATE UNIQUE NULLS NOT DISTINCT INDEX rules_key ON rules (repository, rule_key)");
  }

  @Test
  public void build_whenDialectPostgres15_shouldHaveNullsNotDistinctClause() throws SQLException {
    PostgreSql postgreSql = new PostgreSql();
    getMetadataForDbVersion(15, 0);
    postgreSql.init(getMetadataForDbVersion(15, 0));

    verifySql(new CreateIndexBuilder(postgreSql)
        .setTable("rules")
        .setName("rules_key")
        .addColumn(newVarcharColumnDefBuilder().setColumnName("repository").setLimit(10).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("rule_key").setLimit(50).build())
        .setUnique(true),
      "CREATE UNIQUE INDEX rules_key ON rules (repository, rule_key) NULLS NOT DISTINCT");
  }

  @Test
  public void build_whenDialectPostgres14OrLower_shouldHaveCoalesceConditionsOnNullableColumns() throws SQLException {
    PostgreSql postgreSql = new PostgreSql();
    getMetadataForDbVersion(14, 0);
    postgreSql.init(getMetadataForDbVersion(14, 0));

    verifySql(new CreateIndexBuilder(postgreSql)
        .setTable("rules")
        .setName("rules_key")
        .addColumn(newVarcharColumnDefBuilder().setColumnName("repository").setLimit(10).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("rule_key").setLimit(50).setIsNullable(false).build())
        .setUnique(true),
      "CREATE UNIQUE INDEX rules_key ON rules (COALESCE(repository, ''), rule_key)");
  }

  private static DatabaseMetaData getMetadataForDbVersion(int major, int minor) throws SQLException {
    DatabaseMetaData databaseMetaData = Mockito.mock(DatabaseMetaData.class);
    Mockito.when(databaseMetaData.getDatabaseMajorVersion()).thenReturn(major);
    Mockito.when(databaseMetaData.getDatabaseMinorVersion()).thenReturn(minor);
    return databaseMetaData;
  }


  @Test
  public void index_length_is_not_specified_on_big_varchar_columns() {
    verifySql(new CreateIndexBuilder(new Oracle())
        .setTable("issues")
        .setName("issues_key")
        .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(4000).build()),
      "CREATE INDEX issues_key ON issues (kee)");
  }

  @Test
  public void throw_NPE_if_table_is_missing() {
    CreateIndexBuilder builder = new CreateIndexBuilder(new Oracle())
      .setName("issues_key")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(10).build());
    assertThatThrownBy(builder::build)
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Table name can't be null");
  }

  @Test
  public void throw_NPE_if_index_name_is_missing() {
    CreateIndexBuilder builder = new CreateIndexBuilder(new H2())
      .setTable("issues")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(10).build());
    assertThatThrownBy(builder::build)
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Index name can't be null");
  }

  @Test
  public void build_shouldThrowException_whenUniqueAndColumnNullabilityIsNotProvided() {
    CreateIndexBuilder builder = new CreateIndexBuilder(new H2())
      .setTable("issues")
      .setName("name")
      .addColumn("columnName")
      .setUnique(true);
    assertThatThrownBy(builder::build)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Nullability of column should be provided for unique indexes");
  }

  @Test
  public void throw_IAE_if_columns_are_missing() {
    CreateIndexBuilder builder = new CreateIndexBuilder(new H2())
      .setTable("issues")
      .setName("issues_key");
    assertThatThrownBy(builder::build)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("at least one column must be specified");
  }

  @Test
  public void throw_IAE_if_table_name_is_not_valid() {
    CreateIndexBuilder builder = new CreateIndexBuilder(new H2())
      .setTable("(not valid)")
      .setName("issues_key")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(10).build());
    assertThatThrownBy(builder::build)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Table name must be lower case and contain only alphanumeric chars or '_', got '(not valid)'");
  }

  @Test
  public void throw_NPE_when_adding_null_column() {
    CreateIndexBuilder builder = new CreateIndexBuilder(new H2())
      .setTable("issues")
      .setName("issues_key");
    assertThatThrownBy(() -> builder.addColumn((String) null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Column cannot be null");
  }

  private static void verifySql(CreateIndexBuilder builder, String expectedSql) {
    List<String> actual = builder.build();
    assertThat(actual).containsExactly(expectedSql);
  }

}
