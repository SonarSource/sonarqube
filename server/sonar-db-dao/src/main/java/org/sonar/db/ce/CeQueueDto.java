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
package org.sonar.db.ce;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.component.ComponentDto;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class CeQueueDto {

  public enum Status {
    PENDING, IN_PROGRESS
  }

  private String uuid;
  private String taskType;
  /**
   * Can be {@code null} when task is not associated to any data in table PROJECTS, but must always be non {@code null}
   * at the same time as {@link #mainComponentUuid}.
   * <p>
   * The component uuid of a any component (project or not) is its own UUID.
   */
  private String componentUuid;
  /**
   * Can be {@code null} when task is not associated to any data in table PROJECTS, but must always be non {@code null}
   * at the same time as {@link #componentUuid}.
   * <p>
   * The main component uuid of the main branch of project is its own UUID. For other branches of a project, it is the
   * project UUID of the main branch of that project ({@link ComponentDto#getMainBranchProjectUuid()}).
   */
  private String mainComponentUuid;
  private Status status;
  private String submitterUuid;
  /**
   * UUID of the worker that is executing, or of the last worker that executed, the current task.
   */
  private String workerUuid;
  private Long startedAt;
  private long createdAt;
  private long updatedAt;

  public String getUuid() {
    return uuid;
  }

  public CeQueueDto setUuid(String s) {
    checkUuid(s, "UUID");
    this.uuid = s;
    return this;
  }

  /**
   * Helper methods which sets both {@link #componentUuid} and {@link #mainComponentUuid} from the specified
   * {@link ComponentDto}.
   */
  public CeQueueDto setComponent(@Nullable ComponentDto component) {
    if (component == null) {
      this.componentUuid = null;
      this.mainComponentUuid = null;
    } else {
      this.componentUuid = requireNonNull(component.uuid());
      this.mainComponentUuid = firstNonNull(component.getMainBranchProjectUuid(), component.uuid());
    }
    return this;
  }

  @CheckForNull
  public String getComponentUuid() {
    return componentUuid;
  }

  public CeQueueDto setComponentUuid(@Nullable String s) {
    checkUuid(s, "COMPONENT_UUID");
    this.componentUuid = s;
    return this;
  }

  @CheckForNull
  public String getMainComponentUuid() {
    return mainComponentUuid;
  }

  public CeQueueDto setMainComponentUuid(@Nullable String s) {
    checkUuid(s, "MAIN_COMPONENT_UUID");
    this.mainComponentUuid = s;
    return this;
  }

  private static void checkUuid(@Nullable String s, String columnName) {
    checkArgument(s == null || s.length() <= 40, "Value is too long for column CE_QUEUE.%s: %s", columnName, s);
  }

  public Status getStatus() {
    return status;
  }

  public CeQueueDto setStatus(Status s) {
    this.status = s;
    return this;
  }

  public String getTaskType() {
    return taskType;
  }

  public CeQueueDto setTaskType(String s) {
    checkArgument(s.length() <= 15, "Value of task type is too long: %s", s);
    this.taskType = s;
    return this;
  }

  @CheckForNull
  public String getSubmitterUuid() {
    return submitterUuid;
  }

  public CeQueueDto setSubmitterUuid(@Nullable String s) {
    checkArgument(s == null || s.length() <= 255, "Value of submitter uuid is too long: %s", s);
    this.submitterUuid = s;
    return this;
  }

  public String getWorkerUuid() {
    return workerUuid;
  }

  public CeQueueDto setWorkerUuid(@Nullable String workerUuid) {
    this.workerUuid = workerUuid;
    return this;
  }

  @CheckForNull
  public Long getStartedAt() {
    return startedAt;
  }

  public CeQueueDto setStartedAt(@Nullable Long l) {
    this.startedAt = l;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public CeQueueDto setCreatedAt(long l) {
    this.createdAt = l;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public CeQueueDto setUpdatedAt(long l) {
    this.updatedAt = l;
    return this;
  }

  @Override
  public String toString() {
    return "CeQueueDto{" +
      "uuid='" + uuid + '\'' +
      ", taskType='" + taskType + '\'' +
      ", componentUuid='" + componentUuid + '\'' +
      ", mainComponentUuid='" + mainComponentUuid + '\'' +
      ", status=" + status +
      ", submitterLogin='" + submitterUuid + '\'' +
      ", workerUuid='" + workerUuid + '\'' +
      ", startedAt=" + startedAt +
      ", createdAt=" + createdAt +
      ", updatedAt=" + updatedAt +
      '}';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CeQueueDto that = (CeQueueDto) o;
    return uuid.equals(that.uuid);

  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }
}
