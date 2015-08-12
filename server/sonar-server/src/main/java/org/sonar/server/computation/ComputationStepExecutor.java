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
package org.sonar.server.computation;

import org.sonar.api.utils.log.Logger;
import org.sonar.core.util.logs.Profiler;
import org.sonar.server.computation.monitoring.CEQueueStatus;
import org.sonar.server.computation.step.ComputationStep;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public final class ComputationStepExecutor {
  private final Logger logger;
  private final Listener listener;
  private final String description;
  private final CEQueueStatus queueStatus;

  public ComputationStepExecutor(Logger logger, Listener listener, String description, CEQueueStatus queueStatus) {
    this.logger = requireNonNull(logger);
    this.listener = requireNonNull(listener);
    this.description = requireNonNull(description);
    this.queueStatus = queueStatus;
  }

  public void execute(Iterable<ComputationStep> steps) {
    queueStatus.addInProgress();
    listener.onStart();
    Profiler profiler = Profiler.create(logger).startDebug(description);
    long timingSum = 0L;
    Profiler stepProfiler = Profiler.create(logger);
    try {
      for (ComputationStep step : steps) {
        stepProfiler.start();
        step.execute();
        timingSum += stepProfiler.stopInfo(step.getDescription());
      }
      long timing = logProcessingEnd(description, profiler, timingSum);
      queueStatus.addSuccess(timing);
      listener.onSuccess(timing);
    } catch (Throwable e) {
      long timing = logProcessingEnd(description, profiler, timingSum);
      queueStatus.addError(timing);
      listener.onError(e, timing);
    } finally {
      listener.onEnd();
    }
  }

  private static long logProcessingEnd(String message, Profiler profiler, long timingSum) {
    return profiler.stopInfo(format("%s total time spent in steps=%sms", message, timingSum));
  }

  public interface Listener {

    /**
     * Called before the first ComputationStep is executed.
     */
    void onStart();

    /**
     * Called when on ComputationSteps have been executed and no error occurred.
     *
     * @param timing the duration of the execution
     */
    void onSuccess(long timing);

    /**
     * Called when on ComputationSteps have been executed and no error occurred.
     *
     * @param e the error
     * @param timing the duration of the execution
     */
    void onError(Throwable e, long timing);

    /**
     * Called when all ComputationSteps have been executed, after either {@link #onSuccess(long)} or {@link #onError(Throwable, long)}
     */
    void onEnd();
  }
}
