/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

public class DropProjectAlmBindingsTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DropProjectAlmBindingsTest.class, "schema.sql");

  private MigrationStep underTest = new DropProjectAlmBindings(db.database());

  @Test
  public void drops_table() throws SQLException {
    insertData();
    db.assertTableExists("project_alm_bindings");
    underTest.execute();
    db.assertTableDoesNotExist("project_alm_bindings");
  }

  private void insertData() {
    db.executeInsert("project_alm_bindings",
      "uuid", "uuid1",
      "alm_id", "alm1",
      "repo_id", "repo1",
      "project_uuid", "project1",
      "url", "url1",
      "created_at", 123L,
      "updated_at", 456L
    );
  }
}
