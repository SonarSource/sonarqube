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
package org.sonar.server.platform.db.migration.version.v83;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.sql.Types.BOOLEAN;
import static org.assertj.core.api.Assertions.assertThat;

public class AddSummaryEnabledColumnToAlmSettingsTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddSummaryEnabledColumnToAlmSettingsTest.class, "schema.sql");

  private DdlChange underTest = new AddSummaryEnabledColumnToAlmSettings(db.database());

  @Before
  public void setup() {
    insertProjectAlmSetting("1");
    insertProjectAlmSetting("2");
    insertProjectAlmSetting("3");
    insertProjectAlmSetting("4");
    insertProjectAlmSetting("5");
  }

  @Test
  public void should_add_summary_comment_enabled_column() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("project_alm_settings", "summary_comment_enabled", BOOLEAN, null, true);

    assertThat(db.countSql("select count(uuid) from project_alm_settings where summary_comment_enabled is null"))
      .isEqualTo(5);
  }

  private void insertProjectAlmSetting(String uuid) {
    db.executeInsert("PROJECT_ALM_SETTINGS",
      "UUID", uuid,
      "ALM_SETTING_UUID", uuid + "-name",
      "PROJECT_UUID", uuid + "-description",
      "UPDATED_AT", System2.INSTANCE.now(),
      "CREATED_AT", System2.INSTANCE.now());
  }

}
