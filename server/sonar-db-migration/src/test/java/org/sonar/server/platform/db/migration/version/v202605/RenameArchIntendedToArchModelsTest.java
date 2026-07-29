/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v202605.RenameArchIntendedToArchModels.NEW_TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.RenameArchIntendedToArchModels.OLD_TABLE_NAME;

class RenameArchIntendedToArchModelsTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RenameArchIntendedToArchModels.class);

  private final RenameArchIntendedToArchModels underTest = new RenameArchIntendedToArchModels(db.database());

  @Test
  void execute_shouldRenameTableAndKeepExistingData() throws SQLException {
    db.assertTableExists(OLD_TABLE_NAME);
    db.assertTableDoesNotExist(NEW_TABLE_NAME);

    db.executeInsert(OLD_TABLE_NAME,
      "project_id", "project-uuid-1",
      "uuid", "model-uuid-1",
      "organization_id", "org-uuid-1",
      "data", "{\"groups\":[]}");

    underTest.execute();

    db.assertTableDoesNotExist(OLD_TABLE_NAME);
    db.assertTableExists(NEW_TABLE_NAME);

    List<Map<String, Object>> rows = db.select(
      "SELECT project_id, organization_id, data FROM arch_models WHERE uuid = 'model-uuid-1'");
    assertThat(rows).hasSize(1);
    assertThat(rows.getFirst())
      .containsEntry("PROJECT_ID", "project-uuid-1")
      .containsEntry("ORGANIZATION_ID", "org-uuid-1")
      .containsEntry("DATA", "{\"groups\":[]}");
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertTableDoesNotExist(OLD_TABLE_NAME);
    db.assertTableExists(NEW_TABLE_NAME);
  }
}
