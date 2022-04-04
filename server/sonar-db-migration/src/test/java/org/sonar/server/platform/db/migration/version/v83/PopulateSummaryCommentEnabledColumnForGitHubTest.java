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
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateSummaryCommentEnabledColumnForGitHubTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateSummaryCommentEnabledColumnForGitHubTest.class, "schema.sql");

  private final System2 system = System2.INSTANCE;

  private final DataChange underTest = new PopulateSummaryCommentEnabledColumnForGitHub(db.database(), system);

  @Test
  public void does_not_fail_if_alm_settings_are_empty() throws SQLException {
    underTest.execute();

    assertThat(db.countSql("select count(uuid) from project_alm_settings where summary_comment_enabled is null"))
      .isZero();

    // re-entrant migration
    underTest.execute();
  }

  @Test
  public void does_not_set_comment_summary_enabled_flag_for_alms_other_than_github() throws SQLException {
    insertAlmSetting("alm-bitbucket", "bitbucket");
    insertAlmSetting("alm-azure", "azure");
    insertAlmSetting("alm-gitlab", "gitlab");

    insertProjectAlmSetting("project-alm-1", "alm-bitbucket");
    insertProjectAlmSetting("project-alm-2", "alm-bitbucket");
    insertProjectAlmSetting("project-alm-3", "alm-azure");
    insertProjectAlmSetting("project-alm-4", "alm-azure");
    insertProjectAlmSetting("project-alm-5", "alm-azure");
    insertProjectAlmSetting("project-alm-6", "alm-gitlab");

    underTest.execute();

    verifySummaryColumnForProjectAlmSettings(null, "project-alm-1", "project-alm-2", "project-alm-3",
      "project-alm-4", "project-alm-5", "project-alm-6");
  }

  @Test
  public void set_comment_summary_enabled_flag_to_true_for_github_alm_only() throws SQLException {
    insertAlmSetting("alm-github", "github");
    insertAlmSetting("alm-azure", "azure");
    insertAlmSetting("alm-gitlab", "gitlab");

    insertProjectAlmSetting("project-alm-1", "alm-bitbucket");
    insertProjectAlmSetting("project-alm-2", "alm-bitbucket");
    insertProjectAlmSetting("project-alm-3", "alm-github");
    insertProjectAlmSetting("project-alm-4", "alm-github");
    insertProjectAlmSetting("project-alm-5", "alm-github");
    insertProjectAlmSetting("project-alm-6", "alm-github");
    insertProjectAlmSetting("project-alm-7", "alm-gitlab");

    underTest.execute();

    verifySummaryColumnForProjectAlmSettings(null, "project-alm-1", "project-alm-2", "project-alm-7");
    verifySummaryColumnForProjectAlmSettings(true, "project-alm-3", "project-alm-4", "project-alm-5", "project-alm-6");
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertAlmSetting("alm-github", "github");
    insertAlmSetting("alm-azure", "azure");
    insertAlmSetting("alm-gitlab", "gitlab");

    insertProjectAlmSetting("project-alm-1", "alm-bitbucket");
    insertProjectAlmSetting("project-alm-2", "alm-github");

    underTest.execute();
    // re-entrant
    underTest.execute();

    verifySummaryColumnForProjectAlmSettings(null, "project-alm-1");
    verifySummaryColumnForProjectAlmSettings(true, "project-alm-2");
  }

  private void verifySummaryColumnForProjectAlmSettings(@Nullable Boolean expectedSummarColumnValue, String... projectUuids) {
    assertThat(db.select("select uuid, summary_comment_enabled from project_alm_settings")
      .stream()
      .filter(rowColumns -> Objects.equals(expectedSummarColumnValue, getBooleanValue(rowColumns.get("SUMMARY_COMMENT_ENABLED"))))
      .map(row -> row.get("UUID"))
      .collect(Collectors.toList()))
        .containsExactly(projectUuids);
  }

  private Boolean getBooleanValue(@Nullable Object value) {
    return value == null ? null : Boolean.parseBoolean(value.toString());
  }

  private void insertProjectAlmSetting(String uuid, String almSettingsUuid) {
    db.executeInsert("PROJECT_ALM_SETTINGS",
      "UUID", uuid,
      "ALM_SETTING_UUID", almSettingsUuid,
      "PROJECT_UUID", uuid + "-description",
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
