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

package org.sonar.server.batch;

import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.computation.AnalysisReportQueue;
import org.sonar.server.computation.AnalysisReportTaskLauncher;

import java.io.InputStream;

public class UploadReportAction implements RequestHandler {

  public static final String UPLOAD_REPORT_ACTION = "upload_report";

  static final String PARAM_PROJECT_KEY = "project";
  static final String PARAM_SNAPSHOT = "snapshot";
  static final String PARAM_REPORT_DATA = "report";

  private final AnalysisReportQueue analysisReportQueue;
  private final AnalysisReportTaskLauncher analysisTaskLauncher;

  public UploadReportAction(AnalysisReportQueue analysisReportQueue, AnalysisReportTaskLauncher analysisTaskLauncher) {
    this.analysisReportQueue = analysisReportQueue;
    this.analysisTaskLauncher = analysisTaskLauncher;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(UPLOAD_REPORT_ACTION)
      .setDescription("Update analysis report")
      .setSince("5.0")
      .setPost(true)
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_PROJECT_KEY)
      .setRequired(true)
      .setDescription("Project key")
      .setExampleValue("org.codehaus.sonar:sonar");

    action
      .createParam(PARAM_SNAPSHOT)
      .setRequired(true)
      .setDescription("Snapshot id")
      .setExampleValue("123");

    action
      .createParam(PARAM_REPORT_DATA)
      .setRequired(false)
      .setDescription("Report file");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PARAM_PROJECT_KEY);
    String snapshotId = request.mandatoryParam(PARAM_SNAPSHOT);
    InputStream reportData = request.paramAsInputStream(PARAM_REPORT_DATA);

    try {
      analysisReportQueue.add(projectKey, Long.valueOf(snapshotId), reportData);
      analysisTaskLauncher.startAnalysisTaskNow();
    } finally {
      IOUtils.closeQuietly(reportData);
    }
  }
}
