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
package org.sonar.server.qualitygate.notification;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.server.issue.notification.EmailMessage;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QGChangeEmailTemplateTest {

  private QGChangeEmailTemplate template;

  @Before
  public void setUp() {
    EmailSettings configuration = mock(EmailSettings.class);
    when(configuration.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
    template = new QGChangeEmailTemplate(configuration);
  }

  @Test
  public void shouldNotFormatIfNotCorrectNotification() {
    Notification notification = new Notification("other-notif");
    EmailMessage message = template.format(notification);
    assertThat(message, nullValue());
  }

  @Test
  public void shouldFormatAlertWithSeveralMessages() {
    Notification notification = createNotification("Red (was Green)", "violations > 4, coverage < 75%", "ERROR", "false");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("alerts/45"));
    assertThat(message.getSubject(), is("Quality gate status changed on \"Foo\""));
    assertThat(message.getMessage(), is("" +
      "Project: Foo\n" +
      "Version: V1-SNAP\n" +
      "Quality gate status: Red (was Green)\n" +
      "\n" +
      "Quality gate thresholds:\n" +
      "  - violations > 4\n" +
      "  - coverage < 75%\n" +
      "\n" +
      "More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo"));
  }

  @Test
  public void shouldFormatAlertWithSeveralMessagesOnBranch() {
    Notification notification = createNotification("Red (was Green)", "violations > 4, coverage < 75%", "ERROR", "false")
        .setFieldValue("branch", "feature");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("alerts/45"));
    assertThat(message.getSubject(), is("Quality gate status changed on \"Foo (feature)\""));
    assertThat(message.getMessage(), is("" +
      "Project: Foo\n" +
      "Branch: feature\n" +
      "Version: V1-SNAP\n" +
      "Quality gate status: Red (was Green)\n" +
      "\n" +
      "Quality gate thresholds:\n" +
      "  - violations > 4\n" +
      "  - coverage < 75%\n" +
      "\n" +
      "More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo&branch=feature"));
  }

  @Test
  public void shouldFormatNewAlertWithSeveralMessages() {
    Notification notification = createNotification("Red (was Green)", "violations > 4, coverage < 75%", "ERROR", "true");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("alerts/45"));
    assertThat(message.getSubject(), is("New quality gate threshold reached on \"Foo\""));
    assertThat(message.getMessage(), is("" +
      "Project: Foo\n" +
      "Version: V1-SNAP\n" +
      "Quality gate status: Red (was Green)\n" +
      "\n" +
      "New quality gate thresholds:\n" +
      "  - violations > 4\n" +
      "  - coverage < 75%\n" +
      "\n" +
      "More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo"));
  }

  @Test
  public void shouldFormatNewAlertWithOneMessage() {
    Notification notification = createNotification("Red (was Green)", "violations > 4", "ERROR", "true");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("alerts/45"));
    assertThat(message.getSubject(), is("New quality gate threshold reached on \"Foo\""));
    assertThat(message.getMessage(), is("" +
      "Project: Foo\n" +
      "Version: V1-SNAP\n" +
      "Quality gate status: Red (was Green)\n" +
      "\n" +
      "New quality gate threshold: violations > 4\n" +
      "\n" +
      "More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo"));
  }

  @Test
  public void shouldFormatNewAlertWithoutVersion() {
    Notification notification = createNotification("Red (was Green)", "violations > 4", "ERROR", "true")
        .setFieldValue("projectVersion", null);

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("alerts/45"));
    assertThat(message.getSubject(), is("New quality gate threshold reached on \"Foo\""));
    assertThat(message.getMessage(), is("" +
      "Project: Foo\n" +
      "Quality gate status: Red (was Green)\n" +
      "\n" +
      "New quality gate threshold: violations > 4\n" +
      "\n" +
      "More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo"));
  }

  @Test
  public void shouldFormatNewAlertWithOneMessageOnBranch() {
    Notification notification = createNotification("Red (was Green)", "violations > 4", "ERROR", "true")
      .setFieldValue("branch", "feature");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("alerts/45"));
    assertThat(message.getSubject(), is("New quality gate threshold reached on \"Foo (feature)\""));
    assertThat(message.getMessage(), is("" +
      "Project: Foo\n" +
      "Branch: feature\n" +
      "Version: V1-SNAP\n" +
      "Quality gate status: Red (was Green)\n" +
      "\n" +
      "New quality gate threshold: violations > 4\n" +
      "\n" +
      "More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo&branch=feature"));
  }

  @Test
  public void shouldFormatBackToGreenMessage() {
    Notification notification = createNotification("Green (was Red)", "", "OK", "false");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("alerts/45"));
    assertThat(message.getSubject(), is("\"Foo\" is back to green"));
    assertThat(message.getMessage(), is("" +
      "Project: Foo\n" +
      "Version: V1-SNAP\n" +
      "Quality gate status: Green (was Red)\n" +
      "\n" +
      "\n" +
      "More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo"));
  }

  @Test
  public void shouldFormatBackToGreenMessageOnBranch() {
    Notification notification = createNotification("Green (was Red)", "", "OK", "false")
        .setFieldValue("branch", "feature");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("alerts/45"));
    assertThat(message.getSubject(), is("\"Foo (feature)\" is back to green"));
    assertThat(message.getMessage(), is("" +
      "Project: Foo\n" +
      "Branch: feature\n" +
      "Version: V1-SNAP\n" +
      "Quality gate status: Green (was Red)\n" +
      "\n" +
      "\n" +
      "More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo&branch=feature"));
  }

  private Notification createNotification(String alertName, String alertText, String alertLevel, String isNewAlert) {
    Notification notification = new Notification("alerts")
        .setFieldValue("projectName", "Foo")
        .setFieldValue("projectKey", "org.sonar.foo:foo")
        .setFieldValue("projectId", "45")
        .setFieldValue("projectVersion", "V1-SNAP")
        .setFieldValue("alertName", alertName)
        .setFieldValue("alertText", alertText)
        .setFieldValue("alertLevel", alertLevel)
        .setFieldValue("isNewAlert", isNewAlert);
    return notification;
  }

}
