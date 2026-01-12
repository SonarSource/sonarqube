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
package org.sonar.server.platform.telemetry;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.issue.Issue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueCountByStatusAndResolution;
import org.sonar.db.issue.IssueDao;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryIssueCountsPerStatusProviderTest {

  @Mock
  private DbClient dbClient;

  @Mock
  private DbSession dbSession;

  @Mock
  private IssueDao issueDao;

  @InjectMocks
  private TelemetryIssueCountsPerStatusProvider underTest;

  @Test
  void getMetricKey_returnsCorrectKey() {
    assertThat(underTest.getMetricKey()).isEqualTo("issue_count_by_status");
  }

  @Test
  void getDimension_returnsInstallation() {
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
  }

  @Test
  void getGranularity_returnsDaily() {
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.DAILY);
  }

  @Test
  void getType_returnsInteger() {
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.INTEGER);
  }

  @Test
  void getValues_whenNoIssues_returnEmptyMap() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.issueDao()).thenReturn(issueDao);
    when(issueDao.countIssuesByStatusOnMainBranches(any())).thenReturn(List.of());

    Map<String, Integer> result = underTest.getValues();

    assertThat(result).isEmpty();
  }

  @Test
  void getValues_whenIssuesExist_mapIssuesByStatus() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.issueDao()).thenReturn(issueDao);

    // OPEN status (status=OPEN, resolution=null)
    IssueCountByStatusAndResolution open = new IssueCountByStatusAndResolution()
      .setStatus(Issue.STATUS_OPEN).setResolution(null).setCount(50);

    // CONFIRMED status (status=CONFIRMED, resolution=null)
    IssueCountByStatusAndResolution confirmed = new IssueCountByStatusAndResolution()
      .setStatus(Issue.STATUS_CONFIRMED).setResolution(null).setCount(30);

    // FALSE_POSITIVE status (status=RESOLVED, resolution=FALSE_POSITIVE)
    IssueCountByStatusAndResolution falsePositive = new IssueCountByStatusAndResolution()
      .setStatus(Issue.STATUS_RESOLVED).setResolution(Issue.RESOLUTION_FALSE_POSITIVE).setCount(10);

    // ACCEPTED status (status=RESOLVED, resolution=WONT_FIX)
    IssueCountByStatusAndResolution accepted = new IssueCountByStatusAndResolution()
      .setStatus(Issue.STATUS_RESOLVED).setResolution(Issue.RESOLUTION_WONT_FIX).setCount(15);

    // FIXED status (status=CLOSED, resolution=FIXED)
    IssueCountByStatusAndResolution fixed = new IssueCountByStatusAndResolution()
      .setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED).setCount(20);

    when(issueDao.countIssuesByStatusOnMainBranches(any())).thenReturn(
      List.of(open, confirmed, falsePositive, accepted, fixed)
    );

    Map<String, Integer> result = underTest.getValues();

    assertThat(result).containsExactlyInAnyOrderEntriesOf(
      Map.of(
        "open", 50,
        "confirmed", 30,
        "false_positive", 10,
        "accepted", 15,
        "fixed", 20
      )
    );
  }

  @Test
  void getValues_whenMultipleRowsMapToSameStatus_aggregatesCount() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.issueDao()).thenReturn(issueDao);

    // Two different DB rows that both map to FIXED status
    IssueCountByStatusAndResolution fixed1 = new IssueCountByStatusAndResolution()
      .setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED).setCount(10);
    IssueCountByStatusAndResolution fixed2 = new IssueCountByStatusAndResolution()
      .setStatus(Issue.STATUS_RESOLVED).setResolution(Issue.RESOLUTION_FIXED).setCount(5);

    when(issueDao.countIssuesByStatusOnMainBranches(any())).thenReturn(
      List.of(fixed1, fixed2)
    );

    Map<String, Integer> result = underTest.getValues();

    assertThat(result).containsExactly(Map.entry("fixed", 15));
  }

  @Test
  void getValues_whenIssuesInWeirdState_shouldNotFail() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.issueDao()).thenReturn(issueDao);

    IssueCountByStatusAndResolution empty = new IssueCountByStatusAndResolution();

    when(issueDao.countIssuesByStatusOnMainBranches(any())).thenReturn(
      List.of(empty)
    );

    Map<String, Integer> result = underTest.getValues();

    // If the issue doesn't have a status, it falls back to OPEN
    assertThat(result).containsEntry("open", 0);
  }
}
