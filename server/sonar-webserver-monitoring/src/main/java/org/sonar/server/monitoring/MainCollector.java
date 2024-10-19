/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.monitoring;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.sonar.api.Startable;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class MainCollector implements Startable {

  private final MonitoringTask[] monitoringTasks;
  private ScheduledExecutorService scheduledExecutorService;

  public MainCollector(MonitoringTask[] monitoringTasks) {
    this.monitoringTasks = monitoringTasks;
  }

  @Override
  public void start() {
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat(getClass().getCanonicalName() + "-thread-%d")
        .build());
    for (MonitoringTask task : monitoringTasks) {
      scheduledExecutorService.scheduleWithFixedDelay(task, task.getDelay(), task.getPeriod(), MILLISECONDS);
    }
  }

  @Override
  public void stop() {
    scheduledExecutorService.shutdown();
  }

  @VisibleForTesting
  ScheduledExecutorService getScheduledExecutorService() {
    return scheduledExecutorService;
  }
}
