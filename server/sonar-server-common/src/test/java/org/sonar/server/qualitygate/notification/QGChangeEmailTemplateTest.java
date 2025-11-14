/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.notifications.Notification;
import org.sonar.api.platform.Server;
import org.sonar.server.issue.notification.EmailMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class QGChangeEmailTemplateTest {

  private final Server server = mock();
  private final QGChangeEmailTemplate underTest = new QGChangeEmailTemplate(server);

  @Before
  public void setUp() {
    when(server.getPublicRootUrl()).thenReturn("http://nemo.sonarsource.org");
  }

  @Test
  public void shouldNotFormatIfNotCorrectNotification() {
    Notification notification = new Notification("other-notif");
    EmailMessage message = underTest.format(notification);
    assertThat(message).isNull();
  }

  @Test
  public void shouldFormatAlertWithSeveralMessages() {
    Notification notification = createNotification("Failed", "violations > 4, coverage < 75%", "ERROR", "false");

    EmailMessage message = underTest.format(notification);
    assertThat(message.getMessageId()).isEqualTo("alerts/45");
    assertThat(message.getSubject()).isEqualTo("Quality gate status changed on \"Foo\"");
    assertThat(message.getMessage()).isEqualTo("""
      Project: Foo
      Version: V1-SNAP
      Quality gate status: Failed

      Quality gate thresholds:
        - violations > 4
        - coverage < 75%

      More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo""");
  }

  @Test
  public void shouldFormatAlertWithSeveralMessagesOnBranch() {
    Notification notification = createNotification("Failed", "violations > 4, coverage < 75%", "ERROR", "false")
      .setFieldValue(QGChangeNotification.FIELD_BRANCH, "feature");

    EmailMessage message = underTest.format(notification);
    assertThat(message.getMessageId()).isEqualTo("alerts/45");
    assertThat(message.getSubject()).isEqualTo("Quality gate status changed on \"Foo (feature)\"");
    assertThat(message.getMessage()).isEqualTo("""
      Project: Foo
      Branch: feature
      Version: V1-SNAP
      Quality gate status: Failed

      Quality gate thresholds:
        - violations > 4
        - coverage < 75%

      More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo&branch=feature""");
  }

  @Test
  public void shouldFormatNewAlertWithSeveralMessages() {
    Notification notification = createNotification("Failed", "violations > 4, coverage < 75%", "ERROR", "true");

    EmailMessage message = underTest.format(notification);
    assertThat(message.getMessageId()).isEqualTo("alerts/45");
    assertThat(message.getSubject()).isEqualTo("New quality gate threshold reached on \"Foo\"");
    assertThat(message.getMessage()).isEqualTo("""
      Project: Foo
      Version: V1-SNAP
      Quality gate status: Failed

      New quality gate thresholds:
        - violations > 4
        - coverage < 75%

      More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo""");
  }

  @Test
  public void shouldFormatNewAlertWithOneMessage() {
    Notification notification = createNotification("Failed", "violations > 4", "ERROR", "true");

    EmailMessage message = underTest.format(notification);
    assertThat(message.getMessageId()).isEqualTo("alerts/45");
    assertThat(message.getSubject()).isEqualTo("New quality gate threshold reached on \"Foo\"");
    assertThat(message.getMessage()).isEqualTo("""
      Project: Foo
      Version: V1-SNAP
      Quality gate status: Failed

      New quality gate threshold: violations > 4

      More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo""");
  }

  @Test
  public void shouldFormatNewAlertWithoutVersion() {
    Notification notification = createNotification("Failed", "violations > 4", "ERROR", "true")
      .setFieldValue(QGChangeNotification.FIELD_PROJECT_VERSION, null);

    EmailMessage message = underTest.format(notification);
    assertThat(message.getMessageId()).isEqualTo("alerts/45");
    assertThat(message.getSubject()).isEqualTo("New quality gate threshold reached on \"Foo\"");
    assertThat(message.getMessage()).isEqualTo("""
      Project: Foo
      Quality gate status: Failed
      
      New quality gate threshold: violations > 4
      
      More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo""");
  }

  @Test
  public void shouldFormatBackToGreenMessage() {
    Notification notification = createNotification("Passed", "", "OK", "false");

    EmailMessage message = underTest.format(notification);
    assertThat(message.getMessageId()).isEqualTo("alerts/45");
    assertThat(message.getSubject()).isEqualTo("\"Foo\" is back to green");
    assertThat(message.getMessage()).isEqualTo("""
      Project: Foo
      Version: V1-SNAP
      Quality gate status: Passed


      More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo""");
  }

  @Test
  public void shouldFormatNewAlertWithOneMessageOnBranch() {
    Notification notification = createNotification("Failed", "violations > 4", "ERROR", "true")
      .setFieldValue(QGChangeNotification.FIELD_BRANCH, "feature");

    EmailMessage message = underTest.format(notification);
    assertThat(message.getMessageId()).isEqualTo("alerts/45");
    assertThat(message.getSubject()).isEqualTo("New quality gate threshold reached on \"Foo (feature)\"");
    assertThat(message.getMessage()).isEqualTo("""
      Project: Foo
      Branch: feature
      Version: V1-SNAP
      Quality gate status: Failed
      
      New quality gate threshold: violations > 4
      
      More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo&branch=feature""");
  }

  @Test
  public void shouldFormatBackToGreenMessageOnBranch() {
    Notification notification = createNotification("Passed", "", "OK", "false")
      .setFieldValue(QGChangeNotification.FIELD_BRANCH, "feature");

    EmailMessage message = underTest.format(notification);
    assertThat(message.getMessageId()).isEqualTo("alerts/45");
    assertThat(message.getSubject()).isEqualTo("\"Foo (feature)\" is back to green");
    assertThat(message.getMessage()).isEqualTo("""
      Project: Foo
      Branch: feature
      Version: V1-SNAP
      Quality gate status: Passed
      
      
      More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo&branch=feature""");
  }

  @DataProvider
  public static Object[][] alertTextAndFormattedText() {
    return new Object[][] {
      {"violations > 0", "violations > 0"},
      {"violations > 1", "violations > 1"},
      {"violations > 4", "violations > 4"},
      {"violations > 5", "violations > 5"},
      {"violations > 6", "violations > 6"},
      {"violations > 10", "violations > 10"},
      {"Code Coverage < 0%", "Code Coverage < 0%"},
      {"Code Coverage < 1%", "Code Coverage < 1%"},
      {"Code Coverage < 50%", "Code Coverage < 50%"},
      {"Code Coverage < 100%", "Code Coverage < 100%"},
      {"Custom metric with big number > 100000000000", "Custom metric with big number > 100000000000"},
      {"Custom metric with negative number > -1", "Custom metric with negative number > -1"},
      {"custom metric condition not met", "custom metric condition not met"},

      {"Security Review Rating > 1", "Security Review Rating worse than A"},
      {"Security Review Rating on New Code > 4", "Security Review Rating on New Code worse than D"},
      {"Security Rating > 1", "Security Rating worse than A"},
      {"Maintainability Rating > 3", "Maintainability Rating worse than C"},
      {"Reliability Rating > 4", "Reliability Rating worse than D" }
    };
  }

  @UseDataProvider("alertTextAndFormattedText")
  @Test
  public void shouldFormatNewAlertWithThresholdProperlyFormatted(String alertText, String expectedFormattedAlertText) {
    Notification notification = createNotification("Failed", alertText, "ERROR", "true");

    EmailMessage message = underTest.format(notification);
    assertThat(message.getMessageId()).isEqualTo("alerts/45");
    assertThat(message.getSubject()).isEqualTo("New quality gate threshold reached on \"Foo\"");
    assertThat(message.getMessage()).isEqualTo("""
      Project: Foo
      Version: V1-SNAP
      Quality gate status: Failed
      
      New quality gate threshold: %s
      
      More details at: http://nemo.sonarsource.org/dashboard?id=org.sonar.foo:foo""".formatted(expectedFormattedAlertText));
  }


  private Notification createNotification(String alertName, String alertText, String alertLevel, String isNewAlert) {
    return new Notification("alerts")
      .setFieldValue(QGChangeNotification.FIELD_PROJECT_NAME, "Foo")
      .setFieldValue(QGChangeNotification.FIELD_PROJECT_KEY, "org.sonar.foo:foo")
      .setFieldValue(QGChangeNotification.FIELD_PROJECT_ID, "45")
      .setFieldValue(QGChangeNotification.FIELD_PROJECT_VERSION, "V1-SNAP")
      .setFieldValue(QGChangeNotification.FIELD_ALERT_NAME, alertName)
      .setFieldValue(QGChangeNotification.FIELD_ALERT_TEXT, alertText)
      .setFieldValue(QGChangeNotification.FIELD_ALERT_LEVEL, alertLevel)
      .setFieldValue(QGChangeNotification.FIELD_IS_NEW_ALERT, isNewAlert)
      .setFieldValue(QGChangeNotification.FIELD_RATING_METRICS, "Maintainability Rating,Reliability Rating on New Code," +
        "Maintainability Rating on New Code,Reliability Rating," +
        "Security Rating on New Code,Security Review Rating," +
        "Security Review Rating on New Code,Security Rating");
  }

}
