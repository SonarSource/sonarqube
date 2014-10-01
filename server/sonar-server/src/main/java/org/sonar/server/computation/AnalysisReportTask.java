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

import org.sonar.core.computation.db.AnalysisReportDto;

import java.util.concurrent.TimeUnit;

public class AnalysisReportTask extends Thread {
  private static final String TASK_NAME = "AnalysisReportTask";
  private static final int SLEEP_DURATION_IN_SECONDS = 10;

  private final ComputationService service;

  // TODO to improve – the computationService singleton should be retrieved directly in the pico container
  public AnalysisReportTask(ComputationService service) {
    super(TASK_NAME);
    this.service = service;
  }

  @Override
  public void run() {
    while (!this.isInterrupted()) {
      AnalysisReportDto report = service.findAndBookNextAnalysisReport();
      if (report == null) {
        sleepBeforeNextAttempt();
      } else {
        service.analyzeReport(report);
      }
    }
  }

  private void sleepBeforeNextAttempt() {
    try {
      TimeUnit.SECONDS.sleep(SLEEP_DURATION_IN_SECONDS);
    } catch (InterruptedException e) {
      // thread interrupted while sleeping, no action needed
    }
  }
}
