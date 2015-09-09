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
  private Status status;
  private String taskType;
  private boolean isLast;
  private String isLastKey;
  private String submitterLogin;
  private long submittedAt;
  private Long startedAt;
  private Long finishedAt;
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

  public void setUuid(String s) {
    checkArgument(s.length() <= 40, "Value is too long for column CE_ACTIVITY.UUID: %s", s);
    this.uuid = s;
  }

  public String getTaskType() {
    return taskType;
  }

  public void setTaskType(String s) {
    this.taskType = s;
  }

  @CheckForNull
  public String getComponentUuid() {
    return componentUuid;
  }

  public void setComponentUuid(@Nullable String s) {
    checkArgument(s == null || s.length() <= 40, "Value is too long for column CE_ACTIVITY.COMPONENT_UUID: %s", s);
    this.componentUuid = s;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status s) {
    this.status = s;
  }

  public boolean getIsLast() {
    return isLast;
  }

  void setIsLast(boolean b) {
    this.isLast = b;
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

  public void setSubmittedAt(long submittedAt) {
    this.submittedAt = submittedAt;
  }

  @CheckForNull
  public Long getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(@Nullable Long l) {
    this.startedAt = l;
  }

  @CheckForNull
  public Long getFinishedAt() {
    return finishedAt;
  }

  public void setFinishedAt(@Nullable Long l) {
    this.finishedAt = l;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long l) {
    this.createdAt = l;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(long l) {
    this.updatedAt = l;
  }

  @CheckForNull
  public Long getExecutionTimeMs() {
    return executionTimeMs;
  }

  public void setExecutionTimeMs(@Nullable Long l) {
    checkArgument(l == null || l >= 0, "Execution time must be positive: %s", l);
    this.executionTimeMs = l;
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
      .add("finishedAt", finishedAt)
      .add("createdAt", createdAt)
      .add("updatedAt", updatedAt)
      .add("executionTimeMs", executionTimeMs)
      .toString();
  }
}
