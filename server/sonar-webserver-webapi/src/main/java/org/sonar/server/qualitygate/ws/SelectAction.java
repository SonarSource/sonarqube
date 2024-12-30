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
package org.sonar.server.qualitygate.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateDto;

import static org.sonar.server.qualitygate.ws.CreateAction.NAME_MAXIMUM_LENGTH;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_SELECT;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class SelectAction implements QualityGatesWsAction {
  private final DbClient dbClient;
  private final QualityGatesWsSupport wsSupport;

  public SelectAction(DbClient dbClient, QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_SELECT)
      .setDescription("Associate a project to a quality gate.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Gates'</li>" +
        "  <li>'Administer' right on the specified project</li>" +
        "</ul>")
      .setPost(true)
      .setSince("4.3")
      .setHandler(this)
      .setChangelog(
        new Change("10.8", "Allow to change the Quality Gate of a project flagged as containing AI code."),
        new Change("10.7", "It is not possible anymore to change the Quality Gate of a project flagged as containing AI code."),
        new Change("10.0", "Parameter 'gateId' is removed. Use 'gateName' instead."),
        new Change("8.4", "Parameter 'gateName' added"),
        new Change("8.4", "Parameter 'gateId' is deprecated. Format changes from integer to string. Use 'gateName' instead."),
        new Change("8.3", "The parameter 'projectId' was removed"));

    action.createParam(PARAM_GATE_NAME)
      .setRequired(true)
      .setDescription("Name of the quality gate")
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setSince("8.4")
      .setExampleValue("SonarSource way");

    action.createParam(PARAM_PROJECT_KEY)
      .setRequired(true)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setSince("6.1");

    wsSupport.createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) {
    String gateName = request.mandatoryParam(PARAM_GATE_NAME);
    String projectKey = request.mandatoryParam(PARAM_PROJECT_KEY);


    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      QualityGateDto qualityGate;
      qualityGate = wsSupport.getByOrganizationAndName(dbSession, organization, gateName);
      ProjectDto project = wsSupport.getProject(dbSession, projectKey);
      wsSupport.checkCanAdminProject(organization, project);

      QualityGateDto currentQualityGate = dbClient.qualityGateDao().selectByProjectUuid(dbSession, project.getUuid());
      if (currentQualityGate == null) {
        // project uses the default profile
        dbClient.projectQgateAssociationDao()
          .insertProjectQGateAssociation(dbSession, project.getUuid(), qualityGate.getUuid());
        dbSession.commit();
      } else if (!qualityGate.getUuid().equals(currentQualityGate.getUuid())) {
        dbClient.projectQgateAssociationDao()
          .updateProjectQGateAssociation(dbSession, project.getUuid(), qualityGate.getUuid());
        dbSession.commit();
      }
    }
    response.noContent();
  }
}
