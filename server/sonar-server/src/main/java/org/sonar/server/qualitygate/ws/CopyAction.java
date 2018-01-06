/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.qualitygate.QualityGateUpdater;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.QualityGatesWs.parseId;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Qualitygates.QualityGate.newBuilder;

public class CopyAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final QualityGateUpdater qualityGateUpdater;
  private final QualityGateFinder qualityGateFinder;
  private final QualityGatesWsSupport wsSupport;

  public CopyAction(DbClient dbClient, UserSession userSession, QualityGateUpdater qualityGateUpdater,
    QualityGateFinder qualityGateFinder, QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.qualityGateUpdater = qualityGateUpdater;
    this.qualityGateFinder = qualityGateFinder;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("copy")
      .setDescription("Copy a Quality Gate.<br>" +
        "Requires the 'Administer Quality Gates' permission.")
      .setPost(true)
      .setSince("4.3")
      .setHandler(this);

    action.createParam(PARAM_ID)
      .setDescription("The ID of the source quality gate")
      .setRequired(true)
      .setExampleValue("1");

    action.createParam(PARAM_NAME)
      .setDescription("The name of the quality gate to create")
      .setRequired(true)
      .setExampleValue("My Quality Gate");

    wsSupport.createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) {
    Long id = parseId(request, PARAM_ID);
    String destinationName = request.mandatoryParam(PARAM_NAME);

    checkArgument(!destinationName.isEmpty(), "The 'name' parameter is empty");

    try (DbSession dbSession = dbClient.openSession(false)) {

      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      userSession.checkPermission(ADMINISTER_QUALITY_GATES, organization);
      QualityGateDto qualityGate = qualityGateFinder.getByOrganizationAndId(dbSession, organization, id);
      QualityGateDto copy = qualityGateUpdater.copy(dbSession, organization, qualityGate, destinationName);
      dbSession.commit();

      writeProtobuf(newBuilder()
        .setId(copy.getId())
        .setName(copy.getName())
        .build(), request, response);
    }
  }
}
