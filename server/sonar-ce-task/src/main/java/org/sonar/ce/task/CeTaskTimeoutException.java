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

import org.sonar.db.ce.CeActivityDto;

/**
 * This exception will stop the task and make it be archived with the {@link CeActivityDto.Status#FAILED FAILED}
 * status and the error type {@code TIMEOUT}.
 * <p>
 * This exception has no stacktrace:
 * <ul>
 *   <li>it's irrelevant to the end user</li>
 *   <li>we don't want to leak any implementation detail</li>
 * </ul>
 */
public final class CeTaskTimeoutException extends CeTaskInterruptedException implements TypedException {

  public CeTaskTimeoutException(String message) {
    super(message, CeActivityDto.Status.FAILED);
  }

  @Override
  public String getType() {
    return "TIMEOUT";
  }

  /**
   * Does not fill in the stack trace
   *
   * @see Throwable#fillInStackTrace()
   */
  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
}
