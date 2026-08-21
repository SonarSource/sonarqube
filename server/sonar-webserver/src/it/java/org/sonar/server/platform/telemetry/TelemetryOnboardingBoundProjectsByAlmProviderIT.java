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
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.ProjectData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

class TelemetryOnboardingBoundProjectsByAlmProviderIT {

  @Rule
  public final DbTester db = DbTester.create();

  private final TelemetryOnboardingBoundProjectsByAlmProvider underTest = new TelemetryOnboardingBoundProjectsByAlmProvider(db.getDbClient());

  @BeforeEach
  void setUp() {
    // DbTester's @Rule truncation only fires under the JUnit4 runner; this class runs on Jupiter,
    // so state from other tests in the same DB otherwise leaks in (see TelemetryUserEnabledProviderIT).
    db.truncateTables();
  }

  @Test
  void getValues_whenNoProjects_shouldReturnEveryAlmAtZero() {
    Map<String, Integer> values = underTest.getValues();

    assertThat(values).containsExactlyInAnyOrderEntriesOf(Map.of(
      "github", 0,
      "gitlab", 0,
      "azure_devops", 0,
      "bitbucket", 0,
      "bitbucket_cloud", 0,
      "not_bound", 0,
      "not_bound_scanned", 0));
  }

  @Test
  void getValues_whenProjectsBoundAndUnbound_shouldCountPerAlmAndNotBound() {
    AlmSettingDto githubSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectData boundProject1 = db.components().insertPrivateProject();
    ProjectData boundProject2 = db.components().insertPrivateProject();
    db.almSettings().insertGitHubProjectAlmSetting(githubSetting, boundProject1.getProjectDto());
    db.almSettings().insertGitHubProjectAlmSetting(githubSetting, boundProject2.getProjectDto());
    db.components().insertPrivateProject();
    db.getSession().commit();

    Map<String, Integer> values = underTest.getValues();

    assertThat(values).containsEntry("github", 2)
      .containsEntry("not_bound", 1)
      .containsEntry("gitlab", 0)
      .containsEntry("not_bound_scanned", 0);
  }

  @Test
  void getValues_whenUnboundProjectWasScanned_shouldCountAsNotBoundScanned() {
    ProjectData scannedUnbound = db.components().insertPrivateProject();
    db.components().insertPrivateProject(); // unbound, never scanned
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(scannedUnbound.getMainBranchComponent()).setLast(true));
    db.getSession().commit();

    Map<String, Integer> values = underTest.getValues();

    assertThat(values).containsEntry("not_bound", 2)
      .containsEntry("not_bound_scanned", 1);
  }
}
