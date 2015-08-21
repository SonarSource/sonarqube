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
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.computation.activity.ActivityManager;
import org.sonar.server.computation.monitoring.CEQueueStatus;
import org.sonar.server.computation.step.ComputationSteps;

import static java.lang.String.format;
import static org.sonar.db.compute.AnalysisReportDto.Status.FAILED;
import static org.sonar.db.compute.AnalysisReportDto.Status.SUCCESS;

public class ReportProcessor {

  private final ComputationStepExecutor executor;
  private final ComputationSteps steps;

  public ReportProcessor(ComputationSteps steps,
    ReportQueue.Item item, ActivityManager activityManager, System2 system, CEQueueStatus queueStatus) {
    this.executor = new ComputationStepExecutor(
      Loggers.get(ReportProcessor.class),
      new ReportProcessingStepsExecutorListener(item, system, activityManager, queueStatus),
        createDescription(item), queueStatus);
    this.steps = steps;
  }

  public void process() {
    this.executor.execute(this.steps.instances());
  }

  private static String createDescription(ReportQueue.Item item) {
    String projectKey = item.dto.getProjectKey();
    return format("Analysis of project %s (report %d)", projectKey, item.dto.getId());
  }

  private static class ReportProcessingStepsExecutorListener implements ComputationStepExecutor.Listener {
    private final ReportQueue.Item item;
    private final System2 system;
    private final ActivityManager activityManager;

    private ReportProcessingStepsExecutorListener(ReportQueue.Item item, System2 system, ActivityManager activityManager, CEQueueStatus queueStatus) {
      this.item = item;
      this.system = system;
      this.activityManager = activityManager;
    }

    @Override
    public void onStart() {
      // nothing to do on start
    }

    @Override
    public void onSuccess(long timing) {
      item.dto.setStatus(SUCCESS);
    }

    @Override
    public void onError(Throwable e, long timing) {
      item.dto.setStatus(FAILED);
      throw Throwables.propagate(e);
    }

    @Override
    public void onEnd() {
      item.dto.setFinishedAt(system.now());
      activityManager.saveActivity(item.dto);
    }
  }

}
