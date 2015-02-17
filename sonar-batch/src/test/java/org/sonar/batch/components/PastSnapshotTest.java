/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.components;

import org.sonar.batch.components.PastSnapshot;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Qualifiers;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class PastSnapshotTest {

  @Test
  public void test_some_setters_and_getters() {
    Snapshot snapshot = new Snapshot().setQualifier(Qualifiers.FILE).setCreatedAtMs(System.currentTimeMillis());
    snapshot.setId(10);
    PastSnapshot pastSnapshot = new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_VERSION, new Date(),
      snapshot)
      .setModeParameter("2.3")
      .setIndex(1);

    assertThat(pastSnapshot.getModeParameter()).isEqualTo("2.3");
    assertThat(pastSnapshot.getIndex()).isEqualTo(1);
    assertThat(pastSnapshot.getQualifier()).isEqualTo(Qualifiers.FILE);
    assertThat(pastSnapshot.getDate()).isNotNull();
    assertThat(pastSnapshot.getProjectSnapshotId()).isEqualTo(10);
  }

  @Test
  public void test_some_setters_and_getters_with_empty_snapshot() {
    PastSnapshot pastSnapshot = new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_VERSION);

    assertThat(pastSnapshot.getQualifier()).isNull();
    assertThat(pastSnapshot.getDate()).isNull();
    assertThat(pastSnapshot.getProjectSnapshotId()).isNull();
  }

  @Test
  public void testToStringForVersion() {
    PastSnapshot pastSnapshot = new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_VERSION, new Date(), new Snapshot().setCreatedAtMs(System.currentTimeMillis())).setModeParameter("2.3");
    assertThat(pastSnapshot.toString()).startsWith("Compare to version 2.3");
  }

  @Test
  public void testToStringForVersionWithoutDate() {
    PastSnapshot pastSnapshot = new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_VERSION).setModeParameter("2.3");
    assertThat(pastSnapshot.toString()).isEqualTo("Compare to version 2.3");
  }

  @Test
  public void testToStringForNumberOfDays() {
    PastSnapshot pastSnapshot = new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_DAYS, new Date()).setModeParameter("30");
    assertThat(pastSnapshot.toString()).startsWith("Compare over 30 days (");
  }

  @Test
  public void testToStringForNumberOfDaysWithSnapshot() {
    PastSnapshot pastSnapshot = new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_DAYS, new Date(), new Snapshot().setCreatedAtMs(System.currentTimeMillis())).setModeParameter("30");
    assertThat(pastSnapshot.toString()).startsWith("Compare over 30 days (");
  }

  @Test
  public void testToStringForDate() {
    PastSnapshot pastSnapshot = new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_DATE, new Date());
    assertThat(pastSnapshot.toString()).startsWith("Compare to date ");
  }

  @Test
  public void testToStringForDateWithSnapshot() {
    PastSnapshot pastSnapshot = new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_DATE, new Date(), new Snapshot().setCreatedAtMs(System.currentTimeMillis()));
    assertThat(pastSnapshot.toString()).startsWith("Compare to date ");
  }

  @Test
  public void testToStringForPreviousAnalysis() {
    PastSnapshot pastSnapshot = new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, new Date(), new Snapshot().setCreatedAtMs(System.currentTimeMillis()));
    assertThat(pastSnapshot.toString()).startsWith("Compare to previous analysis ");
  }

  @Test
  public void testToStringForPreviousAnalysisWithoutDate() {
    PastSnapshot pastSnapshot = new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    assertThat(pastSnapshot.toString()).isEqualTo("Compare to previous analysis");
  }
}
