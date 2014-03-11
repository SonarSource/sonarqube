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

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.test.TestUtils;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueChangesEmailTemplateTest {

  @Mock
  UserFinder userFinder;

  IssueChangesEmailTemplate template;

  @Before
  public void setUp() {
    EmailSettings settings = mock(EmailSettings.class);
    when(settings.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
    template = new IssueChangesEmailTemplate(settings, userFinder);
  }

  @Test
  public void should_ignore_non_issue_changes() {
    Notification notification = new Notification("other");
    EmailMessage message = template.format(notification);
    assertThat(message).isNull();
  }

  @Test
  public void email_should_display_assignee_change() throws Exception {
    Notification notification = generateNotification()
      .setFieldValue("old.assignee", "simon")
      .setFieldValue("new.assignee", "louis");

    EmailMessage email = template.format(notification);
    assertThat(email.getMessageId()).isEqualTo("issue-changes/ABCDE");
    assertThat(email.getSubject()).isEqualTo("Struts, change on issue #ABCDE");

    String message = email.getMessage();
    String expectedMessage = TestUtils.getResourceContent("/org/sonar/plugins/core/issue/notification/IssueChangesEmailTemplateTest/email_with_assignee_change.txt");
    expectedMessage = StringUtils.remove(expectedMessage, '\r');
    assertThat(message).isEqualTo(expectedMessage);
    assertThat(email.getFrom()).isNull();
  }

  @Test
  public void email_should_display_plan_change() throws Exception {
    Notification notification = generateNotification()
      .setFieldValue("old.actionPlan", null)
      .setFieldValue("new.actionPlan", "ABC 1.0");

    EmailMessage email = template.format(notification);
    assertThat(email.getMessageId()).isEqualTo("issue-changes/ABCDE");
    assertThat(email.getSubject()).isEqualTo("Struts, change on issue #ABCDE");

    String message = email.getMessage();
    String expectedMessage = TestUtils.getResourceContent("/org/sonar/plugins/core/issue/notification/IssueChangesEmailTemplateTest/email_with_action_plan_change.txt");
    expectedMessage = StringUtils.remove(expectedMessage, '\r');
    assertThat(message).isEqualTo(expectedMessage);
    assertThat(email.getFrom()).isNull();
  }

  @Test
  public void test_email_with_multiple_changes() {
    Notification notification = generateNotification()
      .setFieldValue("comment", "How to fix it?")
      .setFieldValue("old.assignee", "simon")
      .setFieldValue("new.assignee", "louis")
      .setFieldValue("new.resolution", "FALSE-POSITIVE")
      .setFieldValue("new.status", "RESOLVED");

    EmailMessage email = template.format(notification);
    assertThat(email.getMessageId()).isEqualTo("issue-changes/ABCDE");
    assertThat(email.getSubject()).isEqualTo("Struts, change on issue #ABCDE");

    String message = email.getMessage();
    String expectedMessage = TestUtils.getResourceContent("/org/sonar/plugins/core/issue/notification/IssueChangesEmailTemplateTest/email_with_multiple_changes.txt");
    expectedMessage = StringUtils.remove(expectedMessage, '\r');
    assertThat(message).isEqualTo(expectedMessage);
    assertThat(email.getFrom()).isNull();
  }

  @Test
  public void notification_sender_should_be_the_author_of_change() {
    User user = mock(User.class);
    when(user.name()).thenReturn("Simon");
    when(userFinder.findByLogin("simon")).thenReturn(user);

    Notification notification = new Notification("issue-changes")
      .setFieldValue("projectName", "Struts")
      .setFieldValue("projectKey", "org.apache:struts")
      .setFieldValue("changeAuthor", "simon");

    EmailMessage message = template.format(notification);
    assertThat(message.getFrom()).isEqualTo("Simon");
  }

  private Notification generateNotification() {
    Notification notification = new Notification("issue-changes")
      .setFieldValue("projectName", "Struts")
      .setFieldValue("projectKey", "org.apache:struts")
      .setFieldValue("componentName", "org.apache.struts.Action")
      .setFieldValue("key", "ABCDE")
      .setFieldValue("ruleName", "Avoid Cycles")
      .setFieldValue("message", "Has 3 cycles");
    return notification;
  }
}
