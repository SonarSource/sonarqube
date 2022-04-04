/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.users.fk.qprofileeditusers;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class AddUniqueIndexOnUserUuidAndQProfileUuidOfQProfileEditUsersTableTest {

  private static final String TABLE_NAME = "qprofile_edit_users";
  private static final String INDEX_NAME = "qprofile_edit_users_unique";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(AddUniqueIndexOnUserUuidAndQProfileUuidOfQProfileEditUsersTableTest.class, "schema.sql");

  DdlChange underTest = new AddUniqueIndexOnUserUuidAndQProfileUuidOfQProfileEditUsersTable(dbTester.database());

  @Test
  public void add_index() throws SQLException {
    underTest.execute();
    dbTester.assertUniqueIndex(TABLE_NAME, INDEX_NAME, "user_uuid", "qprofile_uuid");
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    underTest.execute();
    dbTester.assertUniqueIndex(TABLE_NAME, INDEX_NAME, "user_uuid", "qprofile_uuid");
  }
}
