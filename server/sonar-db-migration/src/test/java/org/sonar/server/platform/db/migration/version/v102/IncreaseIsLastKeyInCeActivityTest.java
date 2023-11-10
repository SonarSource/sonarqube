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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.platform.db.migration.MigrationDbTester;

import static java.sql.Types.VARCHAR;
import static org.sonar.server.platform.db.migration.version.v102.IncreaseIsLastKeyInCeActivity.COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v102.IncreaseIsLastKeyInCeActivity.NEW_COLUMN_SIZE;
import static org.sonar.server.platform.db.migration.version.v102.IncreaseIsLastKeyInCeActivity.TABLE_NAME;

public class IncreaseIsLastKeyInCeActivityTest {

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(IncreaseIsLastKeyInCeActivity.class);
  private final IncreaseIsLastKeyInCeActivity underTest = new IncreaseIsLastKeyInCeActivity(db.database());

  @Test
  public void execute_increaseColumnSize() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, VARCHAR, 55, false);
    underTest.execute();
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, VARCHAR, NEW_COLUMN_SIZE, false);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, VARCHAR, 55, false);
    underTest.execute();
    underTest.execute();
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, VARCHAR, NEW_COLUMN_SIZE, false);
  }

}
