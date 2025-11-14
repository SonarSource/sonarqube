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

import java.util.List;
import org.junit.Test;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateTableAsBuilderTest {

  @Test
  public void create_table() {
    String createTableAs = "CREATE TABLE issues_copy (rule_uuid) AS (SELECT rule_uuid FROM issues)";
    String selectInto = "SELECT rule_uuid INTO issues_copy FROM issues";

    verifySql(new H2(), createTableAs, "ALTER TABLE issues_copy ALTER COLUMN rule_uuid VARCHAR (40) NOT NULL");
    verifySql(new MsSql(), selectInto, "ALTER TABLE issues_copy ALTER COLUMN rule_uuid NVARCHAR (40) NOT NULL");
    verifySql(new Oracle(), createTableAs, "ALTER TABLE issues_copy MODIFY (rule_uuid VARCHAR2 (40 CHAR) NOT NULL)");
    verifySql(new PostgreSql(), createTableAs, "ALTER TABLE issues_copy ALTER COLUMN rule_uuid TYPE VARCHAR (40), ALTER COLUMN rule_uuid SET NOT NULL");
  }

  @Test
  public void create_table_with_cast() {
    verifySqlWithCast(new H2(), "CREATE TABLE issues_copy (rule_uuid) AS (SELECT CAST (rule_id AS VARCHAR (40)) AS rule_uuid FROM issues)",
      "ALTER TABLE issues_copy ALTER COLUMN rule_uuid VARCHAR (40) NOT NULL");
    verifySqlWithCast(new MsSql(), "SELECT CAST (rule_id AS NVARCHAR (40)) AS rule_uuid INTO issues_copy FROM issues",
      "ALTER TABLE issues_copy ALTER COLUMN rule_uuid NVARCHAR (40) NOT NULL");
    verifySqlWithCast(new Oracle(), "CREATE TABLE issues_copy (rule_uuid) AS (SELECT CAST (rule_id AS VARCHAR2 (40 CHAR)) AS rule_uuid FROM issues)",
      "ALTER TABLE issues_copy MODIFY (rule_uuid VARCHAR2 (40 CHAR) NOT NULL)");
    verifySqlWithCast(new PostgreSql(), "CREATE TABLE issues_copy (rule_uuid) AS (SELECT CAST (rule_id AS VARCHAR (40)) AS rule_uuid FROM issues)",
      "ALTER TABLE issues_copy ALTER COLUMN rule_uuid TYPE VARCHAR (40), ALTER COLUMN rule_uuid SET NOT NULL");
  }

  @Test
  public void fail_if_columns_not_set() {
    assertThatThrownBy(() -> new CreateTableAsBuilder(new H2(), "issues_copy", "issues")
      .build())
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void fail_if_table_not_set() {
    assertThatThrownBy(() -> new CreateTableAsBuilder(new H2(), null, "issues")
      .build())
      .isInstanceOf(NullPointerException.class);
  }

  private static void verifySqlWithCast(Dialect dialect, String... expectedSql) {
    List<String> actual = new CreateTableAsBuilder(dialect, "issues_copy", "issues")
      .addColumnWithCast(newVarcharColumnDefBuilder().setColumnName("rule_uuid").setIsNullable(false).setLimit(40).build(), "rule_id")
      .build();
    assertThat(actual).containsExactly(expectedSql);
  }

  private static void verifySql(Dialect dialect, String... expectedSql) {
    List<String> actual = new CreateTableAsBuilder(dialect, "issues_copy", "issues")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("rule_uuid").setIsNullable(false).setLimit(40).build())
      .build();
    assertThat(actual).containsExactly(expectedSql);
  }
}
