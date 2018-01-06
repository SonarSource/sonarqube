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
package org.sonar.server.platform.db.migration.version.v62;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;

public class MakeRootColumnNotNullOnTableUsersTest {
  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(MakeRootColumnNotNullOnTableUsersTest.class, "users_with_nullable_root.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeRootColumnNotNullOnTableUsers underTest = new MakeRootColumnNotNullOnTableUsers(dbTester.database());

  @Test
  public void execute_make_column_is_root_not_nullable() throws SQLException {
    underTest.execute();

    dbTester.assertColumnDefinition("users", "is_root", Types.BOOLEAN, null, false);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    underTest.execute();

    underTest.execute();
  }
}
