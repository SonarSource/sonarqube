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

import org.sonar.api.notifications.Notification;

import static java.lang.String.valueOf;

public class ReportAnalysisFailureNotificationSerializerImpl implements ReportAnalysisFailureNotificationSerializer {
  private static final String FIELD_PROJECT_UUID = "project.uuid";
  private static final String FIELD_PROJECT_KEY = "project.key";
  private static final String FIELD_PROJECT_NAME = "project.name";
  private static final String FIELD_PROJECT_BRANCH = "project.branchName";
  private static final String FIELD_TASK_UUID = "task.uuid";
  private static final String FIELD_TASK_CREATED_AT = "task.createdAt";
  private static final String FIELD_TASK_FAILED_AT = "task.failedAt";
  private static final String FIELD_ERROR_MESSAGE = "error.message";

  @Override
  public Notification toNotification(ReportAnalysisFailureNotification reportAnalysisFailureNotification) {
    return new Notification(ReportAnalysisFailureNotification.TYPE)
      .setFieldValue(FIELD_PROJECT_UUID, reportAnalysisFailureNotification.getProject().getUuid())
      .setFieldValue(FIELD_PROJECT_KEY, reportAnalysisFailureNotification.getProject().getKey())
      .setFieldValue(FIELD_PROJECT_NAME, reportAnalysisFailureNotification.getProject().getName())
      .setFieldValue(FIELD_PROJECT_BRANCH, reportAnalysisFailureNotification.getProject().getBranchName())
      .setFieldValue(FIELD_TASK_UUID, reportAnalysisFailureNotification.getTask().getUuid())
      .setFieldValue(FIELD_TASK_CREATED_AT, valueOf(reportAnalysisFailureNotification.getTask().getCreatedAt()))
      .setFieldValue(FIELD_TASK_FAILED_AT, valueOf(reportAnalysisFailureNotification.getTask().getFailedAt()))
      .setFieldValue(FIELD_ERROR_MESSAGE, reportAnalysisFailureNotification.getErrorMessage());
  }

  @Override
  public ReportAnalysisFailureNotification fromNotification(Notification notification) {
    return new ReportAnalysisFailureNotification(
      new ReportAnalysisFailureNotification.Project(
        notification.getFieldValue(FIELD_PROJECT_UUID),
        notification.getFieldValue(FIELD_PROJECT_KEY),
        notification.getFieldValue(FIELD_PROJECT_NAME),
        notification.getFieldValue(FIELD_PROJECT_BRANCH)),
      new ReportAnalysisFailureNotification.Task(
        notification.getFieldValue(FIELD_TASK_UUID),
        Long.valueOf(notification.getFieldValue(FIELD_TASK_CREATED_AT)),
        Long.valueOf(notification.getFieldValue(FIELD_TASK_FAILED_AT))),
      notification.getFieldValue(FIELD_ERROR_MESSAGE));
  }
}
