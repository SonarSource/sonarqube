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
package org.sonar.server.platform.db.migration.version.v61;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteReportsFromCeQueueTest {

  private static final long NOW = 1_500_000_000_000L;
  private static final String TABLE_NAME = "ce_queue";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteReportsFromCeQueueTest.class, "schema.sql");

  private DeleteReportsFromCeQueue underTest = new DeleteReportsFromCeQueue(db.database());

  @Test
  public void no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_NAME)).isEqualTo(0);
  }

  @Test
  public void delete_tasks_with_type_REPORT_only() throws SQLException {
    db.executeInsert("ce_queue", "uuid", "U1", "task_type", "REPORT", "status", "PENDING", "created_at", NOW, "updated_at", NOW);
    db.executeInsert("ce_queue", "uuid", "U2", "task_type", "REFRESH_VIEWS", "status", "PENDING", "created_at", NOW, "updated_at", NOW);

    underTest.execute();

    List<Map<String, Object>> uuids = db.select("select uuid as \"uuid\" from ce_queue");
    assertThat(uuids).hasSize(1);
    assertThat(uuids.get(0).get("uuid")).isEqualTo("U2");
  }
}
