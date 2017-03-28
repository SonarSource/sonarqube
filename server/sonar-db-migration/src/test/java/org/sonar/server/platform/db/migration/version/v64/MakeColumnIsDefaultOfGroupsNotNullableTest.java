/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

public class MakeColumnIsDefaultOfGroupsNotNullableTest {

  private static final String TABLE = "groups";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(MakeColumnIsDefaultOfGroupsNotNullableTest.class, "previous_groups.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeColumnIsDefaultOfGroupsNotNullable underTest = new MakeColumnIsDefaultOfGroupsNotNullable(dbTester.database());

  @Test
  public void migration_sets_is_default_column_not_nullable_on_empty_table() throws SQLException {
    underTest.execute();

    verifyColumnDefinition();
  }

  @Test
  public void migration_sets_is_default_column_not_nullable_on_populated_table() throws SQLException {
    insertGroup("group1", true);
    insertGroup("group2", false);

    underTest.execute();

    verifyColumnDefinition();
  }

  @Test
  public void migration_fails_if_some_row_has_a_is_default() throws SQLException {
    insertGroup("group", null);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute");

    underTest.execute();
  }

  private void verifyColumnDefinition() {
    dbTester.assertColumnDefinition(TABLE, "is_default", Types.BOOLEAN, null, false);
  }

  private void insertGroup(String name, @Nullable Boolean isDefault) {
    dbTester.executeInsert(
      "GROUPS",
      "NAME", name,
      "IS_DEFAULT", isDefault == null ? null : String.valueOf(isDefault),
      "ORGANIZATION_UUID", "ORGANIZATION_UUID",
      "CREATED_AT", new Date(),
      "UPDATED_AT", new Date());
  }
}
