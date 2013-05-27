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
package org.sonar.core.issue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.component.Component;
import org.sonar.api.issue.Issue;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.i18n.RuleI18nManager;

import java.util.Arrays;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class IssueNotificationsTest {

  @Mock
  NotificationManager manager;

  @Mock
  RuleI18nManager ruleI18n;

  IssueNotifications issueNotifications;

  @Before
  public void setUp() throws Exception {
    issueNotifications = new IssueNotifications(manager, ruleI18n);
  }

  @Test
  public void sendNewIssues() throws Exception {
    Date date = DateUtils.parseDateTime("2013-05-18T00:00:03+0200");
    Project project = new Project("struts").setAnalysisDate(date);
    Notification notification = issueNotifications.sendNewIssues(project, 42);

    assertThat(notification.getFieldValue("count")).isEqualTo("42");
    assertThat(notification.getFieldValue("projectDate")).isEqualTo("2013-05-18T00:00:03+0200");
    Mockito.verify(manager).scheduleForSending(notification);
  }

  @Test
  public void sendChanges() throws Exception {
    IssueChangeContext context = IssueChangeContext.createScan(new Date());
    DefaultIssue issue = new DefaultIssue()
      .setMessage("the message")
      .setKey("ABCDE")
      .setAssignee("freddy")
      .setFieldDiff(context, "resolution", null, "FIXED")
      .setFieldDiff(context, "status", "OPEN", "RESOLVED")
      .setComponentKey("struts:Action")
      .setProjectKey("struts");
    DefaultIssueQueryResult queryResult = new DefaultIssueQueryResult();
    queryResult.setIssues(Arrays.<Issue>asList(issue));
    queryResult.addProjects(Arrays.<Component>asList(new Project("struts")));

    Notification notification = issueNotifications.sendChanges(issue, context, queryResult);

    assertThat(notification.getFieldValue("message")).isEqualTo("the message");
    assertThat(notification.getFieldValue("key")).isEqualTo("ABCDE");
    assertThat(notification.getFieldValue("old.resolution")).isNull();
    assertThat(notification.getFieldValue("new.resolution")).isEqualTo("FIXED");
    assertThat(notification.getFieldValue("old.status")).isEqualTo("OPEN");
    assertThat(notification.getFieldValue("new.status")).isEqualTo("RESOLVED");
    Mockito.verify(manager).scheduleForSending(notification);
  }
}
