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


import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.server.computation.step.ComputationSteps;
import org.sonar.server.platform.Platform;

/**
 * This thread pops queue of reports and processes the report if present
 */
public class ComputationThread implements Runnable {
  private static final Logger LOG = Loggers.get(ComputationThread.class);

  private final AnalysisReportQueue queue;

  public ComputationThread(AnalysisReportQueue queue) {
    this.queue = queue;
  }

  @Override
  public void run() {
    AnalysisReportDto report = null;
    try {
      report = queue.pop();
    } catch (Exception e) {
      LOG.error("Failed to pop the queue of analysis reports", e);
    }
    if (report != null) {
      try {
        process(report);
      } catch (Exception e) {
        LOG.error(String.format(
          "Failed to process analysis report %d of project %s", report.getId(), report.getProjectKey()), e);
      } finally {
        removeSilentlyFromQueue(report);
      }
    }
  }

  private void removeSilentlyFromQueue(AnalysisReportDto report) {
    try {
      queue.remove(report);
    } catch (Exception e) {
      LOG.error(String.format("Failed to remove analysis report %d from queue", report.getId()), e);
    }
  }

  private void process(AnalysisReportDto report) {
    ComponentContainer container = Platform.getInstance().getContainer();
    ComponentContainer child = container.createChild();
    child.addSingletons(ComputationSteps.orderedStepClasses());
    child.addSingletons(ComputationComponents.nonStepComponents());
    child.startComponents();
    try {
      child.getComponentByType(ComputationService.class).process(report);
    } finally {
      child.stopComponents();
      // TODO not possible to have multiple children -> will be
      // a problem when we will have multiple concurrent computation workers
      container.removeChild();
    }
  }
}
