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
package org.sonar.server.platform.telemetry;

import java.util.Map;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;
import org.sonar.db.project.CreationMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

class TelemetryOnboardingCountsProviderIT {

  @Rule
  public final DbTester db = DbTester.create();

  private final TelemetryOnboardingCountsProvider underTest = new TelemetryOnboardingCountsProvider(db.getDbClient());

  @BeforeEach
  void setUp() {
    // DbTester's @Rule truncation only fires under the JUnit4 runner; this class runs on Jupiter,
    // so state from other tests in the same DB otherwise leaks in (see TelemetryUserEnabledProviderIT).
    db.truncateTables();
  }

  @Test
  void getValues_whenNoProjectsOrAlmSettings_shouldReturnZeroCounts() {
    Map<String, Integer> values = underTest.getValues();

    assertThat(values).containsExactlyInAnyOrderEntriesOf(Map.of(
      "total_projects", 0,
      "analysed_projects", 0,
      "alm_imported_projects", 0,
      "configured_alm", 0));
  }

  @Test
  void getValues_whenSomeProjectsAnalysedAndAlmSettingsConfigured_shouldCountThemAll() {
    ProjectData analysedProject = db.components().insertPrivateProject();
    db.components().insertPrivateProject();
    db.components().insertPrivateProjectWithCreationMethod(CreationMethod.ALM_IMPORT_API);
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(analysedProject.getMainBranchComponent()).setLast(true));
    db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitlabAlmSetting();
    db.getSession().commit();

    Map<String, Integer> values = underTest.getValues();

    assertThat(values).containsExactlyInAnyOrderEntriesOf(Map.of(
      "total_projects", 3,
      "analysed_projects", 1,
      "alm_imported_projects", 1,
      "configured_alm", 2));
  }
}
