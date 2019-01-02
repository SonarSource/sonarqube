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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskInterruptedException;
import org.sonar.ce.task.CeTaskTimeoutException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * An implementation of {@link org.sonar.ce.task.CeTaskInterrupter} which interrupts the processing of the task
 * if:
 * <ul>
 *   <li>the thread has been interrupted</li>
 *   <li>it's been running for more than a certain, configurable, amount of time</li>
 * </ul>
 */
public class TimeoutCeTaskInterrupter extends SimpleCeTaskInterrupter {
  private final long taskTimeoutThreshold;
  private final CeWorkerController ceWorkerController;
  private final System2 system2;
  private final Map<String, Long> startTimestampByCeTaskUuid = new HashMap<>();

  public TimeoutCeTaskInterrupter(long taskTimeoutThreshold, CeWorkerController ceWorkerController, System2 system2) {
    checkArgument(taskTimeoutThreshold >= 1, "threshold must be >= 1");
    Loggers.get(TimeoutCeTaskInterrupter.class).info("Compute Engine Task timeout enabled: {} ms", taskTimeoutThreshold);

    this.taskTimeoutThreshold = taskTimeoutThreshold;
    this.ceWorkerController = ceWorkerController;
    this.system2 = system2;
  }

  @Override
  public void check(Thread currentThread) throws CeTaskInterruptedException {
    super.check(currentThread);

    computeTimeOutOf(taskOf(currentThread))
      .ifPresent(timeout -> {
        throw new CeTaskTimeoutException(format("Execution of task timed out after %s ms", timeout));
      });
  }

  private Optional<Long> computeTimeOutOf(CeTask ceTask) {
    Long startTimestamp = startTimestampByCeTaskUuid.get(ceTask.getUuid());
    checkState(startTimestamp != null, "No start time recorded for task %s", ceTask.getUuid());

    long duration = system2.now() - startTimestamp;
    return Optional.of(duration)
      .filter(t -> t > taskTimeoutThreshold);
  }

  private CeTask taskOf(Thread currentThread) {
    return ceWorkerController.getCeWorkerIn(currentThread)
      .flatMap(CeWorker::getCurrentTask)
      .orElseThrow(() -> new IllegalStateException(format("Could not find the CeTask being executed in thread '%s'", currentThread.getName())));
  }

  @Override
  public void onStart(CeTask ceTask) {
    long now = system2.now();
    Long existingTimestamp = startTimestampByCeTaskUuid.put(ceTask.getUuid(), now);
    if (existingTimestamp != null) {
      Loggers.get(TimeoutCeTaskInterrupter.class)
        .warn("Notified of start of execution of task %s but start had already been recorded at %s. Recording new start at %s",
          ceTask.getUuid(), existingTimestamp, now);
    }
  }

  @Override
  public void onEnd(CeTask ceTask) {
    Long startTimestamp = startTimestampByCeTaskUuid.remove(ceTask.getUuid());
    if (startTimestamp == null) {
      Loggers.get(TimeoutCeTaskInterrupter.class)
        .warn("Notified of end of execution of task %s but start wasn't recorded", ceTask.getUuid());
    }
  }

}
