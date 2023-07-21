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
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

public class RenameCreatedAtTempInComponentsTest {

  public static final String TABLE_NAME = "components";
  public static final String OLD_COLUMN_NAME = "created_at_temp";
  public static final String NEW_COLUMN_NAME = "created_at";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(RenameCreatedAtTempInComponentsTest.class, "schema.sql");

  private final RenameCreatedAtTempInComponents underTest = new RenameCreatedAtTempInComponents(db.database());

  @Test
  public void execute_shouldRenameColumn() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, OLD_COLUMN_NAME, Types.BIGINT, null, null);
    db.assertColumnDoesNotExist(TABLE_NAME, NEW_COLUMN_NAME);
    underTest.execute();
    db.assertColumnDefinition(TABLE_NAME, NEW_COLUMN_NAME, Types.BIGINT, null, null);
    db.assertColumnDoesNotExist(TABLE_NAME, OLD_COLUMN_NAME);
  }

  @Test
  public void execute_whenExecutedTwice_shouldNotFail() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, OLD_COLUMN_NAME, Types.BIGINT, null, null);
    db.assertColumnDoesNotExist(TABLE_NAME, NEW_COLUMN_NAME);
    underTest.execute();
    underTest.execute();
    db.assertColumnDefinition(TABLE_NAME, NEW_COLUMN_NAME, Types.BIGINT, null, null);
    db.assertColumnDoesNotExist(TABLE_NAME, OLD_COLUMN_NAME);
  }
}
