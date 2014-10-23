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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerStartHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class AnalysisReportTaskLauncher implements Startable, ServerComponent, ServerStartHandler {

  public static final String ANALYSIS_REPORT_THREAD_NAME_PREFIX = "ar-";
  private static final Logger LOG = LoggerFactory.getLogger(AnalysisReportTaskLauncher.class);

  private final ComputationService service;
  private final AnalysisReportQueue queue;
  private final ScheduledExecutorService executorService;

  private final long delayBetweenTasks;
  private final long delayForFirstStart;
  private final TimeUnit timeUnit;

  public AnalysisReportTaskLauncher(ComputationService service, AnalysisReportQueue queue) {
    this.service = service;
    this.queue = queue;
    this.executorService = Executors.newSingleThreadScheduledExecutor(threadFactoryWithSpecificNameForLogging());

    this.delayBetweenTasks = 10;
    this.delayForFirstStart = 0;
    this.timeUnit = TimeUnit.SECONDS;
  }

  @VisibleForTesting
  AnalysisReportTaskLauncher(ComputationService service, AnalysisReportQueue queue, long delayForFirstStart, long delayBetweenTasks, TimeUnit timeUnit) {
    this.queue = queue;
    this.executorService = Executors.newSingleThreadScheduledExecutor(threadFactoryWithSpecificNameForLogging());

    this.delayBetweenTasks = delayBetweenTasks;
    this.delayForFirstStart = delayForFirstStart;
    this.timeUnit = timeUnit;
    this.service = service;
  }

  /**
   * @see org.sonar.server.platform.SwitchLogbackAppender
   */
  private ThreadFactory threadFactoryWithSpecificNameForLogging() {
    return new ThreadFactoryBuilder()
      .setNameFormat(ANALYSIS_REPORT_THREAD_NAME_PREFIX + "%d").setPriority(Thread.MIN_PRIORITY).build();
  }

  @Override
  public void start() {
    // do nothing because we want to wait for the server to finish startup
  }

  @Override
  public void stop() {
    executorService.shutdown();
    LOG.info("AnalysisReportTaskLauncher gracefully stopped");
  }

  public void startAnalysisTaskNow() {
    executorService.execute(new AnalysisReportTask(queue, service));
  }

  @Override
  public void onServerStart(Server server) {
    executorService.scheduleAtFixedRate(new AnalysisReportTask(queue, service), delayForFirstStart, delayBetweenTasks, timeUnit);
    LOG.info("AnalysisReportTaskLauncher started");
  }
}
