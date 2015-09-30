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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.COMPONENT;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.DEBT;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.RULE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.SEVERITY;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.TAG;

public class MyNewIssuesEmailTemplateTest {

  MyNewIssuesEmailTemplate underTest;
  DefaultI18n i18n;
  UserIndex userIndex;
  Date date;

  @Before
  public void setUp() {
    EmailSettings settings = mock(EmailSettings.class);
    when(settings.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
    i18n = mock(DefaultI18n.class);
    date = new Date();
    userIndex = mock(UserIndex.class);
    // returns the login passed in parameter
    when(userIndex.getNullableByLogin(anyString())).thenAnswer(new Answer<UserDoc>() {
      @Override
      public UserDoc answer(InvocationOnMock invocationOnMock) throws Throwable {
        return new UserDoc().setName((String) invocationOnMock.getArguments()[0]);
      }
    });
    when(i18n.message(any(Locale.class), eq("severity.BLOCKER"), anyString())).thenReturn("Blocker");
    when(i18n.message(any(Locale.class), eq("severity.CRITICAL"), anyString())).thenReturn("Critical");
    when(i18n.message(any(Locale.class), eq("severity.MAJOR"), anyString())).thenReturn("Major");
    when(i18n.message(any(Locale.class), eq("severity.MINOR"), anyString())).thenReturn("Minor");
    when(i18n.message(any(Locale.class), eq("severity.INFO"), anyString())).thenReturn("Info");

    underTest = new MyNewIssuesEmailTemplate(settings, i18n);
  }

  @Test
  public void no_format_if_not_the_correct_notif() {
    Notification notification = new Notification("new-issues");
    EmailMessage message = underTest.format(notification);
    assertThat(message).isNull();
  }

  @Test
  public void format_email_with_all_fields_filled() throws Exception {
    Notification notification = newNotification();
    addTags(notification);
    addRules(notification);
    addComponents(notification);

    EmailMessage message = underTest.format(notification);

    // TODO datetime to be completed when test is isolated from JVM timezone
    assertStartsWithFile(message.getMessage(), getClass().getResource("MyNewIssuesEmailTemplateTest/email_with_all_details.txt"));
  }

  @Test
  public void message_id() {
    Notification notification = newNotification();

    EmailMessage message = underTest.format(notification);

    assertThat(message.getMessageId()).isEqualTo("my-new-issues/org.apache:struts");
  }

  @Test
  public void subject() {
    Notification notification = newNotification();

    EmailMessage message = underTest.format(notification);

    assertThat(message.getSubject()).isEqualTo("You have 32 new issues on project Struts");
  }

  @Test
  public void format_email_with_no_assignees_tags_nor_components() throws Exception {
    Notification notification = newNotification();

    EmailMessage message = underTest.format(notification);

    // TODO datetime to be completed when test is isolated from JVM timezone
    assertStartsWithFile(message.getMessage(), getClass().getResource("MyNewIssuesEmailTemplateTest/email_with_no_assignee_tags_components.txt"));
  }

  @Test
  public void do_not_add_footer_when_properties_missing() {
    Notification notification = new Notification(MyNewIssuesNotification.MY_NEW_ISSUES_NOTIF_TYPE)
      .setFieldValue(SEVERITY + ".count", "32")
      .setFieldValue("projectName", "Struts");

    EmailMessage message = underTest.format(notification);
    assertThat(message.getMessage()).doesNotContain("See it");
  }

  private Notification newNotification() {
    return new Notification(MyNewIssuesNotification.MY_NEW_ISSUES_NOTIF_TYPE)
      .setFieldValue("projectName", "Struts")
      .setFieldValue("projectKey", "org.apache:struts")
      .setFieldValue("projectUuid", "ABCDE")
      .setFieldValue("projectDate", "2010-05-18T14:50:45+0000")
      .setFieldValue("assignee", "lo.gin")
      .setFieldValue(DEBT + ".count", "1d3h")
      .setFieldValue(SEVERITY + ".count", "32")
      .setFieldValue(SEVERITY + ".INFO.count", "1")
      .setFieldValue(SEVERITY + ".MINOR.count", "3")
      .setFieldValue(SEVERITY + ".MAJOR.count", "10")
      .setFieldValue(SEVERITY + ".CRITICAL.count", "5")
      .setFieldValue(SEVERITY + ".BLOCKER.count", "0");
  }

  private void addTags(Notification notification) {
    notification
      .setFieldValue(TAG + ".1.label", "oscar")
      .setFieldValue(TAG + ".1.count", "3")
      .setFieldValue(TAG + ".2.label", "cesar")
      .setFieldValue(TAG + ".2.count", "10");
  }

  private void addComponents(Notification notification) {
    notification
      .setFieldValue(COMPONENT + ".1.label", "/path/to/file")
      .setFieldValue(COMPONENT + ".1.count", "3")
      .setFieldValue(COMPONENT + ".2.label", "/path/to/directory")
      .setFieldValue(COMPONENT + ".2.count", "7");
  }

  private void addRules(Notification notification) {
    notification
      .setFieldValue(RULE + ".1.label", "Rule the Universe (Clojure)")
      .setFieldValue(RULE + ".1.count", "42")
      .setFieldValue(RULE + ".2.label", "Rule the World (Java)")
      .setFieldValue(RULE + ".2.count", "5");
  }

  private static void assertStartsWithFile(String message, URL file) throws IOException {
    String fileContent = IOUtils.toString(file, StandardCharsets.UTF_8);
    assertThat(sanitizeString(message)).startsWith(sanitizeString(fileContent));
  }

  /**
   * sanitize EOL and tabs if git clone is badly configured
   */
  private static String sanitizeString(String s) {
    return s.replaceAll("\\r\\n|\\r|\\s+", "");
  }
}
