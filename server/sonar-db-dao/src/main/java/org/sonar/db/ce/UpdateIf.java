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
package org.sonar.db.ce;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

final class UpdateIf {
  private UpdateIf() {
    // just a wrapping class, prevent instantiation
  }

  @Immutable
  public static class NewProperties {
    private final CeQueueDto.Status status;
    private final String workerUuid;
    private final int executionCount;
    private final Long startedAt;
    private final long updatedAt;

    NewProperties(CeQueueDto.Status status, @Nullable String workerUuid, int executionCount,
      Long startedAt, long updatedAt) {
      this.status = requireNonNull(status, "status can't be null");
      this.workerUuid = workerUuid;
      this.executionCount = executionCount;
      this.startedAt = startedAt;
      this.updatedAt = updatedAt;
    }

    public CeQueueDto.Status getStatus() {
      return status;
    }

    @CheckForNull
    public String getWorkerUuid() {
      return workerUuid;
    }

    public int getExecutionCount() {
      return executionCount;
    }

    @CheckForNull
    public Long getStartedAt() {
      return startedAt;
    }

    public long getUpdatedAt() {
      return updatedAt;
    }
  }

  @Immutable
  public static class OldProperties {
    private final CeQueueDto.Status status;
    private final int executionCount;

    OldProperties(CeQueueDto.Status status, int executionCount) {
      this.status = requireNonNull(status, "status can't be null");
      this.executionCount = executionCount;
    }

    public CeQueueDto.Status getStatus() {
      return status;
    }

    public int getExecutionCount() {
      return executionCount;
    }
  }

}
