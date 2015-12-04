/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.ce;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

public class CeActivityDto {

  public enum Status {
    SUCCESS, FAILED, CANCELED
  }

  private String uuid;
  private String componentUuid;
  private Long snapshotId;
  private Status status;
  private String taskType;
  private boolean isLast;
  private String isLastKey;
  private String submitterLogin;
  private long submittedAt;
  private Long startedAt;
  private Long executedAt;
  private long createdAt;
  private long updatedAt;
  private Long executionTimeMs;

  CeActivityDto() {
    // required for MyBatis
  }

  public CeActivityDto(CeQueueDto queueDto) {
    this.uuid = queueDto.getUuid();
    this.taskType = queueDto.getTaskType();
    this.componentUuid = queueDto.getComponentUuid();
    this.isLastKey = format("%s%s", taskType, Strings.nullToEmpty(componentUuid));
    this.submitterLogin = queueDto.getSubmitterLogin();
    this.submittedAt = queueDto.getCreatedAt();
    this.startedAt = queueDto.getStartedAt();
  }

  public String getUuid() {
    return uuid;
  }

  public CeActivityDto setUuid(String s) {
    checkArgument(s.length() <= 40, "Value is too long for column CE_ACTIVITY.UUID: %s", s);
    this.uuid = s;
    return this;
  }

  public String getTaskType() {
    return taskType;
  }

  public CeActivityDto setTaskType(String s) {
    this.taskType = s;
    return this;
  }

  @CheckForNull
  public String getComponentUuid() {
    return componentUuid;
  }

  public CeActivityDto setComponentUuid(@Nullable String s) {
    checkArgument(s == null || s.length() <= 40, "Value is too long for column CE_ACTIVITY.COMPONENT_UUID: %s", s);
    this.componentUuid = s;
    return this;
  }

  public Status getStatus() {
    return status;
  }

  public CeActivityDto setStatus(Status s) {
    this.status = s;
    return this;
  }

  public boolean getIsLast() {
    return isLast;
  }

  CeActivityDto setIsLast(boolean b) {
    this.isLast = b;
    return this;
  }

  public String getIsLastKey() {
    return isLastKey;
  }

  @CheckForNull
  public String getSubmitterLogin() {
    return submitterLogin;
  }

  public long getSubmittedAt() {
    return submittedAt;
  }

  public CeActivityDto setSubmittedAt(long submittedAt) {
    this.submittedAt = submittedAt;
    return this;
  }

  @CheckForNull
  public Long getStartedAt() {
    return startedAt;
  }

  public CeActivityDto setStartedAt(@Nullable Long l) {
    this.startedAt = l;
    return this;
  }

  @CheckForNull
  public Long getExecutedAt() {
    return executedAt;
  }

  public CeActivityDto setExecutedAt(@Nullable Long l) {
    this.executedAt = l;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public CeActivityDto setCreatedAt(long l) {
    this.createdAt = l;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public CeActivityDto setUpdatedAt(long l) {
    this.updatedAt = l;
    return this;
  }

  @CheckForNull
  public Long getExecutionTimeMs() {
    return executionTimeMs;
  }

  public CeActivityDto setExecutionTimeMs(@Nullable Long l) {
    checkArgument(l == null || l >= 0, "Execution time must be positive: %s", l);
    this.executionTimeMs = l;
    return this;
  }

  @CheckForNull
  public Long getSnapshotId() {
    return snapshotId;
  }

  public CeActivityDto setSnapshotId(@Nullable Long snapshotId) {
    this.snapshotId = snapshotId;
    return this;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("uuid", uuid)
      .add("taskType", taskType)
      .add("componentUuid", componentUuid)
      .add("status", status)
      .add("isLast", isLast)
      .add("isLastKey", isLastKey)
      .add("submitterLogin", submitterLogin)
      .add("submittedAt", submittedAt)
      .add("startedAt", startedAt)
      .add("executedAt", executedAt)
      .add("createdAt", createdAt)
      .add("updatedAt", updatedAt)
      .add("executionTimeMs", executionTimeMs)
      .toString();
  }
}
