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

package org.sonar.server.issue.notification;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.Duration;
import org.sonar.server.issue.notification.NewIssuesStatistics.METRIC;

import static org.assertj.core.api.Assertions.assertThat;

public class NewIssuesStatisticsTest {

  NewIssuesStatistics sut = new NewIssuesStatistics();

  @Test
  public void add_issues_with_correct_global_statistics() throws Exception {
    DefaultIssue issue = defaultIssue();

    sut.add(issue);
    sut.add(issue.setAssignee("james"));
    sut.add(issue.setAssignee("keenan"));

    assertThat(countDistribution(METRIC.ASSIGNEE, "maynard")).isEqualTo(1);
    assertThat(countDistribution(METRIC.ASSIGNEE, "james")).isEqualTo(1);
    assertThat(countDistribution(METRIC.ASSIGNEE, "keenan")).isEqualTo(1);
    assertThat(countDistribution(METRIC.ASSIGNEE, "wrong.login")).isEqualTo(0);
    assertThat(countDistribution(METRIC.COMPONENT, "file-uuid")).isEqualTo(3);
    assertThat(countDistribution(METRIC.COMPONENT, "wrong-uuid")).isEqualTo(0);
    assertThat(countDistribution(METRIC.SEVERITY, Severity.INFO)).isEqualTo(3);
    assertThat(countDistribution(METRIC.SEVERITY, Severity.CRITICAL)).isEqualTo(0);
    assertThat(countDistribution(METRIC.TAGS, "owasp")).isEqualTo(3);
    assertThat(countDistribution(METRIC.TAGS, "wrong-tag")).isEqualTo(0);
    assertThat(sut.globalStatistics().debt().toMinutes()).isEqualTo(15L);
    assertThat(sut.globalStatistics().hasIssues()).isTrue();
    assertThat(sut.hasIssues()).isTrue();
    assertThat(sut.assigneesStatistics().get("maynard").hasIssues()).isTrue();
  }

  @Test
  public void do_not_have_issues_when_no_issue_added() throws Exception {
    assertThat(sut.globalStatistics().hasIssues()).isFalse();
  }

  private int countDistribution(METRIC metric, String label) {
    return sut.globalStatistics().countForMetric(metric, label);
  }

  private DefaultIssue defaultIssue() {
    return new DefaultIssue()
      .setAssignee("maynard")
      .setComponentUuid("file-uuid")
      .setNew(true)
      .setSeverity(Severity.INFO)
      .setTags(Lists.newArrayList("bug", "owasp"))
      .setDebt(Duration.create(5L));
  }
}
