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
package org.sonar.server.platform.db.migration.version.v86;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

public class DropOrganizationInGroupRolesTest {

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(DropOrganizationInGroupRolesTest.class, "schema.sql");

  private MigrationStep underTest = new DropOrganizationInGroupRoles(dbTester.database());

  @Test
  public void column_has_been_dropped() throws SQLException {
    underTest.execute();
    dbTester.assertColumnDoesNotExist("group_roles", "organization_uuid");
    dbTester.assertUniqueIndex("group_roles", "uniq_group_roles", "group_uuid", "component_uuid", "role");
  }

}
