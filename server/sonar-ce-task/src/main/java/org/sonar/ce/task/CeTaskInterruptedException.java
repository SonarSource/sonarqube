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
package org.sonar.ce.task;

import java.util.Optional;
import org.sonar.db.ce.CeActivityDto;

import static java.util.Objects.requireNonNull;

public abstract class CeTaskInterruptedException extends RuntimeException {
  private final CeActivityDto.Status status;

  protected CeTaskInterruptedException(String message, CeActivityDto.Status status) {
    super(message);
    this.status = requireNonNull(status, "status can't be null");
  }

  public CeActivityDto.Status getStatus() {
    return status;
  }

  public static Optional<CeTaskInterruptedException> isTaskInterruptedException(Throwable e) {
    if (e instanceof CeTaskInterruptedException) {
      return Optional.of((CeTaskInterruptedException) e);
    }
    return isCauseInterruptedException(e);
  }

  private static Optional<CeTaskInterruptedException> isCauseInterruptedException(Throwable e) {
    Throwable cause = e.getCause();
    if (cause == null || cause == e) {
      return Optional.empty();
    }
    if (cause instanceof CeTaskInterruptedException) {
      return Optional.of((CeTaskInterruptedException) cause);
    }
    return isCauseInterruptedException(cause);
  }
}
