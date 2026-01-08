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
package org.sonar.db.issue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.Issue;

import static org.assertj.core.api.Assertions.assertThat;

class IssueCountByStatusAndResolutionTest {

  @Test
  void toStatusMap_whenEmptyList_shouldReturnEmptyMap() {
    Map<String, Integer> result = IssueCountByStatusAndResolution.toStatusMap(List.of());

    assertThat(result).isEmpty();
  }

  @Test
  void toStatusMap_shouldMapAllStatusTypes() {
    List<IssueCountByStatusAndResolution> counts = List.of(
      new IssueCountByStatusAndResolution().setStatus(Issue.STATUS_OPEN).setCount(10),
      new IssueCountByStatusAndResolution().setStatus(Issue.STATUS_CONFIRMED).setCount(20),
      new IssueCountByStatusAndResolution().setStatus(Issue.STATUS_RESOLVED).setResolution(Issue.RESOLUTION_FALSE_POSITIVE).setCount(5),
      new IssueCountByStatusAndResolution().setStatus(Issue.STATUS_RESOLVED).setResolution(Issue.RESOLUTION_WONT_FIX).setCount(8),
      new IssueCountByStatusAndResolution().setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED).setCount(15)
    );

    Map<String, Integer> result = IssueCountByStatusAndResolution.toStatusMap(counts);

    assertThat(result)
      .containsEntry("open", 10)
      .containsEntry("confirmed", 20)
      .containsEntry("false_positive", 5)
      .containsEntry("accepted", 8)
      .containsEntry("fixed", 15)
      .hasSize(5);
  }

  @Test
  void toStatusMap_shouldAggregateMultipleRowsForSameStatus() {
    List<IssueCountByStatusAndResolution> counts = List.of(
      new IssueCountByStatusAndResolution().setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED).setCount(10),
      new IssueCountByStatusAndResolution().setStatus(Issue.STATUS_RESOLVED).setResolution(Issue.RESOLUTION_FIXED).setCount(5)
    );

    Map<String, Integer> result = IssueCountByStatusAndResolution.toStatusMap(counts);

    assertThat(result)
      .containsEntry("fixed", 15)
      .hasSize(1);
  }

}
