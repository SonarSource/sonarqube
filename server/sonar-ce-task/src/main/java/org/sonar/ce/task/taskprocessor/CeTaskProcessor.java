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
package org.sonar.ce.task.taskprocessor;

import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskResult;

/**
 * This interface is used to provide the processing code for {@link CeTask}s of one or more type to be called by the
 * Compute Engine.
 */
@ComputeEngineSide
@ServerSide
public interface CeTaskProcessor {

  /**
   * The {@link CeTask#getType()} for which this {@link CeTaskProcessor} provides the processing code.
   * <p>
   * The match of type is done using {@link String#equals(Object)} and if more than one {@link CeTaskProcessor} declares
   * itself had handler for the same {@link CeTask#getType()}, an error will be raised at startup and startup will
   * fail.
   * </p>
   * <p>
   * If an empty {@link Set} is returned, the {@link CeTaskProcessor} will be ignored.
   * </p>
   */
  Set<String> getHandledCeTaskTypes();

  /**
   * Calls the processing code for a specific {@link CeTask} which will optionally return a {@link CeTaskResult}
   * holding information to be persisted in the processing history of the Compute Engine (currently the {@code CE_ACTIVITY} table).
   * <p>
   * The specified is guaranteed to be non {@code null} and its {@link CeTask#getType()} to be one of the values
   * of {@link #getHandledCeTaskTypes()}.
   * </p>
   *
   * @throws RuntimeException when thrown, it will be caught and logged by the Compute Engine and the processing of the
   *         specified {@link CeTask} will be flagged as failed.
   */
  @CheckForNull
  CeTaskResult process(CeTask task);
}
