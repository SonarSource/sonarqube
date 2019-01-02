/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.io.Resources;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.plugins.emailnotifications.api.EmailMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.CoreProperties.SERVER_BASE_URL;

public class IssueChangesEmailTemplateTest {

  @Rule
  public DbTester db = DbTester.create();

  private MapSettings settings = new MapSettings().setProperty(SERVER_BASE_URL, "http://nemo.sonarsource.org");

  private IssueChangesEmailTemplate underTest = new IssueChangesEmailTemplate(db.getDbClient(), new EmailSettings(settings.asConfig()));

  @Test
  public void should_ignore_non_issue_changes() {
    Notification notification = new Notification("other");
    EmailMessage message = underTest.format(notification);
    assertThat(message).isNull();
  }

  @Test
  public void email_should_display_assignee_change() throws Exception {
    Notification notification = generateNotification()
      .setFieldValue("old.assignee", "simon")
      .setFieldValue("new.assignee", "louis");

    EmailMessage email = underTest.format(notification);
    assertThat(email.getMessageId()).isEqualTo("issue-changes/ABCDE");
    assertThat(email.getSubject()).isEqualTo("Struts, change on issue #ABCDE");

    String message = email.getMessage();
    String expected = Resources.toString(Resources.getResource(
      "org/sonar/server/issue/notification/IssueChangesEmailTemplateTest/email_with_assignee_change.txt"),
      StandardCharsets.UTF_8);
    expected = StringUtils.remove(expected, '\r');
    assertThat(message).isEqualTo(expected);
    assertThat(email.getFrom()).isNull();
  }

  @Test
  public void email_should_display_plan_change() throws Exception {
    Notification notification = generateNotification()
      .setFieldValue("old.actionPlan", null)
      .setFieldValue("new.actionPlan", "ABC 1.0");

    EmailMessage email = underTest.format(notification);
    assertThat(email.getMessageId()).isEqualTo("issue-changes/ABCDE");
    assertThat(email.getSubject()).isEqualTo("Struts, change on issue #ABCDE");

    String message = email.getMessage();
    String expected = Resources.toString(Resources.getResource(
      "org/sonar/server/issue/notification/IssueChangesEmailTemplateTest/email_with_action_plan_change.txt"),
      StandardCharsets.UTF_8);
    expected = StringUtils.remove(expected, '\r');
    assertThat(message).isEqualTo(expected);
    assertThat(email.getFrom()).isNull();
  }

  @Test
  public void email_should_display_resolution_change() throws Exception {
    Notification notification = generateNotification()
      .setFieldValue("old.resolution", "FALSE-POSITIVE")
      .setFieldValue("new.resolution", "FIXED");

    EmailMessage email = underTest.format(notification);
    assertThat(email.getMessageId()).isEqualTo("issue-changes/ABCDE");
    assertThat(email.getSubject()).isEqualTo("Struts, change on issue #ABCDE");

    String message = email.getMessage();
    String expected = Resources.toString(Resources.getResource(
      "org/sonar/server/issue/notification/IssueChangesEmailTemplateTest/email_should_display_resolution_change.txt"),
      StandardCharsets.UTF_8);
    expected = StringUtils.remove(expected, '\r');
    assertThat(message).isEqualTo(expected);
    assertThat(email.getFrom()).isNull();
  }

  @Test
  public void display_component_key_if_no_component_name() throws Exception {
    Notification notification = generateNotification()
      .setFieldValue("componentName", null);

    EmailMessage email = underTest.format(notification);
    assertThat(email.getMessageId()).isEqualTo("issue-changes/ABCDE");
    assertThat(email.getSubject()).isEqualTo("Struts, change on issue #ABCDE");

    String message = email.getMessage();
    String expected = Resources.toString(Resources.getResource(
      "org/sonar/server/issue/notification/IssueChangesEmailTemplateTest/display_component_key_if_no_component_name.txt"),
      StandardCharsets.UTF_8);
    expected = StringUtils.remove(expected, '\r');
    assertThat(message).isEqualTo(expected);
  }

  @Test
  public void test_email_with_multiple_changes() throws Exception {
    Notification notification = generateNotification()
      .setFieldValue("comment", "How to fix it?")
      .setFieldValue("old.assignee", "simon")
      .setFieldValue("new.assignee", "louis")
      .setFieldValue("new.resolution", "FALSE-POSITIVE")
      .setFieldValue("new.status", "RESOLVED")
      .setFieldValue("new.type", "BUG")
      .setFieldValue("new.tags", "bug performance");

    EmailMessage email = underTest.format(notification);
    assertThat(email.getMessageId()).isEqualTo("issue-changes/ABCDE");
    assertThat(email.getSubject()).isEqualTo("Struts, change on issue #ABCDE");

    String message = email.getMessage();
    String expected = Resources.toString(Resources.getResource(
      "org/sonar/server/issue/notification/IssueChangesEmailTemplateTest/email_with_multiple_changes.txt"), StandardCharsets.UTF_8);
    expected = StringUtils.remove(expected, '\r');
    assertThat(message).isEqualTo(expected);
    assertThat(email.getFrom()).isNull();
  }

  @Test
  public void test_email_with_issue_on_branch() throws Exception {
    Notification notification = generateNotification()
      .setFieldValue("branch", "feature1");

    EmailMessage email = underTest.format(notification);
    assertThat(email.getMessageId()).isEqualTo("issue-changes/ABCDE");
    assertThat(email.getSubject()).isEqualTo("Struts, change on issue #ABCDE");

    String message = email.getMessage();
    String expected = Resources.toString(Resources.getResource(
      "org/sonar/server/issue/notification/IssueChangesEmailTemplateTest/email_with_issue_on_branch.txt"),
      StandardCharsets.UTF_8);
    expected = StringUtils.remove(expected, '\r');
    assertThat(message).isEqualTo(expected);
  }

  @Test
  public void notification_sender_should_be_the_author_of_change() {
    UserDto user = db.users().insertUser();

    Notification notification = new IssueChangeNotification()
      .setChangeAuthor(user)
      .setProject("Struts", "org.apache:struts", null, null);

    EmailMessage message = underTest.format(notification);
    assertThat(message.getFrom()).isEqualTo(user.getName());
  }

  @Test
  public void notification_contains_user_login_when_user_is_removed() {
    UserDto user = db.users().insertDisabledUser();

    Notification notification = new IssueChangeNotification()
      .setChangeAuthor(user)
      .setProject("Struts", "org.apache:struts", null, null);

    EmailMessage message = underTest.format(notification);
    assertThat(message.getFrom()).isEqualTo(user.getLogin());
  }

  private static Notification generateNotification() {
    return new IssueChangeNotification()
      .setFieldValue("projectName", "Struts")
      .setFieldValue("projectKey", "org.apache:struts")
      .setFieldValue("componentName", "Action")
      .setFieldValue("componentKey", "org.apache.struts.Action")
      .setFieldValue("key", "ABCDE")
      .setFieldValue("ruleName", "Avoid Cycles")
      .setFieldValue("message", "Has 3 cycles");
  }
}
