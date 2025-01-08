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
package org.sonar.ce.notification;

import java.time.Duration;
import javax.annotation.Nullable;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskResult;
import org.sonar.ce.task.projectanalysis.notification.ReportAnalysisFailureNotification;
import org.sonar.ce.task.projectanalysis.notification.ReportAnalysisFailureNotificationBuilder;
import org.sonar.ce.task.projectanalysis.notification.ReportAnalysisFailureNotificationSerializer;
import org.sonar.ce.taskprocessor.CeWorker;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.notification.NotificationService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Collections.singleton;

public class ReportAnalysisFailureNotificationExecutionListener implements CeWorker.ExecutionListener {
  private final NotificationService notificationService;
  private final DbClient dbClient;
  private final ReportAnalysisFailureNotificationSerializer taskFailureNotificationSerializer;
  private final System2 system2;

  public ReportAnalysisFailureNotificationExecutionListener(NotificationService notificationService, DbClient dbClient,
    ReportAnalysisFailureNotificationSerializer taskFailureNotificationSerializer, System2 system2) {
    this.notificationService = notificationService;
    this.dbClient = dbClient;
    this.taskFailureNotificationSerializer = taskFailureNotificationSerializer;
    this.system2 = system2;
  }

  @Override
  public void onStart(CeTask ceTask) {
    // nothing to do
  }

  @Override
  public void onEnd(CeTask ceTask, CeActivityDto.Status status, Duration duration, @Nullable CeTaskResult taskResult, @Nullable Throwable error) {
    if (status == CeActivityDto.Status.SUCCESS) {
      return;
    }
    String branchUuid = ceTask.getComponent().map(CeTask.Component::getUuid).orElse(null);
    if (!CeTaskTypes.REPORT.equals(ceTask.getType()) || branchUuid == null) {
      return;
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      BranchDto branchDto = dbClient.branchDao().selectByUuid(dbSession, branchUuid)
        .orElseThrow(() -> new IllegalStateException("Could not find a branch with uuid %s".formatted(branchUuid)));
      if (notificationService.hasProjectSubscribersForTypes(branchDto.getProjectUuid(), singleton(ReportAnalysisFailureNotification.class))) {
        ProjectDto projectDto = dbClient.projectDao().selectByUuid(dbSession, branchDto.getProjectUuid())
          .orElseThrow(() -> new IllegalStateException("Could not find project uuid %s".formatted(branchDto.getProjectUuid())));

        checkQualifier(projectDto);
        CeActivityDto ceActivityDto = dbClient.ceActivityDao().selectByUuid(dbSession, ceTask.getUuid())
          .orElseThrow(() -> new RowNotFoundException(format("CeActivity with uuid '%s' not found", ceTask.getUuid())));
        ReportAnalysisFailureNotificationBuilder taskFailureNotification = buildNotification(ceActivityDto, projectDto, branchDto, error);
        ReportAnalysisFailureNotification notification = taskFailureNotificationSerializer.toNotification(taskFailureNotification);
        notificationService.deliverEmails(singleton(notification));

        // compatibility with old API
        notificationService.deliver(notification);
      }
    }
  }

  /**
   * @throws IllegalArgumentException if specified {@link ComponentDto} is not a project.
   */
  private static void checkQualifier(ProjectDto projectDto) {
    String qualifier = projectDto.getQualifier();
    checkArgument(qualifier.equals(ComponentQualifiers.PROJECT), "Component %s must be a project (qualifier=%s)", projectDto.getUuid(), qualifier);
  }

  private ReportAnalysisFailureNotificationBuilder buildNotification(CeActivityDto ceActivityDto, ProjectDto projectDto, BranchDto branchDto,
    @Nullable Throwable error) {
    String projectBranch = branchDto.isMain() ? null : branchDto.getBranchKey();
    Long executedAt = ceActivityDto.getExecutedAt();
    return new ReportAnalysisFailureNotificationBuilder(
      new ReportAnalysisFailureNotificationBuilder.Project(
        projectDto.getUuid(),
        projectDto.getKey(),
        projectDto.getName(),
        projectBranch),
      new ReportAnalysisFailureNotificationBuilder.Task(
        ceActivityDto.getUuid(),
        ceActivityDto.getSubmittedAt(),
        executedAt == null ? system2.now() : executedAt),
      error == null ? null : error.getMessage());
  }
}
