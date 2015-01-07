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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.computation.AnalysisReportQueue;
import org.sonar.server.computation.AnalysisReportTaskLauncher;

import java.io.InputStream;

public class SubmitReportWsAction implements ComputationWsAction, RequestHandler {

  public static final String ACTION = "submit_report";
  public static final String PARAM_PROJECT_KEY = "projectKey";
  public static final String PARAM_SNAPSHOT = "snapshot";
  public static final String PARAM_REPORT_DATA = "report";

  private final AnalysisReportQueue queue;
  private final AnalysisReportTaskLauncher taskLauncher;

  public SubmitReportWsAction(AnalysisReportQueue queue, AnalysisReportTaskLauncher taskLauncher) {
    this.queue = queue;
    this.taskLauncher = taskLauncher;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION)
      .setDescription("Submit an analysis report to the queue. Report is integrated asynchronously.")
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
      .setDescription("Snapshot ID")
      .setExampleValue("123");

    action
      .createParam(PARAM_REPORT_DATA)
      .setRequired(false)
      .setDescription("Report file. Format is not an API, it changes among SonarQube versions.");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PARAM_PROJECT_KEY);
    long snapshotId = request.mandatoryParamAsLong(PARAM_SNAPSHOT);
    try (InputStream reportData = request.paramAsInputStream(PARAM_REPORT_DATA)) {
      queue.add(projectKey, snapshotId, reportData);
      taskLauncher.startAnalysisTaskNow();
    }
  }
}
