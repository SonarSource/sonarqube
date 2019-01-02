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
package org.sonar.server.platform.db.migration.version.v62;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class DeletePermissionShareDashboardTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeletePermissionShareDashboardTest.class, "roles.sql");

  private DeletePermissionShareDashboard underTest = new DeletePermissionShareDashboard(db.database());

  @Test
  public void delete_permissions() throws SQLException {
    // combinations of group permissions to be kept
    db.executeInsert("group_roles", "id", "1", "group_id", null, "resource_id", null, "role", "admin");
    db.executeInsert("group_roles", "id", "2", "group_id", null, "resource_id", "1", "role", "issueadmin");
    db.executeInsert("group_roles", "id", "3", "group_id", "1", "resource_id", null, "role", "admin");
    db.executeInsert("group_roles", "id", "4", "group_id", "1", "resource_id", "1", "role", "issueadmin");

    // combinations of group permissions to be removed (even if it does make sense to have "shareDashboard"
    // on projects)
    db.executeInsert("group_roles", "id", "5", "group_id", null, "resource_id", null, "role", "shareDashboard");
    db.executeInsert("group_roles", "id", "6", "group_id", null, "resource_id", "1", "role", "shareDashboard");
    db.executeInsert("group_roles", "id", "7", "group_id", "1", "resource_id", null, "role", "shareDashboard");
    db.executeInsert("group_roles", "id", "8", "group_id", "1", "resource_id", "1", "role", "shareDashboard");

    // combinations of user permissions to be kept
    db.executeInsert("user_roles", "id", "1", "user_id", "100", "resource_id", null, "role", "admin");
    db.executeInsert("user_roles", "id", "2", "user_id", "100", "resource_id", "1", "role", "issueadmin");

    // combinations of user permissions to be removed (even if it does make sense to have "shareDashboard"
    // on projects)
    db.executeInsert("user_roles", "id", "3", "user_id", "100", "resource_id", null, "role", "shareDashboard");
    db.executeInsert("user_roles", "id", "4", "user_id", "100", "resource_id", "1", "role", "shareDashboard");

    underTest.execute();

    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(4);
    assertThat(db.countSql("select count(id) from group_roles where role='shareDashboard'")).isEqualTo(0);

    assertThat(db.countRowsOfTable("user_roles")).isEqualTo(2);
    assertThat(db.countSql("select count(id) from user_roles where role='shareDashboard'")).isEqualTo(0);
  }
}
