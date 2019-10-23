/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateDto;

import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_SELECT;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PROJECT_ID;
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
          "The '%s' or '%s' must be provided.<br>" +
          "Project id as a numeric value is deprecated since 6.1. Please use the id similar to '%s'.<br>" +
          "Requires one of the following permissions:" +
          "<ul>" +
          "  <li>'Administer Quality Gates'</li>" +
          "  <li>'Administer' right on the specified project</li>" +
          "</ul>",
        PARAM_PROJECT_ID, PARAM_PROJECT_KEY,
        Uuids.UUID_EXAMPLE_02)
      .setPost(true)
      .setSince("4.3")
      .setHandler(this);

    action.createParam(PARAM_GATE_ID)
      .setDescription("Quality gate id")
      .setRequired(true)
      .setExampleValue("1");

    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project id. Project id as an numeric value is deprecated since 6.1")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setSince("6.1");

    wsSupport.createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) {
    long gateId = request.mandatoryParamAsLong(PARAM_GATE_ID);
    String projectKey = request.param(PARAM_PROJECT_KEY);
    String projectId = request.param(PARAM_PROJECT_ID);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      QGateWithOrgDto qualityGate = wsSupport.getByOrganizationAndId(dbSession, organization, gateId);
      ProjectDto project = wsSupport.getProject(dbSession, organization, projectKey, projectId);
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
