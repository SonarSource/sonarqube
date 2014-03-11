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
package org.sonar.plugins.core.issue.notification;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.Component;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.issue.IssueNotifications;
import org.sonar.core.issue.IssuesBySeverity;

import java.util.Arrays;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SendIssueNotificationsPostJobTest {
  @Mock
  Project project;

  @Mock
  IssueCache issueCache;

  @Mock
  IssueNotifications notifications;

  @Mock
  RuleFinder ruleFinder;

  @Mock
  SensorContext sensorContext;

  @Test
  public void should_send_notif_if_new_issues() throws Exception {
    when(project.getAnalysisDate()).thenReturn(DateUtils.parseDate("2013-05-18"));
    when(issueCache.all()).thenReturn(Arrays.asList(
      new DefaultIssue().setNew(true).setSeverity("MAJOR"),
      new DefaultIssue().setNew(false).setSeverity("MINOR")
      ));

    SendIssueNotificationsPostJob job = new SendIssueNotificationsPostJob(issueCache, notifications, ruleFinder);
    job.executeOn(project, sensorContext);

    ArgumentCaptor<IssuesBySeverity> argument = ArgumentCaptor.forClass(IssuesBySeverity.class);
    verify(notifications).sendNewIssues(eq(project), argument.capture());
    assertThat(argument.getValue().size()).isEqualTo(1);
  }

  @Test
  public void should_not_send_notif_if_no_new_issues() throws Exception {
    when(project.getAnalysisDate()).thenReturn(DateUtils.parseDate("2013-05-18"));
    when(issueCache.all()).thenReturn(Arrays.asList(
      new DefaultIssue().setNew(false)
      ));

    SendIssueNotificationsPostJob job = new SendIssueNotificationsPostJob(issueCache, notifications, ruleFinder);
    job.executeOn(project, sensorContext);

    verifyZeroInteractions(notifications);
  }

  @Test
  public void should_send_notification() throws Exception {
    when(project.getAnalysisDate()).thenReturn(DateUtils.parseDate("2013-05-18"));
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycles");
    Rule rule = new Rule("squid", "AvoidCycles");
    DefaultIssue issue = new DefaultIssue()
      .setNew(false)
      .setChanged(true)
      .setSendNotifications(true)
      .setFieldChange(mock(IssueChangeContext.class), "severity", "MINOR", "BLOCKER")
      .setRuleKey(ruleKey);
    when(issueCache.all()).thenReturn(Arrays.asList(issue));
    when(ruleFinder.findByKey(ruleKey)).thenReturn(rule);

    SendIssueNotificationsPostJob job = new SendIssueNotificationsPostJob(issueCache, notifications, ruleFinder);
    job.executeOn(project, sensorContext);

    verify(notifications).sendChanges(argThat(matchMapOf(issue, rule)), any(IssueChangeContext.class), any(Component.class), (Component) isNull());
  }

  @Test
  public void should_not_send_notification_if_issue_change_on_removed_rule() throws Exception {
    IssueChangeContext changeContext = mock(IssueChangeContext.class);

    when(project.getAnalysisDate()).thenReturn(DateUtils.parseDate("2013-05-18"));
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycles");
    DefaultIssue issue = new DefaultIssue()
      .setChanged(true)
      .setFieldChange(changeContext, "severity", "MINOR", "BLOCKER")
      .setRuleKey(ruleKey);
    when(issueCache.all()).thenReturn(Arrays.asList(issue));
    when(ruleFinder.findByKey(ruleKey)).thenReturn(null);

    SendIssueNotificationsPostJob job = new SendIssueNotificationsPostJob(issueCache, notifications, ruleFinder);
    job.executeOn(project, sensorContext);

    verify(notifications, never()).sendChanges(argThat(matchMapOf(issue, null)), eq(changeContext), any(Component.class), any(Component.class));
  }

  @Test
  public void should_not_send_notification_on_any_change() throws Exception {
    IssueChangeContext changeContext = mock(IssueChangeContext.class);

    when(project.getAnalysisDate()).thenReturn(DateUtils.parseDate("2013-05-18"));
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycles");
    DefaultIssue issue = new DefaultIssue()
      .setChanged(true)
      .setSendNotifications(false)
      .setFieldChange(changeContext, "severity", "MINOR", "BLOCKER")
      .setRuleKey(ruleKey);
    when(issueCache.all()).thenReturn(Arrays.asList(issue));
    when(ruleFinder.findByKey(ruleKey)).thenReturn(null);

    SendIssueNotificationsPostJob job = new SendIssueNotificationsPostJob(issueCache, notifications, ruleFinder);
    job.executeOn(project, sensorContext);

    verify(notifications, never()).sendChanges(argThat(matchMapOf(issue, null)), eq(changeContext), any(Component.class), any(Component.class));
  }

  private static IsMapOfIssueAndRule matchMapOf(DefaultIssue issue, Rule rule) {
    return new IsMapOfIssueAndRule(issue, rule);
  }

  static class IsMapOfIssueAndRule extends ArgumentMatcher<Map<DefaultIssue, Rule>> {
    private DefaultIssue issue;
    private Rule rule;

    public IsMapOfIssueAndRule(DefaultIssue issue, Rule rule) {
      this.issue = issue;
      this.rule = rule;
    }

    public boolean matches(Object arg) {
      Map map = (Map) arg;
      return map.size() == 1 && map.get(issue) != null && map.get(issue).equals(rule);
    }
  }
}
