/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.qualitygate.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;

import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class DeselectAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final QualityGatesWsSupport wsSupport;

  public DeselectAction(DbClient dbClient, QualityGatesWsSupport wsSupport) {
    this.wsSupport = wsSupport;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("deselect")
      .setDescription("Remove the association of a project from a quality gate.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer Quality Gates'</li>" +
        "<li>'Administer' rights on the project</li>" +
        "</ul>")
      .setPost(true)
      .setSince("4.3")
      .setHandler(this)
      .setChangelog(
        new Change("10.8", "Allow to change the Quality Gate of a project flagged as containing AI code."),
        new Change("10.7", "It is not possible anymore to change the Quality Gate of a project flagged as containing AI code."),
        new Change("6.6", "The parameter 'gateId' was removed"),
        new Change("8.3", "The parameter 'projectId' was removed"));

    action.createParam(PARAM_PROJECT_KEY)
      .setRequired(true)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setSince("6.1");
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = wsSupport.getProject(dbSession, request.mandatoryParam(PARAM_PROJECT_KEY));
      dissociateProject(dbSession, project);
      response.noContent();
    }
  }

  private void dissociateProject(DbSession dbSession, ProjectDto project) {
    wsSupport.checkCanAdminProject(project);
    dbClient.projectQgateAssociationDao().deleteByProjectUuid(dbSession, project.getUuid());
    dbSession.commit();
  }
}
