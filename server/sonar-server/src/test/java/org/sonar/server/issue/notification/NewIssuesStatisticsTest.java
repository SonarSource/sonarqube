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
import org.sonar.core.issue.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.Duration;
import org.sonar.server.issue.notification.NewIssuesStatistics.Metric;

import static org.assertj.core.api.Assertions.assertThat;

public class NewIssuesStatisticsTest {

  NewIssuesStatistics underTest = new NewIssuesStatistics();

  @Test
  public void add_issues_with_correct_global_statistics() {
    DefaultIssue issue = defaultIssue();

    underTest.add(issue);
    underTest.add(issue.setAssignee("james"));
    underTest.add(issue.setAssignee("keenan"));

    assertThat(countDistribution(Metric.ASSIGNEE, "maynard")).isEqualTo(1);
    assertThat(countDistribution(Metric.ASSIGNEE, "james")).isEqualTo(1);
    assertThat(countDistribution(Metric.ASSIGNEE, "keenan")).isEqualTo(1);
    assertThat(countDistribution(Metric.ASSIGNEE, "wrong.login")).isEqualTo(0);
    assertThat(countDistribution(Metric.COMPONENT, "file-uuid")).isEqualTo(3);
    assertThat(countDistribution(Metric.COMPONENT, "wrong-uuid")).isEqualTo(0);
    assertThat(countDistribution(Metric.SEVERITY, Severity.INFO)).isEqualTo(3);
    assertThat(countDistribution(Metric.SEVERITY, Severity.CRITICAL)).isEqualTo(0);
    assertThat(countDistribution(Metric.TAG, "owasp")).isEqualTo(3);
    assertThat(countDistribution(Metric.TAG, "wrong-tag")).isEqualTo(0);
    assertThat(countDistribution(Metric.RULE, "SonarQube:rule-the-world")).isEqualTo(3);
    assertThat(countDistribution(Metric.RULE, "SonarQube:has-a-fake-rule")).isEqualTo(0);
    assertThat(underTest.globalStatistics().debt().toMinutes()).isEqualTo(15L);
    assertThat(underTest.globalStatistics().hasIssues()).isTrue();
    assertThat(underTest.hasIssues()).isTrue();
    assertThat(underTest.assigneesStatistics().get("maynard").hasIssues()).isTrue();
  }

  @Test
  public void do_not_have_issues_when_no_issue_added() {
    assertThat(underTest.globalStatistics().hasIssues()).isFalse();
  }

  private int countDistribution(Metric metric, String label) {
    return underTest.globalStatistics().countForMetric(metric, label);
  }

  private DefaultIssue defaultIssue() {
    return new DefaultIssue()
      .setAssignee("maynard")
      .setComponentUuid("file-uuid")
      .setNew(true)
      .setSeverity(Severity.INFO)
      .setRuleKey(RuleKey.of("SonarQube", "rule-the-world"))
      .setTags(Lists.newArrayList("bug", "owasp"))
      .setDebt(Duration.create(5L));
  }
}
