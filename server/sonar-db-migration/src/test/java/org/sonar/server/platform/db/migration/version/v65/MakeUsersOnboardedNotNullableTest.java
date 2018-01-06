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
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

public class MakeUsersOnboardedNotNullableTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeUsersOnboardedNotNullableTest.class, "users_with_nullable_onboarded_column.sql");

  private MakeUsersOnboardedNotNullable underTest = new MakeUsersOnboardedNotNullable(db.database());

  @Test
  public void execute_makes_column_component_uuid_not_nullable_on_empty_table() throws SQLException {
    underTest.execute();

    verifyColumn();
  }

  @Test
  public void execute_makes_column_component_uuid_not_nullable_on_populated_table() throws SQLException {
    insertUser();
    insertUser();
    insertUser();

    underTest.execute();

    verifyColumn();
  }

  private void verifyColumn() {
    db.assertColumnDefinition("users", "onboarded", Types.BOOLEAN, null, false);
  }

  private void insertUser() {
    db.executeInsert(
      "users",
      "ONBOARDED", true,
      "IS_ROOT", true);
  }
}
