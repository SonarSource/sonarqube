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
  public void should_format_change() {
    Notification notification = new Notification("issue-changes")
      .setFieldValue("projectName", "Struts")
      .setFieldValue("projectKey", "org.apache:struts")
      .setFieldValue("key", "ABCDE")
      .setFieldValue("ruleName", "Avoid Cycles")
      .setFieldValue("message", "Has 3 cycles")
      .setFieldValue("comment", "How to fix it?")
      .setFieldValue("old.assignee", "simon")
      .setFieldValue("new.assignee", "louis")
      .setFieldValue("new.resolution", "FALSE-POSITIVE")
      .setFieldValue("new.status", "RESOLVED");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId()).isEqualTo("issue-changes/ABCDE");
    assertThat(message.getSubject()).isEqualTo("Project Struts, change on issue #ABCDE");
    assertThat(message.getMessage()).contains("Rule: Avoid Cycles");
    assertThat(message.getMessage()).contains("Message: Has 3 cycles");
    assertThat(message.getMessage()).contains("Comment: How to fix it?");
    assertThat(message.getMessage()).contains("Assignee: louis (was simon)");
    assertThat(message.getMessage()).contains("Resolution: FALSE-POSITIVE");
    assertThat(message.getMessage()).contains("See it in SonarQube: http://nemo.sonarsource.org/issue/show/ABCDE");
    assertThat(message.getFrom()).isNull();
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

}
