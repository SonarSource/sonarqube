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
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.util.logs.Profiler;
import org.sonar.server.computation.container.ComputeEngineContainer;
import org.sonar.server.computation.container.ContainerFactory;

/**
 * This Compute E pops a report from the queue and integrate it.
 */
public class ReportProcessingTask implements ComputeEngineTask {

  private static final Logger LOG = Loggers.get(ReportProcessingTask.class);

  private final ReportQueue queue;
  private final ComponentContainer sqContainer;
  private final ContainerFactory containerFactory;

  public ReportProcessingTask(ReportQueue queue, ComponentContainer sqContainer, ContainerFactory containerFactory) {
    this.queue = queue;
    this.sqContainer = sqContainer;
    this.containerFactory = containerFactory;
  }

  @Override
  public void run() {
    Profiler profiler = Profiler.create(LOG).start();
    ReportQueue.Item item = null;
    try {
      item = queue.pop();
    } catch (Exception e) {
      LOG.error("Failed to pop the queue of analysis reports", e);
    }
    if (item == null) {
      return;
    }

    ComputeEngineContainer computeEngineContainer = containerFactory.create(sqContainer, item);
    try {
      computeEngineContainer.getComponentByType(ReportProcessor.class).process();
    } catch (Throwable e) {
      LOG.error(String.format(
        "Failed to process analysis report %d of project %s", item.dto.getId(), item.dto.getProjectKey()), e);
    } finally {
      computeEngineContainer.cleanup();

      removeSilentlyFromQueue(item);
    }
    profiler.stopInfo(String.format("Total thread execution of project %s (report %d)", item.dto.getProjectKey(), item.dto.getId()));
  }

  private void removeSilentlyFromQueue(ReportQueue.Item item) {
    try {
      queue.remove(item);
    } catch (Exception e) {
      LOG.error(String.format("Failed to remove analysis report %d from queue", item.dto.getId()), e);
    }
  }
}
