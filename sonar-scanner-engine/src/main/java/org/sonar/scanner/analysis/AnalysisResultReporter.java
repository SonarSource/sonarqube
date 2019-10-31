/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner.analysis;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.report.CeTaskReportDataHolder;
import org.sonar.scanner.scan.ScanProperties;

public class AnalysisResultReporter {

  private static final Logger LOG = Loggers.get(AnalysisResultReporter.class);

  private final CeTaskReportDataHolder ceTaskReportDataHolder;
  private final GlobalAnalysisMode analysisMode;
  private final ScanProperties scanProperties;

  public AnalysisResultReporter(GlobalAnalysisMode analysisMode, CeTaskReportDataHolder ceTaskReportDataHolder,
    ScanProperties scanProperties) {
    this.analysisMode = analysisMode;
    this.ceTaskReportDataHolder = ceTaskReportDataHolder;
    this.scanProperties = scanProperties;
  }

  public void report() {
    if (analysisMode.isMediumTest()) {
      LOG.info("ANALYSIS SUCCESSFUL");
    } else {
      LOG.info("ANALYSIS SUCCESSFUL, you can browse {}", ceTaskReportDataHolder.getDashboardUrl());
      if (!scanProperties.shouldWaitForQualityGate()) {
        LOG.info("Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report");
      }
      LOG.info("More about the report processing at {}", ceTaskReportDataHolder.getCeTaskUrl());
    }
  }
}
