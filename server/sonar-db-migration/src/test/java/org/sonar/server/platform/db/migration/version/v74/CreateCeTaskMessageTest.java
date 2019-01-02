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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;
import static org.assertj.core.api.Assertions.assertThat;

public class CreateCeTaskMessageTest {

  private static final String TABLE = "ce_task_message";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(CreateCeTaskMessageTest.class, "empty.sql");

  private CreateCeTaskMessage underTest = new CreateCeTaskMessage(db.database());

  @Test
  public void creates_table_on_empty_db() throws SQLException {
    underTest.execute();

    checkTable();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    checkTable();
  }

  private void checkTable() {
    assertThat(db.countRowsOfTable(TABLE)).isEqualTo(0);

    db.assertColumnDefinition(TABLE, "uuid", VARCHAR, 40, false);
    db.assertPrimaryKey(TABLE, "pk_" + TABLE, "uuid");
    db.assertColumnDefinition(TABLE, "task_uuid", VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE, "message", VARCHAR, 4000, false);
    db.assertColumnDefinition(TABLE, "created_at", BIGINT, null, false);

    db.assertIndex(TABLE, TABLE + "_task", "task_uuid");
  }


}
