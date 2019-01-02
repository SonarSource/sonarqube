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
package org.sonar.server.platform.db.migration;

import java.util.Date;
import javax.annotation.CheckForNull;

public interface DatabaseMigrationState {

  enum Status {
    NONE, RUNNING, FAILED, SUCCEEDED
  }

  /**
   * Current status of the migration.
   */
  Status getStatus();

  /**
   * The time and day the last migration was started.
   * <p>
   * If no migration was ever started, the returned date is {@code null}.
   * </p>
   *
   * @return a {@link Date} or {@code null}
   */
  @CheckForNull
  Date getStartedAt();

  /**
   * The error of the last migration if it failed.
   *
   * @return a {@link Throwable} or {@code null}
   */
  @CheckForNull
  Throwable getError();
}
