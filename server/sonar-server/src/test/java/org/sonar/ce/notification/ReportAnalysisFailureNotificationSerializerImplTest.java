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
import org.junit.Test;
import org.sonar.api.notifications.Notification;

import static java.lang.String.valueOf;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class ReportAnalysisFailureNotificationSerializerImplTest {
  private Random random = new Random();
  private ReportAnalysisFailureNotification.Project project = new ReportAnalysisFailureNotification.Project(
    randomAlphanumeric(2),
    randomAlphanumeric(3),
    randomAlphanumeric(4),
    randomAlphanumeric(5));
  private ReportAnalysisFailureNotification.Task task = new ReportAnalysisFailureNotification.Task(
    randomAlphanumeric(6),
    random.nextInt(99),
    random.nextInt(99));
  private String errorMessage = randomAlphanumeric(7);
  private ReportAnalysisFailureNotificationSerializerImpl underTest = new ReportAnalysisFailureNotificationSerializerImpl();

  @Test
  public void verify_toNotification() {

    Notification notification = underTest.toNotification(new ReportAnalysisFailureNotification(project, task, errorMessage));

    assertThat(notification.getFieldValue("project.uuid")).isEqualTo(project.getUuid());
    assertThat(notification.getFieldValue("project.name")).isEqualTo(project.getName());
    assertThat(notification.getFieldValue("project.key")).isEqualTo(project.getKey());
    assertThat(notification.getFieldValue("project.branchName")).isEqualTo(project.getBranchName());
    assertThat(notification.getFieldValue("task.uuid")).isEqualTo(task.getUuid());
    assertThat(notification.getFieldValue("task.createdAt")).isEqualTo(valueOf(task.getCreatedAt()));
    assertThat(notification.getFieldValue("task.failedAt")).isEqualTo(valueOf(task.getFailedAt()));
    assertThat(notification.getFieldValue("error.message")).isEqualTo(errorMessage);
  }

  @Test
  public void verify_fromNotification() {
    Notification notification = new Notification(randomAlphanumeric(1))
      .setFieldValue("project.uuid", project.getUuid())
      .setFieldValue("project.name", project.getName())
      .setFieldValue("project.key", project.getKey())
      .setFieldValue("project.branchName", project.getBranchName())
      .setFieldValue("task.uuid", task.getUuid())
      .setFieldValue("task.createdAt", valueOf(task.getCreatedAt()))
      .setFieldValue("task.failedAt", valueOf(task.getFailedAt()))
      .setFieldValue("error.message", errorMessage);

    ReportAnalysisFailureNotification reportAnalysisFailureNotification = underTest.fromNotification(notification);

    assertThat(reportAnalysisFailureNotification.getProject().getUuid()).isEqualTo(project.getUuid());
    assertThat(reportAnalysisFailureNotification.getProject().getKey()).isEqualTo(project.getKey());
    assertThat(reportAnalysisFailureNotification.getProject().getName()).isEqualTo(project.getName());
    assertThat(reportAnalysisFailureNotification.getProject().getBranchName()).isEqualTo(project.getBranchName());
    assertThat(reportAnalysisFailureNotification.getTask().getUuid()).isEqualTo(task.getUuid());
    assertThat(reportAnalysisFailureNotification.getTask().getCreatedAt()).isEqualTo(task.getCreatedAt());
    assertThat(reportAnalysisFailureNotification.getTask().getFailedAt()).isEqualTo(task.getFailedAt());
    assertThat(reportAnalysisFailureNotification.getErrorMessage()).isEqualTo(errorMessage);
  }
}
