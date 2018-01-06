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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class ReportAnalysisFailureNotification {
  public static final String TYPE = "ce-report-task-failure";

  private final Project project;
  private final Task task;
  private final String errorMessage;

  public ReportAnalysisFailureNotification(Project project, Task task, @Nullable String errorMessage) {
    this.project = requireNonNull(project, "project can't be null");
    this.task = requireNonNull(task, "task can't be null");
    this.errorMessage = errorMessage;
  }

  public Project getProject() {
    return project;
  }

  public Task getTask() {
    return task;
  }

  @CheckForNull
  public String getErrorMessage() {
    return errorMessage;
  }

  public static final class Project {
    private final String uuid;
    private final String key;
    private final String name;
    private final String branchName;

    public Project(String uuid, String key, String name, @Nullable String branchName) {
      this.uuid = requireNonNull(uuid, "uuid can't be null");
      this.key = requireNonNull(key, "key can't be null");
      this.name = requireNonNull(name, "name can't be null");
      this.branchName = branchName;
    }

    public String getUuid() {
      return uuid;
    }

    public String getKey() {
      return key;
    }

    public String getName() {
      return name;
    }

    @CheckForNull
    public String getBranchName() {
      return branchName;
    }
  }

  public static final class Task {
    private final String uuid;
    private final long createdAt;
    private final long failedAt;

    public Task(String uuid, long createdAt, long failedAt) {
      this.uuid = requireNonNull(uuid, "uuid can't be null");
      this.createdAt = createdAt;
      this.failedAt = failedAt;
    }

    public String getUuid() {
      return uuid;
    }

    public long getCreatedAt() {
      return createdAt;
    }

    public long getFailedAt() {
      return failedAt;
    }
  }
}
