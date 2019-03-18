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

package org.sonar.server.platform.db.migration.version.v77;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

public class DropProjectVersionFromSnapshotsTest {

  private static final String TABLE = "snapshots";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(DropProjectVersionFromSnapshotsTest.class, "snapshots.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DropProjectVersionFromSnapshots underTest = new DropProjectVersionFromSnapshots(db.database());

  @Test
  public void drop_column_and_recreate_index() throws SQLException {
    underTest.execute();

    db.assertColumnDoesNotExist(TABLE, "project_version");
  }

  @Test
  public void migration_is_not_re_entrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);

    underTest.execute();
  }

}
