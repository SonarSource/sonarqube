/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.platform.db.migration.version.v63;

import java.sql.Connection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultOrganizationUuidImplTest {
  private static final String AN_ORG_UUID = "org1";

  @Rule
  public DbTester dbTester = DbTester.createForSchema(System2.INSTANCE, DefaultOrganizationUuidImplTest.class, "internal_properties.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DefaultOrganizationUuid underTest = new DefaultOrganizationUuidImpl();

  @Test
  public void get_fails_with_ISE_if_default_organization_internal_property_is_not_set() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default organization uuid is missing");

    callGet(underTest);
  }

  @Test
  public void get_returns_uuid_from_table_INTERNAL_PROPERTIES() throws Exception {
    dbTester.executeInsert("internal_properties", "kee", "organization.default", "is_empty", false, "text_value", AN_ORG_UUID);

    assertThat(callGet(underTest)).isEqualTo(AN_ORG_UUID);
  }

  private String callGet(DefaultOrganizationUuid defaultOrganizationUuid) throws Exception {
    try (Connection connection = dbTester.openConnection()) {
      DataChange.Context context = new DataChange.Context(dbTester.database(), connection, connection);
      return defaultOrganizationUuid.get(context);
    }
  }

}
