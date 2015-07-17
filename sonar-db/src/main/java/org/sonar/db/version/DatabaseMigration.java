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
package org.sonar.db.version;

import java.util.Date;
import javax.annotation.CheckForNull;

public interface DatabaseMigration {
  enum Status {
    NONE, RUNNING, FAILED, SUCCEEDED
  }

  /**
   * Starts the migration status and returns immediately.
   * <p>
   * Migration can not be started twice but calling this method wont raise an error.
   * On the other hand, calling this method when no migration is needed will start the process anyway.
   * </p>
   * <p>
   * <strong>This method should be named {@code start} but it can not be because it will be called by the pico container
   * and this will cause unwanted behavior</strong>
   * </p>
   */
  void startIt();

  /**
   * The time and day the last migration was started.
   * <p>
   * If no migration was ever started, the returned date is {@code null}. This value is reset when {@link #startIt()} is
   * called.
   * </p>
   *
   * @return a {@link Date} or {@code null}
   */
  @CheckForNull
  Date startedAt();

  /**
   * Current status of the migration.
   */
  Status status();

  /**
   * The error of the last migration if it failed.
   * <p>
   * This value is reset when {@link #startIt()} is called.
   * </p>
   * @return a {@link Throwable} or {@code null}
   */
  @CheckForNull
  Throwable failureError();

}
