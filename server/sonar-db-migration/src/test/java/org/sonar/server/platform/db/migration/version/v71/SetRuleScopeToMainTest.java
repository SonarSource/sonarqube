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
package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SetRuleScopeToMainTest {
  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(SetRuleScopeToMainTest.class, "rules.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private System2 system = new System2();

  private SetRuleScopeToMain underTest = new SetRuleScopeToMain(dbTester.database(), system);

  @Test
  public void has_no_effect_if_table_rules_is_empty() throws SQLException {
    underTest.execute();

    assertThat(dbTester.countRowsOfTable("rules")).isEqualTo(0);
  }

  @Test
  public void updates_rows_with_null_is_build_in_column_to_false() throws SQLException {
    insertRow(1, null);
    insertRow(2, null);

    assertThat(countRowsWithValue(null)).isEqualTo(2);
    assertThat(countRowsWithValue(RuleScope.MAIN)).isEqualTo(0);

    underTest.execute();

    assertThat(countRowsWithValue(null)).isEqualTo(0);
    assertThat(countRowsWithValue(RuleScope.MAIN)).isEqualTo(2);
  }

  @Test
  public void execute_is_reentreant() throws SQLException {
    insertRow(1, null);
    insertRow(2, RuleScope.MAIN);

    underTest.execute();

    underTest.execute();

    assertThat(countRowsWithValue(null)).isEqualTo(0);
    assertThat(countRowsWithValue(RuleScope.MAIN)).isEqualTo(2);
  }

  private int countRowsWithValue(@Nullable RuleScope value) {
    if (value == null) {
      return dbTester.countSql("select count(1) from rules where scope is null");
    }
    return dbTester.countSql("select count(1) from rules where scope='" + value + "'");
  }

  private void insertRow(int id, @Nullable RuleScope scope) {
    dbTester.executeInsert(
      "RULES",
      "PLUGIN_RULE_KEY", "key_" + id,
      "PLUGIN_NAME", "name_" + id,
      "SCOPE", scope == null ? null : scope.name());
  }
}
