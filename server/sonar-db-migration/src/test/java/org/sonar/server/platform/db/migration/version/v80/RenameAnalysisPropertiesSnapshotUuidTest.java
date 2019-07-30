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
package org.sonar.server.platform.db.migration.version.v80;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

public class RenameAnalysisPropertiesSnapshotUuidTest {

  private static final String TABLE_NAME = "analysis_properties";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(RenameAnalysisPropertiesSnapshotUuidTest.class, "analysis_properties.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private RenameAnalysisPropertiesSnapshotUuid underTest = new RenameAnalysisPropertiesSnapshotUuid(dbTester.database());

  @Test
  public void execute_renames_column_snapshot_uuid_and_recreate_index_snapshot_uuid_with_a_new_name() throws SQLException {
    underTest.execute();

    dbTester.assertColumnDefinition(TABLE_NAME, "analysis_uuid", Types.VARCHAR, 40, false);
    dbTester.assertIndex(TABLE_NAME, "analysis_properties_analysis", "analysis_uuid");
  }

  @Test
  public void execute_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);
    
    underTest.execute();
  }
}
