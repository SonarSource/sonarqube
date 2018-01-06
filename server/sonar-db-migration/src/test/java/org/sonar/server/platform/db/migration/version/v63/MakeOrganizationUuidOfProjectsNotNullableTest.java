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
package org.sonar.server.platform.db.migration.version.v63;

import java.sql.SQLException;
import java.sql.Types;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;

public class MakeOrganizationUuidOfProjectsNotNullableTest {
  private static final String TABLE_PROJECTS = "projects";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(PopulateOrganizationUuidToProjectsTest.class, "projects_with_nullable_organization.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeOrganizationUuidOfProjectsNotNullable underTest = new MakeOrganizationUuidOfProjectsNotNullable(dbTester.database());

  @Test
  public void migration_sets_uuid_column_not_nullable_on_empty_table() throws SQLException {
    underTest.execute();

    verifyColumnDefinition();
  }

  @Test
  public void migration_sets_uuid_column_not_nullable_on_populated_table() throws SQLException {
    insertComponent("org_A", 1);
    insertComponent("org_B", 2);

    underTest.execute();

    verifyColumnDefinition();
  }

  @Test
  public void migration_fails_if_some_row_has_a_null_uuid() throws SQLException {
    insertComponent(null, 1);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute");

    underTest.execute();
  }

  private void verifyColumnDefinition() {
    dbTester.assertColumnDefinition(TABLE_PROJECTS, "organization_uuid", Types.VARCHAR, 40, false);
  }

  private String insertComponent(@Nullable String organizationUuid, int id) {
    String uuid = "uuid_" + id;
    dbTester.executeInsert(
      "projects",
      "ID", valueOf(id),
      "ORGANIZATION_UUID", organizationUuid,
      "UUID", uuid,
      "ROOT_UUID", uuid,
      "PROJECT_UUID", uuid,
      "UUID_PATH", uuid + ".");
    return uuid;
  }
}
