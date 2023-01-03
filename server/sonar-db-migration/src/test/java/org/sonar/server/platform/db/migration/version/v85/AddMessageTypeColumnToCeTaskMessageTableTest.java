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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;

public class AddMessageTypeColumnToCeTaskMessageTableTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddMessageTypeColumnToCeTaskMessageTableTest.class, "schema.sql");

  private final DdlChange underTest = new AddMessageTypeColumnToCeTaskMessageTable(db.database());

  @Before
  public void setup() {
    insertTaskMessage(1L, "a1");
    insertTaskMessage(2L, "a2");
    insertTaskMessage(3L, "a3");
  }

  @Test
  public void add_message_type_column_to_ce_task_message_table() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("ce_task_message", "message_type", Types.VARCHAR, 255, true);

    assertThat(db.countSql("select count(uuid) from ce_task_message")).isEqualTo(3);
  }

  private void insertTaskMessage(Long id, String message) {
    db.executeInsert("ce_task_message",
      "uuid", "uuid-" + id,
      "task_uuid", "task-uuid-" + id,
      "message", message,
      "created_at", System2.INSTANCE.now());
  }
}
