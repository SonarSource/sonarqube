/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.projectdump.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.server.ce.projectdump.ExportSubmitter;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.ce.task.CeTask;
import org.sonar.server.user.UserSession;

public class ExportAction implements ProjectDumpAction {

  public static final String ACTION_KEY = "export";
  private static final String PARAMETER_PROJECT_KEY = "key";

  private final UserSession userSession;
  private final ExportSubmitter exportSubmitter;
  private final ProjectDumpWsSupport projectDumpWsSupport;

  public ExportAction(ProjectDumpWsSupport projectDumpWsSupport, UserSession userSession, ExportSubmitter exportSubmitter) {
    this.projectDumpWsSupport = projectDumpWsSupport;
    this.userSession = userSession;
    this.exportSubmitter = exportSubmitter;
  }

  @Override
  public void define(WebService.NewController newController) {
    WebService.NewAction newAction = newController.createAction(ACTION_KEY)
      .setDescription("Triggers project dump so that the project can be imported to another SonarQube server " +
        "(see " + ProjectDumpWs.CONTROLLER_PATH + "/import, available in Enterprise Edition). " +
        "Requires the 'Administer' permission.")
      .setSince("1.0")
      .setPost(true)
      .setHandler(this)
      .setChangelog(new Change("9.2", "Moved from Enterprise Edition to Community Edition"))
      .setResponseExample(getClass().getResource("example-export.json"));
    newAction.createParam(PARAMETER_PROJECT_KEY)
      .setRequired(true)
      .setExampleValue("my_project");
  }

  @Override
  public void handle(Request request, Response response) {
    String projectKey = request.mandatoryParam(PARAMETER_PROJECT_KEY);
    projectDumpWsSupport.verifyAdminOfProjectByKey(projectKey);

    CeTask task = exportSubmitter.submitProjectExport(projectKey, userSession.getUuid());
    try (JsonWriter writer = response.newJsonWriter()) {
      CeTask.Component component = task.getComponent().get();
      writer.beginObject()
        .prop("taskId", task.getUuid())
        .prop("projectId", component.getUuid())
        .prop("projectKey", component.getKey().orElse(null))
        .prop("projectName", component.getName().orElse(null))
        .endObject()
        .close();
    }
  }

}
