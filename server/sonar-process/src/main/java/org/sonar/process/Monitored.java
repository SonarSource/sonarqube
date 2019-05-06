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
package org.sonar.process;

/**
 * Base interface for the processes started by the bootstrap process.
 * It provides the information and operations required by {@link ProcessEntryPoint}
 * to handle the lifecycle of the process.
 */
public interface Monitored {

  /**
   * Starts process. No need to block until fully started and operational.
   */
  void start();

  /**
   * @return {@link Status#UP} if the process is done starting, {@link Status#OPERATIONAL} if the service is operation,
   *         {@link Status#FAILED} if the process failed to start, {@link Status#DOWN} otherwise.
   */
  Status getStatus();

  enum Status {
    DOWN, UP, OPERATIONAL, FAILED
  }

  /**
   * Blocks until the process is stopped
   */
  void awaitStop();

  /**
   * Ask process to gracefully stop and wait until then.
   */
  void stop();

  /**
   * Ask process to quickly stop and wait until then.
   */
  void hardStop();
}
