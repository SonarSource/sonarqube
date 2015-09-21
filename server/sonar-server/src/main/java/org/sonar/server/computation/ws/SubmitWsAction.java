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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.computation.queue.CeTask;
import org.sonar.server.computation.queue.report.ReportSubmitter;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.WsCe;

public class SubmitWsAction implements CeWsAction {

  public static final String PARAM_PROJECT_KEY = "projectKey";
  public static final String PARAM_PROJECT_BRANCH = "projectBranch";
  public static final String PARAM_PROJECT_NAME = "projectName";
  public static final String PARAM_REPORT_DATA = "report";

  private final ReportSubmitter reportSubmitter;

  public SubmitWsAction(ReportSubmitter reportSubmitter) {
    this.reportSubmitter = reportSubmitter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("submit")
      .setDescription("Submit an analysis report to the queue of Compute Engine. Report is processed asynchronously.")
      .setPost(true)
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(getClass().getResource("submit-example.json"));

    action
      .createParam(PARAM_PROJECT_KEY)
      .setRequired(true)
      .setDescription("Key of project")
      .setExampleValue("my_project");

    action
      .createParam(PARAM_PROJECT_BRANCH)
      .setDescription("Optional branch of project")
      .setExampleValue("branch-1.x");

    action
      .createParam(PARAM_PROJECT_NAME)
      .setRequired(false)
      .setDescription("Optional name of the project, used only if the project does not exist yet.")
      .setExampleValue("My Project");

    action
      .createParam(PARAM_REPORT_DATA)
      .setRequired(true)
      .setDescription("Report file. Format is not an API, it changes among SonarQube versions.");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    String projectKey = wsRequest.mandatoryParam(PARAM_PROJECT_KEY);
    String projectBranch = wsRequest.param(PARAM_PROJECT_BRANCH);
    String projectName = StringUtils.defaultIfBlank(wsRequest.param(PARAM_PROJECT_NAME), projectKey);
    InputStream reportInput = wsRequest.paramAsInputStream(PARAM_REPORT_DATA);

    CeTask task = reportSubmitter.submit(projectKey, projectBranch, projectName, reportInput);

    WsCe.SubmitResponse submitResponse = WsCe.SubmitResponse.newBuilder()
      .setTaskId(task.getUuid())
      .setProjectId(task.getComponentUuid())
      .build();
    WsUtils.writeProtobuf(submitResponse, wsRequest, wsResponse);
  }
}
