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
package org.sonar.ce.taskprocessor;

import java.util.Optional;

/**
 * This class is responsible of knowing/deciding which {@link CeWorker} is enabled and should actually try and find a
 * task to process.
 */
public interface CeWorkerController {
  interface ProcessingRecorderHook extends AutoCloseable {
    /**
     * Override to not declare any exception thrown.
     */
    @Override
    void close();
  }

  /**
   * Registers to the controller that the specified {@link CeWorker}
   */
  ProcessingRecorderHook registerProcessingFor(CeWorker ceWorker);

  /**
   * Returns {@code true} if the specified {@link CeWorker} is enabled
   */
  boolean isEnabled(CeWorker ceWorker);

  /**
   * @return the {@link CeWorker} running in the specified {@link Thread}, if any.
   */
  Optional<CeWorker> getCeWorkerIn(Thread thread);

  /**
   * Whether at least one worker is processing a task or not.
   *
   * @return {@code false} when all workers are waiting for tasks or are being stopped.
   */
  boolean hasAtLeastOneProcessingWorker();
}
