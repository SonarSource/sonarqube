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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

public class RecreateIndexEventsComponentUuidTest {
  private static final String TABLE_EVENTS = "events";
  private static final String INDEX_EVENTS_COMPONENT_UUID = "events_component_uuid";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(RecreateIndexEventsComponentUuidTest.class, "events.sql");

  private RecreateIndexEventsComponentUuid underTest = new RecreateIndexEventsComponentUuid(db.database());

  @Test
  public void execute_adds_index_EVENTS_COMPONENT_UUID() throws SQLException {
    db.assertIndexDoesNotExist(TABLE_EVENTS, INDEX_EVENTS_COMPONENT_UUID);

    underTest.execute();

    db.assertIndex(TABLE_EVENTS, INDEX_EVENTS_COMPONENT_UUID, "component_uuid");
  }
}
