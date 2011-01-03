/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.components;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.model.Snapshot;

import java.util.Date;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class PastSnapshotTest {

  @Test
  public void testToStringForVersion() {
    PastSnapshot pastSnapshot = new PastSnapshot(PastSnapshotFinderByVersion.MODE, new Snapshot()).setModeParameter("2.3");
    assertThat(pastSnapshot.toString(), startsWith("Compare to version 2.3"));
  }

  @Test
  public void testToStringForNumberOfDays() {
    PastSnapshot pastSnapshot = new PastSnapshot(PastSnapshotFinderByDays.MODE, new Snapshot()).setModeParameter("30");
    assertThat(pastSnapshot.toString(), startsWith("Compare over 30 days"));
  }

  @Test
  public void testToStringForDate() {
    PastSnapshot pastSnapshot = new PastSnapshot(PastSnapshotFinderByDate.MODE, new Snapshot()).setTargetDate(new Date());
    assertThat(pastSnapshot.toString(), startsWith("Compare to date "));
  }

  @Test
  public void testToStringForPreviousAnalysis() {
    PastSnapshot pastSnapshot = new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, new Snapshot()).setTargetDate(new Date());
    assertThat(pastSnapshot.toString(), startsWith("Compare to previous analysis"));
  }
}
