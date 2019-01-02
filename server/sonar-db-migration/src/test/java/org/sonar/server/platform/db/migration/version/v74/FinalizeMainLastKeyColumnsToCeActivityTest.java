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
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.sql.Types.BOOLEAN;
import static java.sql.Types.VARCHAR;

public class FinalizeMainLastKeyColumnsToCeActivityTest {
  private static final String TABLE_NAME = "ce_activity";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(FinalizeMainLastKeyColumnsToCeActivityTest.class, "ce_activity.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private FinalizeMainLastKeyColumnsToCeActivity underTest = new FinalizeMainLastKeyColumnsToCeActivity(db.database());

  @Test
  public void columns_and_indexes_are_added_to_table() throws SQLException {
    underTest.execute();

    db.assertColumnDoesNotExist(TABLE_NAME, "tmp_is_last");
    db.assertColumnDoesNotExist(TABLE_NAME, "tmp_is_last_key");
    db.assertColumnDoesNotExist(TABLE_NAME, "tmp_main_is_last");
    db.assertColumnDoesNotExist(TABLE_NAME, "tmp_main_is_last_key");
    db.assertIndexDoesNotExist(TABLE_NAME, "ce_activity_t_islast_key");
    db.assertIndexDoesNotExist(TABLE_NAME, "ce_activity_t_islast");
    db.assertIndexDoesNotExist(TABLE_NAME, "ce_activity_t_main_islast_key");
    db.assertIndexDoesNotExist(TABLE_NAME, "ce_activity_t_main_islast");
    db.assertColumnDefinition(TABLE_NAME, "is_last", BOOLEAN, null, false);
    db.assertColumnDefinition(TABLE_NAME, "is_last_key", VARCHAR, 55, false);
    db.assertColumnDefinition(TABLE_NAME, "main_is_last", BOOLEAN, null, false);
    db.assertColumnDefinition(TABLE_NAME, "main_is_last_key", VARCHAR, 55, false);
    db.assertIndex(TABLE_NAME, "ce_activity_islast_key", "is_last_key");
    db.assertIndex(TABLE_NAME, "ce_activity_islast", "is_last", "status");
    db.assertIndex(TABLE_NAME, "ce_activity_main_islast_key", "main_is_last_key");
    db.assertIndex(TABLE_NAME, "ce_activity_main_islast", "main_is_last", "status");
  }

  @Test
  public void migration_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);

    underTest.execute();
  }

}
