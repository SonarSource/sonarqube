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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteProjectAlmSettingsOrphansTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteProjectAlmSettingsOrphansTest.class, "schema.sql");

  private DeleteProjectAlmSettingsOrphans underTest = new DeleteProjectAlmSettingsOrphans(db.database());

  @Test
  public void execute_migration() throws SQLException {
    String projectUuid1 = insertProject();
    String projectAlmSetting1 = insertProjectAlmSetting(projectUuid1);
    String projectUuid2 = insertProject();
    String projectAlmSetting2 = insertProjectAlmSetting(projectUuid2);
    String projectUuid3 = insertProject();
    String projectAlmSetting3 = insertProjectAlmSetting(projectUuid3);

    // create orphans
    insertProjectAlmSetting();
    insertProjectAlmSetting();
    insertProjectAlmSetting();
    insertProjectAlmSetting();

    underTest.execute();

    assertProjectAlmSettingsRowsExist(projectAlmSetting1, projectAlmSetting2, projectAlmSetting3);
  }

  private void assertProjectAlmSettingsRowsExist(String... projectAlmSettings) {
    assertThat(db.select("select uuid from project_alm_settings")
      .stream()
      .map(rows -> rows.get("UUID"))).containsOnly(projectAlmSettings);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    String projectUuid = insertProject();
    String projectAlmSetting = insertProjectAlmSetting(projectUuid);
    // create orphans
    insertProjectAlmSetting();
    insertProjectAlmSetting();
    insertProjectAlmSetting();
    insertProjectAlmSetting();

    underTest.execute();
    // should be re-entrant
    underTest.execute();

    assertProjectAlmSettingsRowsExist(projectAlmSetting);
  }

  private String insertProjectAlmSetting(String projectUuid) {
    String uuid = Uuids.createFast();
    db.executeInsert("PROJECT_ALM_SETTINGS",
      "UUID", uuid,
      "ALM_SETTING_UUID", uuid + "-name",
      "PROJECT_UUID", projectUuid,
      "UPDATED_AT", System2.INSTANCE.now(),
      "CREATED_AT", System2.INSTANCE.now());
    return uuid;
  }

  private String insertProjectAlmSetting() {
    String notExistingProjectUuid = Uuids.createFast();
    return insertProjectAlmSetting(notExistingProjectUuid);
  }

  private String insertProject() {
    String uuid = Uuids.createFast();
    db.executeInsert("PROJECTS",
      "UUID", uuid,
      "KEE", uuid + "-key",
      "QUALIFIER", "TRK",
      "ORGANIZATION_UUID", uuid + "-key",
      "PRIVATE", Boolean.toString(false),
      "UPDATED_AT", System.currentTimeMillis());
    return uuid;
  }
}
