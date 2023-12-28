/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.platform.Server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.COMPONENT;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.ISSUE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.RULE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.TAG;

public class MyNewIssuesEmailTemplateTest {

  private MapSettings settings = new MapSettings();

  private Server server = mock(Server.class);
  private MyNewIssuesEmailTemplate underTest = new MyNewIssuesEmailTemplate(new EmailSettings(settings.asConfig(), server));

  @Before
  public void setUp() {
    when(server.getPublicRootUrl()).thenReturn("http://nemo.sonarsource.org");
  }

  @Test
  public void no_format_if_not_the_correct_notif() {
    Notification notification = new Notification("new-issues");
    EmailMessage message = underTest.format(notification);
    assertThat(message).isNull();
  }

  @Test
  public void format_email_with_all_fields_filled() {
    Notification notification = newNotification(32);
    addTags(notification);
    addRules(notification);
    addComponents(notification);

    EmailMessage message = underTest.format(notification);

    // TODO datetime to be completed when test is isolated from JVM timezone
    assertThat(message.getMessage()).startsWith("""
      Project: Struts
      
      32 new issues
      
          Rules
              Rule the Universe (Clojure): 42
              Rule the World (Java): 5
      
          Tags
              oscar: 3
              cesar: 10
      
          Most impacted files
              /path/to/file: 3
              /path/to/directory: 7
      
      More details at: http://nemo.sonarsource.org/project/issues?id=org.apache%3Astruts&assignees=lo.gin&createdAt=2010-05-18""");
  }

  @Test
  public void message_id() {
    Notification notification = newNotification(32);

    EmailMessage message = underTest.format(notification);

    assertThat(message.getMessageId()).isEqualTo("my-new-issues/org.apache:struts");
  }

  @Test
  public void subject() {
    Notification notification = newNotification(32);

    EmailMessage message = underTest.format(notification);

    assertThat(message.getSubject()).isEqualTo("You have 32 new issues on project Struts");
  }

  @Test
  public void subject_on_branch() {
    Notification notification = newNotification(32)
      .setFieldValue("branch", "feature1");

    EmailMessage message = underTest.format(notification);

    assertThat(message.getSubject()).isEqualTo("You have 32 new issues on project Struts (feature1)");
  }

  @Test
  public void format_email_with_no_assignees_tags_nor_components() {
    Notification notification = newNotification(32)
      .setFieldValue("projectVersion", "52.0");

    EmailMessage message = underTest.format(notification);

    // TODO datetime to be completed when test is isolated from JVM timezone
    assertThat(message.getMessage()).startsWith("""
      Project: Struts
      Version: 52.0
      
      32 new issues
      
      More details at: http://nemo.sonarsource.org/project/issues?id=org.apache%3Astruts&assignees=lo.gin&createdAt=2010-05-18""");
  }

  @Test
  public void format_email_with_issue_on_branch() {
    Notification notification = newNotification(32)
      .setFieldValue("projectVersion", "52.0")
      .setFieldValue("branch", "feature1");

    EmailMessage message = underTest.format(notification);

    // TODO datetime to be completed when test is isolated from JVM timezone
    assertThat(message.getMessage()).startsWith("""
      Project: Struts
      Branch: feature1
      Version: 52.0
      
      32 new issues
      
      More details at: http://nemo.sonarsource.org/project/issues?id=org.apache%3Astruts&assignees=lo.gin&branch=feature1&createdAt=2010-05-18""");
  }

  @Test
  public void format_email_supports_single_issue() {
    Notification notification = newNotification(1);

    EmailMessage message = underTest.format(notification);

    assertThat(message.getSubject())
      .isEqualTo("You have 1 new issue on project Struts");
    assertThat(message.getMessage())
      .contains("1 new issue\n");
  }

  @Test
  public void format_supports_null_version() {
    Notification notification = newNotification(32)
      .setFieldValue("branch", "feature1");

    EmailMessage message = underTest.format(notification);

    // TODO datetime to be completed when test is isolated from JVM timezone
    assertThat(message.getMessage()).startsWith("""
      Project: Struts
      Branch: feature1
      
      32 new issues
      
      More details at: http://nemo.sonarsource.org/project/issues?id=org.apache%3Astruts&assignees=lo.gin&branch=feature1&createdAt=2010-05-18""");
  }

  @Test
  public void do_not_add_footer_when_properties_missing() {
    Notification notification = new Notification(MyNewIssuesNotification.MY_NEW_ISSUES_NOTIF_TYPE)
      .setFieldValue(ISSUE + ".count", "32")
      .setFieldValue("projectName", "Struts");

    EmailMessage message = underTest.format(notification);
    assertThat(message.getMessage()).doesNotContain("See it");
  }

  private Notification newNotification(int count) {
    return new Notification(MyNewIssuesNotification.MY_NEW_ISSUES_NOTIF_TYPE)
      .setFieldValue("projectName", "Struts")
      .setFieldValue("projectKey", "org.apache:struts")
      .setFieldValue("projectDate", "2010-05-18T14:50:45+0000")
      .setFieldValue("assignee", "lo.gin")
      .setFieldValue(ISSUE + ".count", String.valueOf(count));
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

  private void assertStartsWithFile(String message, String resourcePath) throws IOException {
    String fileContent = IOUtils.toString(getClass().getResource(resourcePath), StandardCharsets.UTF_8);
    assertThat(sanitizeString(message)).startsWith(sanitizeString(fileContent));
  }

  /**
   * sanitize EOL and tabs if git clone is badly configured
   */
  private static String sanitizeString(String s) {
    return s.replaceAll("\\r\\n|\\r|\\s+", "");
  }
}
