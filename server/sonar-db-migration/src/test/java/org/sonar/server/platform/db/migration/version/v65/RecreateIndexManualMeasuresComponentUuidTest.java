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

public class RecreateIndexManualMeasuresComponentUuidTest {
  private static final String TABLE_MANUAL_MEASURES = "manual_measures";
  private static final String INDEX_MANUAL_MEASURES_COMPONENT_UUID = "manual_measures_component_uuid";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(RecreateIndexManualMeasuresComponentUuidTest.class, "manual_measures.sql");

  private RecreateIndexManualMeasuresComponentUuid underTest = new RecreateIndexManualMeasuresComponentUuid(db.database());

  @Test
  public void execute_adds_index_EVENTS_COMPONENT_UUID() throws SQLException {
    db.assertIndexDoesNotExist(TABLE_MANUAL_MEASURES, INDEX_MANUAL_MEASURES_COMPONENT_UUID);

    underTest.execute();

    db.assertIndex(TABLE_MANUAL_MEASURES, INDEX_MANUAL_MEASURES_COMPONENT_UUID, "component_uuid");
  }
}
