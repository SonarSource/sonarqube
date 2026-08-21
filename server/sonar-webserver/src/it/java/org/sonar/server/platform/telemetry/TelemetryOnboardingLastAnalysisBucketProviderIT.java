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
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

class TelemetryOnboardingLastAnalysisBucketProviderIT {

  private static final long DAY_MILLIS = TimeUnit.DAYS.toMillis(1);
  private static final long NOW = 10_000 * DAY_MILLIS;

  @Rule
  public final DbTester db = DbTester.create();

  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);
  private final TelemetryOnboardingLastAnalysisBucketProvider underTest =
    new TelemetryOnboardingLastAnalysisBucketProvider(db.getDbClient(), system2);

  @BeforeEach
  void setUp() {
    // DbTester's @Rule truncation only fires under the JUnit4 runner; this class runs on Jupiter,
    // so state from other tests in the same DB otherwise leaks in (see TelemetryUserEnabledProviderIT).
    db.truncateTables();
  }

  @Test
  void getValues_whenNoProjects_shouldReturnEveryBucketAtZero() {
    Map<String, Integer> values = underTest.getValues();

    assertThat(values).containsExactlyInAnyOrderEntriesOf(Map.of(
      "le_7d", 0,
      "le_30d", 0,
      "le_180d", 0,
      "gt_180d", 0,
      "never", 0));
  }

  @Test
  void getValues_shouldBucketProjectsByLastAnalysisRecency() {
    ProjectData recent = db.components().insertPrivateProject();
    ProjectData month = db.components().insertPrivateProject();
    ProjectData old = db.components().insertPrivateProject();
    ProjectData veryOld = db.components().insertPrivateProject();
    db.components().insertPrivateProject(); // never analysed

    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(recent.getMainBranchComponent()).setLast(true).setCreatedAt(NOW - 2 * DAY_MILLIS));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(month.getMainBranchComponent()).setLast(true).setCreatedAt(NOW - 20 * DAY_MILLIS));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(old.getMainBranchComponent()).setLast(true).setCreatedAt(NOW - 100 * DAY_MILLIS));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(veryOld.getMainBranchComponent()).setLast(true).setCreatedAt(NOW - 200 * DAY_MILLIS));
    db.getSession().commit();

    Map<String, Integer> values = underTest.getValues();

    assertThat(values).containsExactlyInAnyOrderEntriesOf(Map.of(
      "le_7d", 1,
      "le_30d", 1,
      "le_180d", 1,
      "gt_180d", 1,
      "never", 1));
  }
}
