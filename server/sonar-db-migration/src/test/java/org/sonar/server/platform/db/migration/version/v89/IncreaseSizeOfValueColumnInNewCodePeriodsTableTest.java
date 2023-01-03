/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v89;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

public class IncreaseSizeOfValueColumnInNewCodePeriodsTableTest {
  private static final String TABLE_NAME = "new_code_periods";
  private static final String VERY_LONG_BRANCH_NAME = "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijab" +
    "cdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcd" +
    "efghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijxxxxx";
  private final System2 system = System2.INSTANCE;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(IncreaseSizeOfValueColumnInNewCodePeriodsTableTest.class, "schema.sql");

  private final IncreaseSizeOfValueColumnInNewCodePeriodsTable underTest = new IncreaseSizeOfValueColumnInNewCodePeriodsTable(db.database());

  @Test
  public void cannot_insert_long_value_before_migration() {
    assertThatThrownBy(() -> insertNewCodePeriod("1", VERY_LONG_BRANCH_NAME))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void can_insert_long_value_after_migration() throws SQLException {
    underTest.execute();
    assertThat(db.countRowsOfTable(TABLE_NAME)).isZero();

    insertNewCodePeriod("1", VERY_LONG_BRANCH_NAME);

    assertThat(db.countRowsOfTable(TABLE_NAME)).isEqualTo(1);
  }

  @Test
  public void existing_entries_are_not_affected() throws SQLException {
    insertNewCodePeriod("1", "branch1");
    insertNewCodePeriod("2", null);

    underTest.execute();

    assertThat(db.select("select uuid as \"UUID\", value as \"VALUE\"from new_code_periods"))
      .extracting(r -> r.get("UUID"), r -> r.get("VALUE"))
      .containsExactlyInAnyOrder(
        tuple("1", "branch1"),
        tuple("2", null));
  }

  private void insertNewCodePeriod(String uuid, @Nullable String value) {
    long now = system.now();
    db.executeInsert("NEW_CODE_PERIODS",
      "UUID", uuid,
      "PROJECT_UUID", "proj-" + uuid,
      "BRANCH_UUID", "branch-1",
      "TYPE", "REFERENCE_BRANCH",
      "VALUE", value,
      "UPDATED_AT", now,
      "CREATED_AT", now);
  }
}
