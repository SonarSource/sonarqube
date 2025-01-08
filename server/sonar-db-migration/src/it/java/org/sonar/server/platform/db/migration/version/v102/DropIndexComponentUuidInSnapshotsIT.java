/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;



class DropIndexComponentUuidInSnapshotsIT {

  private static final String TABLE_NAME = "snapshots";
  private static final String COLUMN_NAME = "component_uuid";
  private static final String INDEX_NAME = "snapshot_component";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(DropIndexComponentUuidInSnapshots.class);
  private final DropIndexComponentUuidInSnapshots underTest = new DropIndexComponentUuidInSnapshots(db.database());

  @Test
  void index_is_dropped() throws SQLException {
    db.assertIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME);

    underTest.execute();

    db.assertIndexDoesNotExist(TABLE_NAME, COLUMN_NAME);
  }

  @Test
  void migration_is_reentrant() throws SQLException {
    db.assertIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME);

    underTest.execute();
    underTest.execute();

    db.assertIndexDoesNotExist(TABLE_NAME, COLUMN_NAME);
  }
}
