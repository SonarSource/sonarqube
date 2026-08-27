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
package org.sonar.telemetry.core.event.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.api.issue.Issue;

import static org.assertj.core.api.Assertions.assertThat;

class IssueTelemetryStatusTest {

  @ParameterizedTest
  @CsvSource({
    "OPEN,,OPEN",
    "CONFIRMED,,CONFIRMED",
    "REOPENED,,REOPENED",
    "RESOLVED,FALSE-POSITIVE,FALSE_POSITIVE",
    "RESOLVED,WONTFIX,ACCEPTED",
    "RESOLVED,FIXED,FIXED",
    "CLOSED,,FIXED",
    "CLOSED,REMOVED,REMOVED",
    "CLOSED,FIXED,FIXED",
    "IN_SANDBOX,,IN_SANDBOX",
    "TO_REVIEW,,",
    "REVIEWED,,",
  })
  void of_shouldMapEachReachableStatusResolutionCombination(String status, String resolution, String expected) {
    assertThat(IssueTelemetryStatus.of(status, resolution)).isEqualTo(expected);
  }

  @Test
  void of_whenReopened_isNotFoldedIntoOpen() {
    assertThat(IssueTelemetryStatus.of(Issue.STATUS_REOPENED, null)).isEqualTo("REOPENED");
  }

  @Test
  void of_whenClosedWithResolutionRemoved_isReportedAsRemovedNotFixed() {
    assertThat(IssueTelemetryStatus.of(Issue.STATUS_CLOSED, Issue.RESOLUTION_REMOVED)).isEqualTo("REMOVED");
  }

  @Test
  void of_whenHotspotTransition_returnsNull() {
    assertThat(IssueTelemetryStatus.of(Issue.STATUS_TO_REVIEW, null)).isNull();
    assertThat(IssueTelemetryStatus.of(Issue.STATUS_REVIEWED, null)).isNull();
  }
}
