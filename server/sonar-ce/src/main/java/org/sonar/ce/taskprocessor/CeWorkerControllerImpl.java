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
import java.util.concurrent.ConcurrentHashMap;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.configuration.CeConfiguration;

public class CeWorkerControllerImpl implements CeWorkerController {
  private final ConcurrentHashMap<CeWorker, Status> workerStatuses = new ConcurrentHashMap<>();
  private final CeConfiguration ceConfiguration;

  enum Status {
    PROCESSING, PAUSED
  }

  public CeWorkerControllerImpl(CeConfiguration ceConfiguration) {
    this.ceConfiguration = ceConfiguration;
    logEnabledWorkerCount();
  }

  private void logEnabledWorkerCount() {
    int workerCount = ceConfiguration.getWorkerCount();
    if (workerCount > 1) {
      Loggers.get(CeWorkerController.class).info("Compute Engine will use {} concurrent workers to process tasks", workerCount);
    }
  }

  @Override
  public Optional<CeWorker> getCeWorkerIn(Thread thread) {
    return workerStatuses.keySet().stream()
      .filter(t -> t.isExecutedBy(thread))
      .findFirst();
  }

  @Override
  public ProcessingRecorderHook registerProcessingFor(CeWorker ceWorker) {
    return new ProcessingRecorderHookImpl(ceWorker);
  }

  @Override
  public boolean hasAtLeastOneProcessingWorker() {
    return workerStatuses.entrySet().stream().anyMatch(e -> e.getValue() == Status.PROCESSING);
  }

  /**
   * Returns {@code true} if {@link CeWorker#getOrdinal() worker ordinal} is strictly less than
   * {@link CeConfiguration#getWorkerCount()}.
   *
   * This method does not fail if ordinal is invalid (ie. < 0).
   */
  @Override
  public boolean isEnabled(CeWorker ceWorker) {
    return ceWorker.getOrdinal() < ceConfiguration.getWorkerCount();
  }

  private class ProcessingRecorderHookImpl implements ProcessingRecorderHook {
    private final CeWorker ceWorker;

    private ProcessingRecorderHookImpl(CeWorker ceWorker) {
      this.ceWorker = ceWorker;
      workerStatuses.put(this.ceWorker, Status.PROCESSING);
    }

    @Override
    public void close() {
      workerStatuses.put(ceWorker, Status.PAUSED);
    }
  }
}
