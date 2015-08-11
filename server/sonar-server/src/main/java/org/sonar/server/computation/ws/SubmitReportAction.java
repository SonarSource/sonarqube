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

import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.computation.ReportProcessingScheduler;
import org.sonar.server.computation.ReportQueue;
import org.sonar.server.computation.monitoring.CEQueueStatus;
import org.sonar.server.user.UserSession;

public class SubmitReportAction implements ComputationWsAction {

  public static final String ACTION = "submit_report";
  public static final String PARAM_PROJECT_KEY = "projectKey";
  public static final String PARAM_PROJECT_NAME = "projectName";
  public static final String PARAM_REPORT_DATA = "report";

  private final ReportQueue queue;
  private final ReportProcessingScheduler workerLauncher;
  private final UserSession userSession;
  private final CEQueueStatus queueStatus;

  public SubmitReportAction(ReportQueue queue, ReportProcessingScheduler workerLauncher, UserSession userSession, CEQueueStatus queueStatus) {
    this.queue = queue;
    this.workerLauncher = workerLauncher;
    this.userSession = userSession;
    this.queueStatus = queueStatus;
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
      .createParam(PARAM_PROJECT_NAME)
      .setRequired(true)
      .setDescription("Project name")
      .setExampleValue("SonarQube");

    action
      .createParam(PARAM_REPORT_DATA)
      .setRequired(true)
      .setDescription("Report file. Format is not an API, it changes among SonarQube versions.");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkGlobalPermission(GlobalPermissions.SCAN_EXECUTION);
    String projectKey = request.mandatoryParam(PARAM_PROJECT_KEY);
    String projectName = request.mandatoryParam(PARAM_PROJECT_NAME);
    InputStream reportData = request.paramAsInputStream(PARAM_REPORT_DATA);
    try {
      ReportQueue.Item item = queue.add(projectKey, projectName, reportData);
      queueStatus.addReceived();
      workerLauncher.startAnalysisTaskNow();
      response.newJsonWriter()
        .beginObject()
        // do not write integer for forward-compatibility, for example
        // if we want to write UUID later
        .prop("key", String.valueOf(item.dto.getId()))
        .endObject()
        .close();
    } finally {
      IOUtils.closeQuietly(reportData);
    }
  }
}
