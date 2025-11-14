/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202501;

import java.sql.SQLException;
import java.util.Objects;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Oracle;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

class PopulateInlineAnnotationsEnabledColumnForAzureTest {
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateInlineAnnotationsEnabledColumnForAzure.class);

  private final System2 system = System2.INSTANCE;

  private final DataChange underTest = new PopulateInlineAnnotationsEnabledColumnForAzure(db.database(), system);

  @Test
  void does_not_fail_if_alm_settings_are_empty() throws SQLException {
    underTest.execute();

    assertThat(db.countSql("select count(uuid) from project_alm_settings where inline_annotations_enabled is null"))
      .isZero();

    // re-entrant migration
    underTest.execute();
  }

  @Test
  void does_not_set_issue_annotations_enabled_flag_for_alms_other_than_azure() throws SQLException {
    insertAlmSetting("alm-bitbucket", "bitbucket");
    insertAlmSetting("alm-azure", "azure_devops");
    insertAlmSetting("alm-gitlab", "gitlab");

    insertProjectAlmSetting("project-alm-1", "alm-bitbucket");
    insertProjectAlmSetting("project-alm-2", "alm-bitbucket");
    insertProjectAlmSetting("project-alm-3", "alm-azure");
    insertProjectAlmSetting("project-alm-4", "alm-azure");
    insertProjectAlmSetting("project-alm-5", "alm-azure");
    insertProjectAlmSetting("project-alm-6", "alm-gitlab");

    underTest.execute();

    verifyInlineAnnotationsEnabledColumnForProjectAlmSettings(null,
      "project-alm-1", "project-alm-2", "project-alm-6");
  }

  @Test
  void set_inline_annotations_enabled_flag_to_true_for_azure_alm_only() throws SQLException {
    insertAlmSetting("alm-github", "github");
    insertAlmSetting("alm-azure1", "azure_devops");
    insertAlmSetting("alm-azure2", "azure_devops");
    insertAlmSetting("alm-gitlab", "gitlab");

    insertProjectAlmSetting("project-alm-1", "alm-bitbucket");
    insertProjectAlmSetting("project-alm-2", "alm-bitbucket");
    insertProjectAlmSetting("project-alm-3", "alm-github");
    insertProjectAlmSetting("project-alm-4", "alm-github");
    insertProjectAlmSetting("project-alm-5", "alm-github");
    insertProjectAlmSetting("project-alm-6", "alm-github");
    insertProjectAlmSetting("project-alm-7", "alm-gitlab");
    insertProjectAlmSetting("project-alm-8", "alm-azure1");
    insertProjectAlmSetting("project-alm-9", "alm-azure1");
    insertProjectAlmSetting("project-alm-10", "alm-azure2");

    underTest.execute();

    verifyInlineAnnotationsEnabledColumnForProjectAlmSettings(null,
      "project-alm-1", "project-alm-2", "project-alm-3", "project-alm-4", "project-alm-5", "project-alm-6", "project-alm-7");
    verifyInlineAnnotationsEnabledColumnForProjectAlmSettings(true, "project-alm-8", "project-alm-9", "project-alm-10");
  }

  @Test
  void migration_is_reentrant() throws SQLException {
    insertAlmSetting("alm-github", "github");
    insertAlmSetting("alm-azure", "azure_devops");
    insertAlmSetting("alm-gitlab", "gitlab");

    insertProjectAlmSetting("project-alm-1", "alm-bitbucket");
    insertProjectAlmSetting("project-alm-2", "alm-azure");

    underTest.execute();
    // re-entrant
    underTest.execute();

    verifyInlineAnnotationsEnabledColumnForProjectAlmSettings(null, "project-alm-1");
    verifyInlineAnnotationsEnabledColumnForProjectAlmSettings(true, "project-alm-2");
  }

  private void verifyInlineAnnotationsEnabledColumnForProjectAlmSettings(@Nullable Boolean expectedInlineAnnotationColumnValue, String... projectUuids) {
    assertThat(db.select("select uuid, inline_annotations_enabled from project_alm_settings")
      .stream()
      .filter(rowColumns -> Objects.equals(expectedInlineAnnotationColumnValue, getBooleanValue(rowColumns.get("INLINE_ANNOTATIONS_ENABLED"))))
      .map(row -> row.get("UUID"))
      .toList())
      .containsExactlyInAnyOrder(projectUuids);
  }

  private Boolean getBooleanValue(@Nullable Object value) {
    return value == null ? null : Boolean.parseBoolean(value.toString());
  }

  private void insertProjectAlmSetting(String uuid, String almSettingsUuid) {
    db.executeInsert("PROJECT_ALM_SETTINGS",
      "UUID", uuid,
      "ALM_SETTING_UUID", almSettingsUuid,
      "PROJECT_UUID", uuid + "-description",
      "MONOREPO", Oracle.ID.equals(db.database().getDialect().getId()) ? 0 : false,
      "UPDATED_AT", system.now(),
      "CREATED_AT", system.now());
  }

  private void insertAlmSetting(String uuid, String almId) {
    db.executeInsert("ALM_SETTINGS",
      "UUID", uuid,
      "ALM_ID", almId,
      "KEE", uuid + "-key",
      "UPDATED_AT", system.now(),
      "CREATED_AT", system.now());
  }
}
