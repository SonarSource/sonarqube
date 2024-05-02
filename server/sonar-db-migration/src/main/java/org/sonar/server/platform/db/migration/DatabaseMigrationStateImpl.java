/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * This implementation of {@link MutableDatabaseMigrationState} does not provide any thread safety.
 */
public class DatabaseMigrationStateImpl implements MutableDatabaseMigrationState {
  private Status status = Status.NONE;
  @Nullable
  private Date startedAt = null;
  @Nullable
  private Throwable error = null;
  private int completedMigrations = 0;
  private int totalMigrations = 0;
  @Nullable
  private Date completionExpectedAt = null;

  @Override
  public Status getStatus() {
    return status;
  }

  @Override
  public void setStatus(Status status) {
    this.status = status;
  }

  @Override
  @CheckForNull
  public Date getStartedAt() {
    return startedAt;
  }

  @Override
  public void setStartedAt(@Nullable Date startedAt) {
    this.startedAt = startedAt;
  }

  @Override
  @CheckForNull
  public Throwable getError() {
    return error;
  }

  @Override
  public void incrementCompletedMigrations() {
    completedMigrations++;
    updateExpectedFinishDate();
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
  public Date getExpectedFinishDate() {
    return completionExpectedAt;
  }

  private void updateExpectedFinishDate() {
    // Here the logic is to calculate the expected finish date based on the current time and the number of migrations completed and total
    // migrations
    this.completionExpectedAt = new Date();
  }

}
