/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateIsDismissibleColumnOfCeTaskMessageTableTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateIsDismissibleColumnOfCeTaskMessageTableTest.class, "schema.sql");

  private PopulateIsDismissibleColumnOfCeTaskMessageTable underTest = new PopulateIsDismissibleColumnOfCeTaskMessageTable(db.database());

  @Test
  public void execute_migration() throws SQLException {
    insertCeTaskMessage(null);
    insertCeTaskMessage(null);
    insertCeTaskMessage(null);

    underTest.execute();

    assertIsDismissibleValuesAreAllFalse();
  }

  @Test
  public void migrate_not_already_updated_rows() throws SQLException {
    insertCeTaskMessage(false);
    insertCeTaskMessage(false);
    insertCeTaskMessage(null);

    underTest.execute();

    assertIsDismissibleValuesAreAllFalse();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertCeTaskMessage(null);

    underTest.execute();
    underTest.execute();

    assertIsDismissibleValuesAreAllFalse();
  }

  private void assertIsDismissibleValuesAreAllFalse() {
    assertThat(db.select("select is_dismissible as \"IS_DISMISSIBLE\" from ce_task_message")
      .stream()
      .map(rows -> rows.get("IS_DISMISSIBLE")))
      .containsOnly(false);
  }

  private String insertCeTaskMessage(@Nullable Boolean isDissmisible) {
    String uuid = Uuids.createFast();
    db.executeInsert("CE_TASK_MESSAGE",
      "UUID", uuid,
      "TASK_UUID", Uuids.createFast(),
      "MESSAGE", "message-" + uuid,
      "IS_DISMISSIBLE", isDissmisible,
      "CREATED_AT", System.currentTimeMillis());
    return uuid;
  }
}
