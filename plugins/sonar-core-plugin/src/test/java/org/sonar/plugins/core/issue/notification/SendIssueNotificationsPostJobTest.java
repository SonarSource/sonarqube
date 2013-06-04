/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import java.util.Arrays;

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
      new DefaultIssue().setNew(true),
      new DefaultIssue().setNew(false)
    ));

    SendIssueNotificationsPostJob job = new SendIssueNotificationsPostJob(issueCache, notifications, ruleFinder);
    job.executeOn(project, sensorContext);

    verify(notifications).sendNewIssues(project, 1);
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
  public void should_send_notif_if_issue_change() throws Exception {

    when(project.getAnalysisDate()).thenReturn(DateUtils.parseDate("2013-05-18"));
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycles");
    Rule rule = new Rule("squid", "AvoidCycles");
    DefaultIssue issue = new DefaultIssue()
      .setChanged(true)
      .setFieldChange(mock(IssueChangeContext.class), "severity", "MINOR", "BLOCKER")
      .setRuleKey(ruleKey);
    when(issueCache.all()).thenReturn(Arrays.asList(issue));
    when(ruleFinder.findByKey(ruleKey)).thenReturn(rule);

    SendIssueNotificationsPostJob job = new SendIssueNotificationsPostJob(issueCache, notifications, ruleFinder);
    job.executeOn(project, sensorContext);

    verify(notifications).sendChanges(eq(issue), any(IssueChangeContext.class), eq(rule), any(Component.class), (Component)isNull());
  }

  @Test
  public void should_not_send_notif_if_issue_change_on_removed_rule() throws Exception {
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

    verify(notifications, never()).sendChanges(eq(issue), eq(changeContext), any(Rule.class), any(Component.class), any(Component.class));
  }
}
