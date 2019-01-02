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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import java.sql.Types;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

public class MakeProjectUuidNotNullableTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeProjectUuidNotNullableTest.class, "projects_with_nullable_project_uuid.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeProjectUuidNotNullable underTest = new MakeProjectUuidNotNullable(db.database());

  @Test
  public void execute_makes_column_project_uuid_not_nullable_on_empty_table() throws SQLException {
    db.assertColumnDefinition("projects", "project_uuid", Types.VARCHAR, 50, true);

    underTest.execute();

    db.assertColumnDefinition("projects", "project_uuid", Types.VARCHAR, 50, false);
  }

  @Test
  public void execute_makes_column_project_uuid_not_nullable_on_table_without_null_project_uuid() throws SQLException {
    db.assertColumnDefinition("projects", "project_uuid", Types.VARCHAR, 50, true);
    insertComponent("u1", "u1");
    insertComponent("u2", "u1");

    underTest.execute();

    db.assertColumnDefinition("projects", "project_uuid", Types.VARCHAR, 50, false);
  }

  @Test
  public void execute_fails_on_table_with_null_project_uuid() throws SQLException {
    db.assertColumnDefinition("projects", "project_uuid", Types.VARCHAR, 50, true);
    insertComponent("u1", "u1");
    insertComponent("u2", "u1");
    insertComponent("u3", null);

    expectedException.expect(IllegalStateException.class);

    underTest.execute();
  }

  private void insertComponent(String uuid, @Nullable String projectUuid) {
    db.executeInsert(
      "PROJECTS",
      "ORGANIZATION_UUID", "org_" + uuid,
      "UUID", uuid,
      "UUID_PATH", "path_" + uuid,
      "ROOT_UUID", "root_" + uuid,
      "PROJECT_UUID", projectUuid);
  }
}
