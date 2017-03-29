/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.issue.notification;

import com.google.common.collect.Lists;
import java.util.Date;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.user.index.UserIndex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.ASSIGNEE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.COMPONENT;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.DEBT;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.RULE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.SEVERITY;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.TAG;

public class NewIssuesNotificationTest {

  NewIssuesStatistics.Stats stats = new NewIssuesStatistics.Stats();
  UserIndex userIndex = mock(UserIndex.class);
  DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
  Durations durations = mock(Durations.class);
  NewIssuesNotification underTest = new NewIssuesNotification(userIndex, dbClient, durations);

  @Test
  public void set_project() {
    underTest.setProject("project-key", "project-uuid", "project-long-name");

    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_NAME)).isEqualTo("project-long-name");
    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_UUID)).isEqualTo("project-uuid");
    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_KEY)).isEqualTo("project-key");
  }

  @Test
  public void set_date() {
    Date date = new Date();

    underTest.setAnalysisDate(date);

    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_DATE)).isEqualTo(DateUtils.formatDateTime(date));
  }

  @Test
  public void set_statistics() {
    addIssueNTimes(newIssue1(), 5);
    addIssueNTimes(newIssue2(), 3);
    when(dbClient.componentDao().selectOrFailByUuid(any(DbSession.class), eq("file-uuid")).name()).thenReturn("file-name");
    when(dbClient.componentDao().selectOrFailByUuid(any(DbSession.class), eq("directory-uuid")).name()).thenReturn("directory-name");
    when(dbClient.ruleDao().selectOrFailDefinitionByKey(any(DbSession.class), eq(RuleKey.of("SonarQube", "rule-the-world")))).thenReturn(newRule("Rule the World", "Java"));
    when(dbClient.ruleDao().selectOrFailDefinitionByKey(any(DbSession.class), eq(RuleKey.of("SonarQube", "rule-the-universe")))).thenReturn(newRule("Rule the Universe", "Clojure"));

    underTest.setStatistics("project-long-name", stats);

    assertThat(underTest.getFieldValue(SEVERITY + ".INFO.count")).isEqualTo("5");
    assertThat(underTest.getFieldValue(SEVERITY + ".BLOCKER.count")).isEqualTo("3");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".1.label")).isEqualTo("maynard");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".1.count")).isEqualTo("5");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".2.label")).isEqualTo("keenan");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".2.count")).isEqualTo("3");
    assertThat(underTest.getFieldValue(TAG + ".1.label")).isEqualTo("owasp");
    assertThat(underTest.getFieldValue(TAG + ".1.count")).isEqualTo("8");
    assertThat(underTest.getFieldValue(TAG + ".2.label")).isEqualTo("bug");
    assertThat(underTest.getFieldValue(TAG + ".2.count")).isEqualTo("5");
    assertThat(underTest.getFieldValue(COMPONENT + ".1.label")).isEqualTo("file-name");
    assertThat(underTest.getFieldValue(COMPONENT + ".1.count")).isEqualTo("5");
    assertThat(underTest.getFieldValue(COMPONENT + ".2.label")).isEqualTo("directory-name");
    assertThat(underTest.getFieldValue(COMPONENT + ".2.count")).isEqualTo("3");
    assertThat(underTest.getFieldValue(RULE + ".1.label")).isEqualTo("Rule the World (Java)");
    assertThat(underTest.getFieldValue(RULE + ".1.count")).isEqualTo("5");
    assertThat(underTest.getFieldValue(RULE + ".2.label")).isEqualTo("Rule the Universe (Clojure)");
    assertThat(underTest.getFieldValue(RULE + ".2.count")).isEqualTo("3");
    assertThat(underTest.getDefaultMessage()).startsWith("8 new issues on project-long-name");
  }

  @Test
  public void set_debt() {
    when(durations.format(any(Duration.class))).thenReturn("55 min");

    underTest.setDebt(Duration.create(55));

    assertThat(underTest.getFieldValue(DEBT + ".count")).isEqualTo("55 min");
  }

  private void addIssueNTimes(DefaultIssue issue, int times) {
    for (int i = 0; i < times; i++) {
      stats.add(issue);
    }
  }

  private DefaultIssue newIssue1() {
    return new DefaultIssue()
      .setAssignee("maynard")
      .setComponentUuid("file-uuid")
      .setSeverity(Severity.INFO)
      .setTags(Lists.newArrayList("bug", "owasp"))
      .setRuleKey(RuleKey.of("SonarQube", "rule-the-world"))
      .setEffort(Duration.create(5L));
  }

  private DefaultIssue newIssue2() {
    return new DefaultIssue()
      .setAssignee("keenan")
      .setComponentUuid("directory-uuid")
      .setSeverity(Severity.BLOCKER)
      .setTags(Lists.newArrayList("owasp"))
      .setRuleKey(RuleKey.of("SonarQube", "rule-the-universe"))
      .setEffort(Duration.create(10L));
  }

  private RuleDefinitionDto newRule(String name, String language) {
    return new RuleDefinitionDto()
      .setName(name)
      .setLanguage(language);
  }
}
