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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SetRulesProfilesIsBuiltInToFalseTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(SetRulesProfilesIsBuiltInToFalseTest.class, "initial.sql");

  private SetRulesProfilesIsBuiltInToFalse underTest = new SetRulesProfilesIsBuiltInToFalse(db.database());

  @Test
  public void has_no_effect_if_table_RULES_PROFILES_is_empty() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("rules_profiles")).isEqualTo(0);
  }

  @Test
  public void updates_rows_with_null_is_build_in_column_to_false() throws SQLException {
    insertRow(1, null);
    insertRow(2, null);

    assertThat(countRowsWitValue(null)).isEqualTo(2);
    assertThat(countRowsWitValue(false)).isEqualTo(0);

    underTest.execute();

    assertThat(countRowsWitValue(null)).isEqualTo(0);
    assertThat(countRowsWitValue(false)).isEqualTo(2);
  }

  @Test
  public void support_large_number_of_rows() throws SQLException {
    for (int i = 0; i < 2_000; i++) {
      insertRow(i, null);
    }

    underTest.execute();

    assertThat(countRowsWitValue(null)).isEqualTo(0);
    assertThat(countRowsWitValue(false)).isEqualTo(2_000);
  }

  @Test
  public void execute_is_reentreant() throws SQLException {
    insertRow(1, null);
    insertRow(2, false);

    underTest.execute();

    underTest.execute();

    assertThat(countRowsWitValue(null)).isEqualTo(0);
    assertThat(countRowsWitValue(false)).isEqualTo(2);
  }

  private int countRowsWitValue(@Nullable Boolean value) {
    if (value == null) {
      return db.countSql("select count(1) from rules_profiles where is_built_in is null");
    }
    return db.countSql("select count(1) from rules_profiles where is_built_in=" + value);
  }

  private void insertRow(int id, @Nullable Boolean builtIn) {
    db.executeInsert(
      "RULES_PROFILES",
      "NAME", "name_" + id,
      "ORGANIZATION_UUID", "org_" + id,
      "KEE", "kee" + id,
      "IS_DEFAULT", false,
      "IS_BUILT_IN", builtIn == null ? null : builtIn.toString());
  }
}
