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

import com.google.common.base.Throwables;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.logs.Profiler;
import org.sonar.server.computation.activity.ActivityManager;
import org.sonar.server.computation.monitoring.CEQueueStatus;
import org.sonar.server.computation.step.ComputationStep;
import org.sonar.server.computation.step.ComputationSteps;

import static java.lang.String.format;
import static org.sonar.db.compute.AnalysisReportDto.Status.FAILED;
import static org.sonar.db.compute.AnalysisReportDto.Status.SUCCESS;

@ServerSide
public class ReportProcessor {

  private static final Logger LOG = Loggers.get(ReportProcessor.class);

  private final ReportQueue.Item item;
  private final ComputationSteps steps;
  private final ActivityManager activityManager;
  private final System2 system;
  private final CEQueueStatus queueStatus;

  public ReportProcessor(ReportQueue.Item item, ComputationSteps steps, ActivityManager activityManager, System2 system,
    CEQueueStatus queueStatus) {
    this.item = item;
    this.steps = steps;
    this.activityManager = activityManager;
    this.system = system;
    this.queueStatus = queueStatus;
  }

  public void process() {
    queueStatus.addInProgress();
    String projectKey = item.dto.getProjectKey();
    String message = format("Analysis of project %s (report %d)", projectKey, item.dto.getId());
    Profiler profiler = Profiler.create(LOG).startDebug(message);

    long timingSum = 0L;
    Profiler stepProfiler = Profiler.create(LOG);
    try {
      for (ComputationStep step : steps.instances()) {
        stepProfiler.start();
        step.execute();
        timingSum += stepProfiler.stopInfo(step.getDescription());
      }
      item.dto.setStatus(SUCCESS);
      long timing = logProcessingEnd(message, profiler, timingSum);
      queueStatus.addSuccess(timing);
    } catch (Throwable e) {
      item.dto.setStatus(FAILED);
      long timing = logProcessingEnd(message, profiler, timingSum);
      queueStatus.addError(timing);
      throw Throwables.propagate(e);
    } finally {
      item.dto.setFinishedAt(system.now());
      activityManager.saveActivity(item.dto);
    }
  }

  private static long logProcessingEnd(String message, Profiler profiler, long timingSum) {
    return profiler.stopInfo(format("%s total time spent in steps=%sms", message, timingSum));
  }
}
