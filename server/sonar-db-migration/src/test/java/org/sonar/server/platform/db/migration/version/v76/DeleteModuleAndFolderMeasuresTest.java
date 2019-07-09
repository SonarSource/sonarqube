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
package org.sonar.server.platform.db.migration.version.v76;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class DeleteModuleAndFolderMeasuresTest {

  private static final String TABLE_MEASURES = "project_measures";
  private static final int COMPONENT_ID_1 = 125;
  private static final int COMPONENT_ID_2 = 604;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteModuleAndFolderMeasuresTest.class, "project_measures.sql");

  private MapSettings settings = new MapSettings();
  private DeleteModuleAndFolderMeasures underTest = new DeleteModuleAndFolderMeasures(db.database(), settings.asConfig());

  @Test
  public void migration_has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isZero();
  }

  @Test
  public void execute_has_no_effect_on_SonarCloud() throws SQLException {
    String moduleUuid = insertComponent(1, "BRC");
    insertMeasure(1, moduleUuid);

    settings.setProperty("sonar.sonarcloud.enabled", true);

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isEqualTo(1);
  }

  @Test
  public void migration_removes_module_level_measures() throws SQLException {
    String moduleUuid = insertComponent(1, "BRC");
    insertMeasure(1, moduleUuid);

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isZero();
  }

  @Test
  public void migration_removes_folder_level_measures() throws SQLException {
    String dirUuid = insertComponent(1, "DIR");
    insertMeasure(1, dirUuid);

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isZero();
  }

  @Test
  public void migration_ignores_not_relevant_measures() throws SQLException {
    String projectUuid = insertComponent(1, "TRK");
    insertMeasure(1, projectUuid);
    String moduleUuid = insertComponent(2, "BRC");
    insertMeasure(2, moduleUuid);
    insertMeasure(3, moduleUuid);
    String dirUuid = insertComponent(3, "DIR");
    insertMeasure(4, dirUuid);
    insertMeasure(5, dirUuid);
    String fileUuid = insertComponent(4, "FIL");
    insertMeasure(6, fileUuid);

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isEqualTo(2);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    String dirUuid = insertComponent(3, "DIR");
    insertMeasure(1, dirUuid);
    String fileUuid = insertComponent(4, "FIL");
    insertMeasure(2, fileUuid);

    underTest.execute();
    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isEqualTo(1);

    underTest.execute();
    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isEqualTo(1);
  }

  private String insertComponent(long id, String qualifier) {
    String uuid = "uuid_" + id;
    db.executeInsert(
      "projects",
      "ID", valueOf(id),
      "QUALIFIER", qualifier,
      "ORGANIZATION_UUID", "org_" + id,
      "UUID_PATH", "path_" + id,
      "ROOT_UUID", "root_" + id,
      "PROJECT_UUID", "project_" + id,
      "PRIVATE", false,
      "UUID", uuid);
    return uuid;
  }

  private void insertMeasure(long id, String componentUuid) {
    db.executeInsert(
      "project_measures",
      "ID", valueOf(id),
      "COMPONENT_UUID", componentUuid,
      "METRIC_ID", valueOf(id + 10),
      "ANALYSIS_UUID", valueOf(id + 100),
      "VALUE", valueOf(id + 1000));
  }

}
