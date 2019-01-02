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

/**
 * Method {@link #check(Thread)} of the {@link CeTaskInterrupter} can be called during the processing of a
 * {@link CeTask} to check whether processing of this task must be interrupted.
 * <p>
 * Interruption cause may be user cancelling the task, operator stopping the Compute Engine, execution running for
 * too long, ...
 */
public interface CeTaskInterrupter {
  /**
   * @throws CeTaskInterruptedException if the execution of the task must be interrupted
   */
  void check(Thread currentThread) throws CeTaskInterruptedException;

  /**
   * Lets the interrupter know that the processing of the specified task has started.
   */
  void onStart(CeTask ceTask);

  /**
   * Lets the interrupter know that the processing of the specified task has ended.
   */
  void onEnd(CeTask ceTask);
}
