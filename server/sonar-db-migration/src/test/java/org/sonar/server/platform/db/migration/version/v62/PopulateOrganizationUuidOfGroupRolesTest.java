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

public class PopulateOrganizationUuidOfGroupRolesTest {

  private static final String TABLE_GROUP_ROLES = "group_roles";
  private static final String AN_ORG_UUID = "org1";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(PopulateOrganizationUuidOfGroupRolesTest.class, "group_roles.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PopulateOrganizationUuidOfGroupRoles underTest = new PopulateOrganizationUuidOfGroupRoles(dbTester.database());

  @Test
  public void execute_fails_with_ISE_if_default_organization_internal_property_is_not_set() throws SQLException {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default organization uuid is missing");

    underTest.execute();
  }
  @Test
  public void migration_populates_missing_organization_uuids() throws SQLException {
    dbTester.executeInsert("group_roles", "group_id", "1", "role", "admin", "organization_uuid", null);
    dbTester.executeInsert("group_roles", "group_id", "2", "role", "viewever", "organization_uuid", null);
    dbTester.executeInsert("group_roles", "group_id", "3", "role", "viewever", "organization_uuid", AN_ORG_UUID);
    insertDefaultOrganizationInternalProperty(AN_ORG_UUID);

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_GROUP_ROLES)).isEqualTo(3);
    assertThat(dbTester.countSql("select count(1) from group_roles where organization_uuid='" + AN_ORG_UUID + "'")).isEqualTo(3);
  }

  @Test
  public void migration_has_no_effect_on_empty_table() throws SQLException {
    insertDefaultOrganizationInternalProperty(AN_ORG_UUID);

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_GROUP_ROLES)).isEqualTo(0);
  }

  private void insertDefaultOrganizationInternalProperty(String defaultOrganizationUuid) {
    dbTester.executeInsert("internal_properties", "kee", "organization.default", "is_empty", false, "text_value", defaultOrganizationUuid);
  }
}
