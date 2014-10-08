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

import com.google.common.annotations.VisibleForTesting;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AnalysisReportTaskLauncher implements Startable, ServerComponent {
  private final Logger LOG = LoggerFactory.getLogger(AnalysisReportTaskLauncher.class);

  private final ComputationService service;
  private final ScheduledExecutorService executorService;

  private final long delayBetweenTasks;
  private final long delayForFirstStart;
  private final TimeUnit timeUnit;

  public AnalysisReportTaskLauncher(ComputationService service) {
    this.service = service;
    this.executorService = Executors.newSingleThreadScheduledExecutor();

    this.delayBetweenTasks = 10;
    this.delayForFirstStart = 0;
    this.timeUnit = TimeUnit.SECONDS;
  }

  @VisibleForTesting
  AnalysisReportTaskLauncher(ComputationService service, long delayForFirstStart, long delayBetweenTasks, TimeUnit timeUnit) {
    this.executorService = Executors.newSingleThreadScheduledExecutor();

    this.delayBetweenTasks = delayBetweenTasks;
    this.delayForFirstStart = delayForFirstStart;
    this.timeUnit = timeUnit;
    this.service = service;
  }

  @Override
  public void start() {
    executorService.scheduleAtFixedRate(new AnalysisReportTask(service), delayForFirstStart, delayBetweenTasks, timeUnit);
    LOG.info("AnalysisReportTaskLauncher started");
  }

  @Override
  public void stop() {
    executorService.shutdown();
    LOG.info("AnalysisReportTaskLauncher stopped");
  }

  public void startAnalysisTaskNow() {
    executorService.execute(new AnalysisReportTask(service));
  }
}
