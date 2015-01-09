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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.computation.db.AnalysisReportDto;

/**
 * This thread pops queue of reports and processes the report if present
 */
public class ComputationWorker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(ComputationWorker.class);

  private final AnalysisReportQueue queue;
  private final ComputationService service;

  public ComputationWorker(AnalysisReportQueue queue, ComputationService service) {
    this.queue = queue;
    this.service = service;
  }

  @Override
  public void run() {
    AnalysisReportDto report = queue.pop();
    if (report != null) {
      try {
        service.process(report);
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
}
