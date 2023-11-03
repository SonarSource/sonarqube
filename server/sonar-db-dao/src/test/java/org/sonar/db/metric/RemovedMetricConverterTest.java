/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.metric;

import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RemovedMetricConverterTest {

  @Test
  public void withRemovedMetricAlias_whenListContainsWontFix_shouldReturnListWithAccepted() {
    List<String> coreMetrics = List.of("wont_fix_issues", "blocker_violations", "critical_violations");

    List<String> upToDateMetrics = RemovedMetricConverter.withRemovedMetricAlias(coreMetrics);

    assertThat(upToDateMetrics).containsExactlyInAnyOrder("accepted_issues", "blocker_violations", "critical_violations");
  }

  @Test
  public void withRemovedMetricAlias_whenListContainsAccepted_shouldReturnListWithAccepted() {
    List<String> coreMetrics = List.of("accepted_issues", "blocker_violations", "critical_violations");

    List<String> upToDateMetrics = RemovedMetricConverter.withRemovedMetricAlias(coreMetrics);

    assertThat(upToDateMetrics).containsExactlyInAnyOrder("accepted_issues", "blocker_violations", "critical_violations");
  }

  @Test
  public void includeRenamedMetrics_whenWontFixIssuesPassed_shouldReturnAccepted() {
    String upToDateMetric = RemovedMetricConverter.includeRenamedMetrics("wont_fix_issues");

    assertThat(upToDateMetric).isEqualTo("accepted_issues");
  }

  @Test
  public void includeRenamedMetrics_whenAcceptedIssuesPassed_shouldReturnAccepted() {
    String upToDateMetric = RemovedMetricConverter.includeRenamedMetrics("accepted_issues");

    assertThat(upToDateMetric).isEqualTo("accepted_issues");
  }
}