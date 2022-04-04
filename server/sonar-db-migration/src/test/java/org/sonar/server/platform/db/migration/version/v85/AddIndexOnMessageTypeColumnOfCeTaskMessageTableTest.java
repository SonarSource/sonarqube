/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class AddIndexOnMessageTypeColumnOfCeTaskMessageTableTest {
  private static final String TABLE_NAME = "ce_task_message";
  private static final String INDEX_NAME = "ctm_message_type";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddIndexOnMessageTypeColumnOfCeTaskMessageTableTest.class, "schema.sql");

  DdlChange underTest = new AddIndexOnMessageTypeColumnOfCeTaskMessageTable(db.database());

  @Test
  public void add_index() throws SQLException {
    underTest.execute();

    db.assertIndex(TABLE_NAME, INDEX_NAME, "message_type");
  }
}
