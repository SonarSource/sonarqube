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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateMessageTypeColumnOfCeTaskMessageTableTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateMessageTypeColumnOfCeTaskMessageTableTest.class, "schema.sql");

  private final DataChange underTest = new PopulateMessageTypeColumnOfCeTaskMessageTable(db.database());

  @Test
  public void add_message_type_column_on_empty_table() throws SQLException {
    underTest.execute();

    assertThat(db.countSql("select count(*) from ce_task_message")).isZero();
  }

  @Test
  public void add_message_type_column_on_non_empty_table() throws SQLException {
    insertTaskMessage(1L, "msg1");
    insertTaskMessage(2L, "msg2");
    insertTaskMessage(3L, "msg3");
    insertTaskMessage(4L, "msg4");

    underTest.execute();

    assertThat(db.countSql("select count(*) from ce_task_message")).isEqualTo(4);
    assertThat(db.select("select uuid, task_uuid, message, message_type from ce_task_message"))
      .extracting(m -> m.get("UUID"), m -> m.get("TASK_UUID"), m -> m.get("MESSAGE"), m -> m.get("MESSAGE_TYPE"))
      .containsExactlyInAnyOrder(
        tuple("uuid-1", "task-uuid-1", "msg1", "GENERIC"),
        tuple("uuid-2", "task-uuid-2", "msg2", "GENERIC"),
        tuple("uuid-3", "task-uuid-3", "msg3", "GENERIC"),
        tuple("uuid-4", "task-uuid-4", "msg4", "GENERIC"));
  }

  private void insertTaskMessage(Long id, String message) {
    db.executeInsert("ce_task_message",
      "uuid", "uuid-" + id,
      "task_uuid", "task-uuid-" + id,
      "message", message,
      "created_at", System2.INSTANCE.now());
  }

}
