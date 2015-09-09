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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

public class CeQueueDto {

  public enum Status {
    PENDING, IN_PROGRESS
  }

  private String uuid;
  private String taskType;
  private String componentUuid;
  private Status status;
  private String submitterLogin;
  private Long startedAt;
  private long createdAt;
  private long updatedAt;

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String s) {
    checkArgument(s.length() <= 40, "Value is too long for column CE_QUEUE.UUID: %s", s);
    this.uuid = s;
  }

  @CheckForNull
  public String getComponentUuid() {
    return componentUuid;
  }

  public void setComponentUuid(@Nullable String s) {
    checkArgument(s == null || s.length() <= 40, "Value is too long for column CE_QUEUE.COMPONENT_UUID: %s", s);
    this.componentUuid = s;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status s) {
    this.status = s;
  }

  public String getTaskType() {
    return taskType;
  }

  public void setTaskType(String s) {
    checkArgument(s.length() <= 15, "Value is too long for column CE_QUEUE.TASK_TYPE: %s", s);
    this.taskType = s;
  }

  @CheckForNull
  public String getSubmitterLogin() {
    return submitterLogin;
  }

  public void setSubmitterLogin(@Nullable String s) {
    checkArgument(s == null || s.length() <= 255, "Value is too long for column CE_QUEUE.SUBMITTER_LOGIN: %s", s);
    this.submitterLogin = s;
  }

  @CheckForNull
  public Long getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(@Nullable Long l) {
    this.startedAt = l;
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

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("uuid", uuid)
      .add("taskType", taskType)
      .add("componentUuid", componentUuid)
      .add("status", status)
      .add("submitterLogin", submitterLogin)
      .add("startedAt", startedAt)
      .add("createdAt", createdAt)
      .add("updatedAt", updatedAt)
      .toString();
  }

  @Override
  public boolean equals(Object o) {
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
