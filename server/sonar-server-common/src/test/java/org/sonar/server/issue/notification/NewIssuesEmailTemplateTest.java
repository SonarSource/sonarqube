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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.platform.Server;
import org.sonar.server.l18n.I18nRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.ASSIGNEE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.COMPONENT;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.EFFORT;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.RULE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.RULE_TYPE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.TAG;

public class NewIssuesEmailTemplateTest {

  @Rule
  public I18nRule i18n = new I18nRule()
    .put("issue.type.BUG", "Bug")
    .put("issue.type.CODE_SMELL", "Code Smell")
    .put("issue.type.VULNERABILITY", "Vulnerability");
  private MapSettings settings = new MapSettings();

  private Server server = mock(Server.class);
  private NewIssuesEmailTemplate template = new NewIssuesEmailTemplate(new EmailSettings(settings.asConfig(), server), i18n);

  @Before
  public void setUp() throws Exception {
    when(server.getPublicRootUrl()).thenReturn("http://nemo.sonarsource.org");
  }

  @Test
  public void no_format_is_not_the_correct_notification() {
    Notification notification = new Notification("my-new-issues");
    EmailMessage message = template.format(notification);
    assertThat(message).isNull();
  }

  @Test
  public void message_id() {
    Notification notification = newNotification(32);

    EmailMessage message = template.format(notification);

    assertThat(message.getMessageId()).isEqualTo("new-issues/org.apache:struts");
  }

  @Test
  public void subject() {
    Notification notification = newNotification(32);

    EmailMessage message = template.format(notification);

    assertThat(message.getSubject()).isEqualTo("Struts: 32 new issues (new debt: 1d3h)");
  }

  @Test
  public void subject_on_branch() {
    Notification notification = newNotification(32)
      .setFieldValue("branch", "feature1");

    EmailMessage message = template.format(notification);

    assertThat(message.getSubject()).isEqualTo("Struts (feature1): 32 new issues (new debt: 1d3h)");
  }

  @Test
  public void format_email_with_all_fields_filled() {
    Notification notification = newNotification(32)
      .setFieldValue("projectVersion", "42.1.1");
    addAssignees(notification);
    addRules(notification);
    addTags(notification);
    addComponents(notification);

    EmailMessage message = template.format(notification);

    // TODO datetime to be completed when test is isolated from JVM timezone
    assertThat(message.getMessage())
      .startsWith("Project: Struts\n" +
        "Version: 42.1.1\n" +
        "\n" +
        "32 new issues (new debt: 1d3h)\n" +
        "\n" +
        "    Type\n" +
        "        Bug: 1    Vulnerability: 10    Code Smell: 3\n" +
        "\n" +
        "    Assignees\n" +
        "        robin.williams: 5\n" +
        "        al.pacino: 7\n" +
        "\n" +
        "    Rules\n" +
        "        Rule the Universe (Clojure): 42\n" +
        "        Rule the World (Java): 5\n" +
        "\n" +
        "    Tags\n" +
        "        oscar: 3\n" +
        "        cesar: 10\n" +
        "\n" +
        "    Most impacted files\n" +
        "        /path/to/file: 3\n" +
        "        /path/to/directory: 7\n" +
        "\n" +
        "More details at: http://nemo.sonarsource.org/project/issues?id=org.apache%3Astruts&createdAt=2010-05-1");
  }

  @Test
  public void format_email_with_no_assignees_tags_nor_components_nor_version() {
    Notification notification = newNotification(32);

    EmailMessage message = template.format(notification);

    // TODO datetime to be completed when test is isolated from JVM timezone
    assertThat(message.getMessage())
      .startsWith("Project: Struts\n" +
        "\n" +
        "32 new issues (new debt: 1d3h)\n" +
        "\n" +
        "    Type\n" +
        "        Bug: 1    Vulnerability: 10    Code Smell: 3\n" +
        "\n" +
        "More details at: http://nemo.sonarsource.org/project/issues?id=org.apache%3Astruts&createdAt=2010-05-1");
  }

  @Test
  public void format_email_supports_single_issue() {
    Notification notification = newNotification(1);

    EmailMessage message = template.format(notification);

    assertThat(message.getSubject())
      .isEqualTo("Struts: 1 new issue (new debt: 1d3h)");
    assertThat(message.getMessage())
      .contains("1 new issue (new debt: 1d3h)\n");
  }

  @Test
  public void format_email_with_issue_on_branch() {
    Notification notification = newNotification(32)
      .setFieldValue("branch", "feature1");

    EmailMessage message = template.format(notification);

    // TODO datetime to be completed when test is isolated from JVM timezone
    assertThat(message.getMessage())
      .startsWith("Project: Struts\n" +
        "Branch: feature1\n" +
        "\n" +
        "32 new issues (new debt: 1d3h)\n" +
        "\n" +
        "    Type\n" +
        "        Bug: 1    Vulnerability: 10    Code Smell: 3\n" +
        "\n" +
        "More details at: http://nemo.sonarsource.org/project/issues?id=org.apache%3Astruts&branch=feature1&createdAt=2010-05-1");
  }

  @Test
  public void format_email_with_issue_on_branch_with_version() {
    Notification notification = newNotification(32)
      .setFieldValue("branch", "feature1")
      .setFieldValue("projectVersion", "42.1.1");

    EmailMessage message = template.format(notification);

    // TODO datetime to be completed when test is isolated from JVM timezone
    assertThat(message.getMessage())
      .startsWith("Project: Struts\n" +
        "Branch: feature1\n" +
        "Version: 42.1.1\n" +
        "\n" +
        "32 new issues (new debt: 1d3h)\n" +
        "\n" +
        "    Type\n" +
        "        Bug: 1    Vulnerability: 10    Code Smell: 3\n" +
        "\n" +
        "More details at: http://nemo.sonarsource.org/project/issues?id=org.apache%3Astruts&branch=feature1&createdAt=2010-05-1");
  }

  @Test
  public void do_not_add_footer_when_properties_missing() {
    Notification notification = new Notification(NewIssuesNotification.TYPE)
      .setFieldValue(RULE_TYPE + ".count", "32")
      .setFieldValue("projectName", "Struts");

    EmailMessage message = template.format(notification);

    assertThat(message.getMessage()).doesNotContain("See it");
  }

  private Notification newNotification(int count) {
    return new Notification(NewIssuesNotification.TYPE)
      .setFieldValue("projectName", "Struts")
      .setFieldValue("projectKey", "org.apache:struts")
      .setFieldValue("projectDate", "2010-05-18T14:50:45+0000")
      .setFieldValue(EFFORT + ".count", "1d3h")
      .setFieldValue(RULE_TYPE + ".count", String.valueOf(count))
      .setFieldValue(RULE_TYPE + ".BUG.count", "1")
      .setFieldValue(RULE_TYPE + ".CODE_SMELL.count", "3")
      .setFieldValue(RULE_TYPE + ".VULNERABILITY.count", "10");
  }

  private void addAssignees(Notification notification) {
    notification
      .setFieldValue(ASSIGNEE + ".1.label", "robin.williams")
      .setFieldValue(ASSIGNEE + ".1.count", "5")
      .setFieldValue(ASSIGNEE + ".2.label", "al.pacino")
      .setFieldValue(ASSIGNEE + ".2.count", "7");
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
