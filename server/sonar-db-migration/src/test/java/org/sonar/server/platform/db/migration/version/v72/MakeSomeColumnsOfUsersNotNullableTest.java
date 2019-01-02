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
package org.sonar.server.platform.db.migration.version.v72;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.sql.Types.VARCHAR;

public class MakeSomeColumnsOfUsersNotNullableTest {

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(MakeSomeColumnsOfUsersNotNullableTest.class, "users.sql");

  private MakeSomeColumnsOfUsersNotNullable underTest = new MakeSomeColumnsOfUsersNotNullable(db.database());

  @Test
  public void columns_are_set_as_not_nullable() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("users", "uuid", VARCHAR, 255, false);
    db.assertColumnDefinition("users", "login", VARCHAR, 255, false);
    db.assertColumnDefinition("users", "external_id", VARCHAR, 255, false);
    db.assertColumnDefinition("users", "external_login", VARCHAR, 255, false);
    db.assertColumnDefinition("users", "external_identity_provider", VARCHAR, 100, false);
    db.assertUniqueIndex("users", "users_login", "login");
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition("users", "uuid", VARCHAR, 255, false);
    db.assertColumnDefinition("users", "login", VARCHAR, 255, false);
    db.assertColumnDefinition("users", "external_id", VARCHAR, 255, false);
    db.assertColumnDefinition("users", "external_login", VARCHAR, 255, false);
    db.assertColumnDefinition("users", "external_identity_provider", VARCHAR, 100, false);
    db.assertUniqueIndex("users", "users_login", "login");
  }

}
