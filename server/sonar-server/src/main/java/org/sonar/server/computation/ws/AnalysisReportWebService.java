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

package org.sonar.server.computation.ws;

import org.sonar.api.server.ws.WebService;

public class AnalysisReportWebService implements WebService {
  public static final String API_ENDPOINT = "api/analysis_reports";

  private final ActiveAnalysisReportsAction activeAnalysisReportsAction;
  private final IsAnalysisReportQueueEmptyAction isAnalysisReportQueueEmptyAction;
  private final AnalysisReportHistorySearchAction historySearchAction;

  public AnalysisReportWebService(ActiveAnalysisReportsAction activeReports, IsAnalysisReportQueueEmptyAction isAnalysisReportQueueEmptyAction,
    AnalysisReportHistorySearchAction historySearchAction) {
    this.activeAnalysisReportsAction = activeReports;
    this.isAnalysisReportQueueEmptyAction = isAnalysisReportQueueEmptyAction;
    this.historySearchAction = historySearchAction;
  }

  @Override
  public void define(Context context) {
    NewController controller = context
      .createController(API_ENDPOINT)
      .setDescription("Analysis reports processed");

    activeAnalysisReportsAction.define(controller);
    isAnalysisReportQueueEmptyAction.define(controller);
    historySearchAction.define(controller);

    controller.done();
  }
}
