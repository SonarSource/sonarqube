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
package org.sonar.server.platform.db.migration;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * This implementation of {@link MutableDatabaseMigrationState} does not provide any thread safety.
 */
public class DatabaseMigrationStateImpl implements MutableDatabaseMigrationState {
  private Status status = Status.NONE;
  @Nullable
  private Instant startedAt = null;
  @Nullable
  private Throwable error = null;
  private int completedMigrations = 0;
  private int totalMigrations = 0;

  @Override
  public Status getStatus() {
    return status;
  }

  @Override
  public void setStatus(Status status) {
    this.status = status;
  }

  @Override
  public Optional<Instant> getStartedAt() {
    return Optional.ofNullable(startedAt);
  }

  @Override
  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }

  @Override
  public Optional<Throwable> getError() {
    return Optional.ofNullable(error);
  }

  @Override
  public void incrementCompletedMigrations() {
    completedMigrations++;
  }

  @Override
  public int getCompletedMigrations() {
    return completedMigrations;
  }

  @Override
  public void setTotalMigrations(int totalMigrations) {
    this.totalMigrations = totalMigrations;
  }

  @Override
  public int getTotalMigrations() {
    return totalMigrations;
  }

  @Override
  public void setError(@Nullable Throwable error) {
    this.error = error;
  }

  @Override
  public Optional<Instant> getExpectedFinishDate(Instant now) {
    if (this.getStatus() != Status.RUNNING || startedAt == null || totalMigrations == 0 || completedMigrations == 0) {
      return Optional.empty();
    }
    Duration elapsed = Duration.between(startedAt, now);
    double progress = (double) completedMigrations / totalMigrations;
    return Optional.of(startedAt.plusMillis((long) (elapsed.toMillis() / progress)));
  }
}
