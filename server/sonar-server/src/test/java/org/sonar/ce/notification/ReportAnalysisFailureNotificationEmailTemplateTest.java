/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.ce.notification;

import java.util.Random;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.utils.DateUtils;
import org.sonar.plugins.emailnotifications.api.EmailMessage;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportAnalysisFailureNotificationEmailTemplateTest {
  private String serverUrl = RandomStringUtils.randomAlphanumeric(12);
  private Notification notification = new Notification(ReportAnalysisFailureNotification.TYPE);
  private Random random = new Random();
  private ReportAnalysisFailureNotification.Project projectNoBranch = new ReportAnalysisFailureNotification.Project(
    randomAlphanumeric(2),
    randomAlphanumeric(3),
    randomAlphanumeric(4),
    null);
  private ReportAnalysisFailureNotification.Project projectWithBranch = new ReportAnalysisFailureNotification.Project(
    randomAlphanumeric(6),
    randomAlphanumeric(7),
    randomAlphanumeric(8),
    randomAlphanumeric(9));
  private ReportAnalysisFailureNotification.Task task = new ReportAnalysisFailureNotification.Task(
    randomAlphanumeric(10),
    random.nextInt(99),
    random.nextInt(99));
  private String errorMessage = randomAlphanumeric(11);

  private ReportAnalysisFailureNotificationSerializer serializer = mock(ReportAnalysisFailureNotificationSerializer.class);
  private EmailSettings emailSettings = mock(EmailSettings.class);
  private ReportAnalysisFailureNotificationEmailTemplate underTest = new ReportAnalysisFailureNotificationEmailTemplate(serializer, emailSettings);

  @Before
  public void setUp() throws Exception {
    when(emailSettings.getServerBaseURL()).thenReturn(serverUrl);
  }

  @Test
  public void returns_null_if_notification_type_is_not_ReportAnalysisFailureNotification_TYPE() {
    Notification notification = new Notification(RandomStringUtils.randomAlphanumeric(5));

    EmailMessage emailMessage = underTest.format(notification);

    assertThat(emailMessage).isNull();
  }

  @Test
  public void format_returns_email_with_subject_without_branch() {
    when(serializer.fromNotification(notification)).thenReturn(new ReportAnalysisFailureNotification(
      projectNoBranch, task, errorMessage));

    EmailMessage emailMessage = underTest.format(notification);

    assertThat(emailMessage.getSubject()).isEqualTo(projectNoBranch.getName() + ": Background task in failure");
  }

  @Test
  public void format_returns_email_with_subject_with_branch() {
    when(serializer.fromNotification(notification)).thenReturn(new ReportAnalysisFailureNotification(
      projectWithBranch, task, errorMessage));

    EmailMessage emailMessage = underTest.format(notification);

    assertThat(emailMessage.getSubject()).isEqualTo(projectWithBranch.getName() + " (" + projectWithBranch.getBranchName() + "): Background task in failure");
  }

  @Test
  public void format_returns_email_with_message_without_branch() {
    when(serializer.fromNotification(notification)).thenReturn(new ReportAnalysisFailureNotification(
      projectNoBranch, task, errorMessage));

    EmailMessage emailMessage = underTest.format(notification);

    assertThat(emailMessage.getMessage())
      .contains("Project:\t" + projectNoBranch.getName() + "\n");
  }

  @Test
  public void format_returns_email_with_message_with_branch() {
    when(serializer.fromNotification(notification)).thenReturn(new ReportAnalysisFailureNotification(
      projectWithBranch, task, errorMessage));

    EmailMessage emailMessage = underTest.format(notification);

    assertThat(emailMessage.getMessage())
      .contains("Project:\t" + projectWithBranch.getName() + " (" + projectWithBranch.getBranchName() + ")\n");
  }

  @Test
  public void format_returns_email_with_message_containing_all_information() {
    when(serializer.fromNotification(notification)).thenReturn(new ReportAnalysisFailureNotification(
      projectNoBranch, task, errorMessage));

    EmailMessage emailMessage = underTest.format(notification);

    assertThat(emailMessage.getMessage())
      .isEqualTo("Project:\t" + projectNoBranch.getName() + "\n" +
        "Background task:\t" + task.getUuid() + "\n" +
        "Submission time:\t" + DateUtils.formatDateTime(task.getCreatedAt()) + "\n" +
        "Failure time:\t" + DateUtils.formatDateTime(task.getFailedAt()) + "\n" +
        "\n" +
        "Error message:\t" + errorMessage + "\n" +
        "\n" +
        "More details at: " + serverUrl + "/project/background_tasks?id=" + projectNoBranch.getKey());
  }

  @Test
  public void format_returns_email_with_message_with_error_message_when_null() {
    when(serializer.fromNotification(notification)).thenReturn(new ReportAnalysisFailureNotification(
      projectNoBranch, task, null));

    EmailMessage emailMessage = underTest.format(notification);

    assertThat(emailMessage.getMessage())
      .doesNotContain("Error message:\t");
  }
}
