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

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.issue.notification.NewIssuesStatistics.METRIC.*;

public class NewIssuesEmailTemplateTest {

  private static final String EMAIL_HEADER = "Project: Struts\n\n";
  private static final String EMAIL_TOTAL_ISSUES = "32 new issues - Total debt: 1d3h\n\n";
  private static final String EMAIL_ISSUES = "   Severity - Blocker: 0   Critical: 5   Major: 10   Minor: 3   Info: 1\n";
  private static final String EMAIL_ASSIGNEES = "   Assignee - robin.williams: 5   al.pacino: 7   \n";
  private static final String EMAIL_TAGS = "   Tags - oscar: 3   cesar: 10   \n";
  private static final String EMAIL_COMPONENTS = "   Components:\n" +
    "      /path/to/file : 3\n" +
    "      /path/to/directory : 7\n";
  private static final String EMAIL_FOOTER = "\nSee it in SonarQube: http://nemo.sonarsource.org/issues/search#projectUuids=ABCDE|createdAt=2010-05-1";

  NewIssuesEmailTemplate template;
  DefaultI18n i18n;
  UserIndex userIndex;

  @Before
  public void setUp() {
    EmailSettings settings = mock(EmailSettings.class);
    when(settings.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
    i18n = mock(DefaultI18n.class);
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

    template = new NewIssuesEmailTemplate(settings, i18n, userIndex);
  }

  @Test
  public void shouldNotFormatIfNotCorrectNotification() {
    Notification notification = new Notification("other-notif");
    EmailMessage message = template.format(notification);
    assertThat(message).isNull();
  }

  /**
   * <pre>
   * Subject: Project Struts, new issues
   * From: Sonar
   *
   * Project: Foo
   * 32 new issues - Total debt: 1d3h
   *
   *    Severity - Blocker: 0   Critical: 5   Major: 10   Minor: 3   Info: 1
   *    Assignee - robin.williams: 5   al.pacino: 7
   *    Tags - oscar: 3   cesar:10
   *    Components:
   *       /path/to/file : 3
   *       /path/to/directoy : 7
   *
   * See it in SonarQube: http://nemo.sonarsource.org/drilldown/measures/org.sonar.foo:foo?metric=new_violations
   * </pre>
   */
  @Test
  public void format_email_with_all_fields_filled() {
    Notification notification = newNotification();
    addAssignees(notification);
    addTags(notification);
    addComponents(notification);

    EmailMessage message = template.format(notification);

    assertThat(message.getMessageId()).isEqualTo("new-issues/org.apache:struts");
    assertThat(message.getSubject()).isEqualTo("Struts: 32 new issues");

    // TODO datetime to be completed when test is isolated from JVM timezone
    assertThat(message.getMessage()).startsWith("" +
      EMAIL_HEADER +
      EMAIL_TOTAL_ISSUES +
      EMAIL_ISSUES +
      EMAIL_ASSIGNEES +
      EMAIL_TAGS +
      EMAIL_COMPONENTS +
      EMAIL_FOOTER);
  }

  @Test
  public void format_email_with_no_assignees_tags_nor_components() throws Exception {
    Notification notification = newNotification();

    when(i18n.message(any(Locale.class), eq("severity.BLOCKER"), anyString())).thenReturn("Blocker");
    when(i18n.message(any(Locale.class), eq("severity.CRITICAL"), anyString())).thenReturn("Critical");
    when(i18n.message(any(Locale.class), eq("severity.MAJOR"), anyString())).thenReturn("Major");
    when(i18n.message(any(Locale.class), eq("severity.MINOR"), anyString())).thenReturn("Minor");
    when(i18n.message(any(Locale.class), eq("severity.INFO"), anyString())).thenReturn("Info");

    EmailMessage message = template.format(notification);

    assertThat(message.getMessageId()).isEqualTo("new-issues/org.apache:struts");
    assertThat(message.getSubject()).isEqualTo("Struts: 32 new issues");

    // TODO datetime to be completed when test is isolated from JVM timezone
    assertThat(message.getMessage()).startsWith("" +
      EMAIL_HEADER +
      EMAIL_TOTAL_ISSUES +
      EMAIL_ISSUES +
      EMAIL_FOOTER);
  }

  @Test
  public void do_not_add_footer_when_properties_missing() {
    Notification notification = new NewIssuesNotification()
      .setFieldValue(SEVERITY + ".count", "32")
      .setFieldValue("projectName", "Struts");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessage()).doesNotContain("See it");
  }

  private Notification newNotification() {
    return new NewIssuesNotification()
      .setFieldValue("projectName", "Struts")
      .setFieldValue("projectKey", "org.apache:struts")
      .setFieldValue("projectUuid", "ABCDE")
      .setFieldValue("projectDate", "2010-05-18T14:50:45+0000")
      .setFieldValue(DEBT + ".count", "1d3h")
      .setFieldValue(SEVERITY + ".count", "32")
      .setFieldValue(SEVERITY + ".INFO.count", "1")
      .setFieldValue(SEVERITY + ".MINOR.count", "3")
      .setFieldValue(SEVERITY + ".MAJOR.count", "10")
      .setFieldValue(SEVERITY + ".CRITICAL.count", "5")
      .setFieldValue(SEVERITY + ".BLOCKER.count", "0");
  }

  private void addAssignees(Notification notification) {
    notification
      .setFieldValue(LOGIN + ".1.label", "robin.williams")
      .setFieldValue(LOGIN + ".1.count", "5")
      .setFieldValue(LOGIN + ".2.label", "al.pacino")
      .setFieldValue(LOGIN + ".2.count", "7");
  }

  private void addTags(Notification notification) {
    notification
      .setFieldValue(TAGS + ".1.label", "oscar")
      .setFieldValue(TAGS + ".1.count", "3")
      .setFieldValue(TAGS + ".2.label", "cesar")
      .setFieldValue(TAGS + ".2.count", "10");
  }

  private void addComponents(Notification notification) {
    notification
      .setFieldValue(COMPONENT + ".1.label", "/path/to/file")
      .setFieldValue(COMPONENT + ".1.count", "3")
      .setFieldValue(COMPONENT + ".2.label", "/path/to/directory")
      .setFieldValue(COMPONENT + ".2.count", "7");
  }
}
