/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v107;

import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static java.sql.Types.VARCHAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v107.AddDevopsPlatformColumnInDevopsPermsMapping.DEVOPS_PLATFORM_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v107.AddDevopsPlatformColumnInDevopsPermsMapping.DEFAULT_COLUMN_VALUE;
import static org.sonar.server.platform.db.migration.version.v107.RenameGithubPermsMappingTable.DEVOPS_PERMS_MAPPING_TABLE_NAME;

class AddDevopsPlatformColumnInDevopsPermsMappingIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddDevopsPlatformColumnInDevopsPermsMapping.class);

  private final AddDevopsPlatformColumnInDevopsPermsMapping underTest = new AddDevopsPlatformColumnInDevopsPermsMapping(db.database());

  @Test
  void execute_whenColumnDoesNotExist_shouldCreateColumn() throws SQLException {
    db.assertColumnDoesNotExist(DEVOPS_PERMS_MAPPING_TABLE_NAME, DEVOPS_PLATFORM_COLUMN_NAME);
    underTest.execute();
    assertColumnExists();
  }

  @Test
  void execute_whenColumnsAlreadyExists_shouldNotFail() throws SQLException {
    underTest.execute();
    assertColumnExists();
    underTest.execute();
  }

  @Test
  void execute_whenDataAlreadyExists_shouldCreateColumnWithDefaultValue() throws SQLException {
    db.executeInsert(DEVOPS_PERMS_MAPPING_TABLE_NAME, "uuid", "UUID", "devops_platform_role", "uniqAdmin", "sonarqube_permission", "uniqPermission");
    underTest.execute();
    assertDevopsPlatformColumnSetToDefault();
    assertColumnExists();
  }

  private void assertDevopsPlatformColumnSetToDefault() {
    Map<String, Object> selectResult = db.selectFirst("select devops_platform from devops_perms_mapping where uuid = 'UUID'");
    assertThat(selectResult).containsEntry("devops_platform", DEFAULT_COLUMN_VALUE);
  }

  private void assertColumnExists() {
    db.assertColumnDefinition(DEVOPS_PERMS_MAPPING_TABLE_NAME, DEVOPS_PLATFORM_COLUMN_NAME, VARCHAR, 40, false);
  }

}
