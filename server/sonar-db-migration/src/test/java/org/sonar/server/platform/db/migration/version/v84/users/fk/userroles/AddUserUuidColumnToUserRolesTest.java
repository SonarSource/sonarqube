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
package org.sonar.server.platform.db.migration.version.v84.users.fk.userroles;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;

public class AddUserUuidColumnToUserRolesTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddUserUuidColumnToUserRolesTest.class, "schema.sql");

  private DdlChange underTest = new AddUserUuidColumnToUserRoles(db.database());

  @Before
  public void setup() {
    insertUserRole(Uuids.createFast(), 1L);
    insertUserRole(Uuids.createFast(), 2L);
    insertUserRole(Uuids.createFast(), 3L);
  }

  @Test
  public void add_uuid_column() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("user_roles", "user_uuid", Types.VARCHAR, 255, true);

    assertThat(db.countSql("select count(*) from user_roles"))
      .isEqualTo(3);
  }

  private void insertUserRole(String uuid, Long userId) {
    db.executeInsert("user_roles",
      "uuid", uuid,
      "organization_uuid", Uuids.createFast(),
      "user_id", userId,
      "role", Uuids.createFast());
  }
}
