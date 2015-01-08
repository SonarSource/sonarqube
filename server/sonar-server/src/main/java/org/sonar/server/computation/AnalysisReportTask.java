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

import javax.annotation.CheckForNull;

public class AnalysisReportTask implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(AnalysisReportTask.class);

  private final AnalysisReportQueue queue;
  private final ComputationService service;

  public AnalysisReportTask(AnalysisReportQueue queue, ComputationService service) {
    this.queue = queue;
    this.service = service;
  }

  @Override
  public void run() {
    AnalysisReportDto report = bookNextAvailableReport();
    if (report != null) {
      analyzeReport(report);
    }
  }

  private void analyzeReport(AnalysisReportDto report) {
    try {
      service.analyzeReport(report);
    } catch (Exception exception) {
      LOG.error(String.format("Analysis of report %s failed", report), exception);
    } finally {
      removeSilentlyFromQueue(report);
    }
  }

  @CheckForNull
  private AnalysisReportDto bookNextAvailableReport() {
    try {
      return queue.bookNextAvailable();
    } catch (Exception e) {
      LOG.error("Booking next available analysis report failed", e);
      return null;
    }
  }

  private void removeSilentlyFromQueue(AnalysisReportDto report) {
    try {
      queue.remove(report);
    } catch (Exception exception) {
      LOG.error(String.format("Removing of report %s failed", report), exception);
    }
  }
}
