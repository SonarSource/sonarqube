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
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.QualityGateUpdater;
import org.sonar.server.user.UserSession;

import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.CreateAction.NAME_MAXIMUM_LENGTH;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_SOURCE_NAME;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Qualitygates.QualityGate.newBuilder;

public class CopyAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final QualityGateUpdater qualityGateUpdater;
  private final QualityGatesWsSupport wsSupport;
  private final Logger logger = Loggers.get(CopyAction.class);

  public CopyAction(DbClient dbClient, UserSession userSession, QualityGateUpdater qualityGateUpdater,
    QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.qualityGateUpdater = qualityGateUpdater;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("copy")
      .setDescription("Copy a Quality Gate.<br>" +
        "'sourceName' must be provided. Requires the 'Administer Quality Gates' permission.")
      .setPost(true)
      .setChangelog(
        new Change("10.0", "Field 'id' in the response is deprecated"),
        new Change("10.0", "Parameter 'id' is removed. Use 'sourceName' instead."),
        new Change("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'sourceName' instead."),
        new Change("8.4", "Parameter 'sourceName' added"))
      .setSince("4.3")
      .setHandler(this);

    action.createParam(PARAM_SOURCE_NAME)
      .setDescription("The name of the quality gate to copy")
      .setRequired(true)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setSince("8.4")
      .setExampleValue("My Quality Gate");

    action.createParam(PARAM_NAME)
      .setDescription("The name of the quality gate to create")
      .setRequired(true)
      .setExampleValue("My New Quality Gate");

    wsSupport.createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) {
    String sourceName = request.mandatoryParam(PARAM_SOURCE_NAME);

    String destinationName = request.mandatoryParam(PARAM_NAME);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);

      userSession.checkPermission(ADMINISTER_QUALITY_GATES, organization);
      QualityGateDto qualityGate = wsSupport.getByOrganizationAndName(dbSession, organization, sourceName);
      logger.info("Copy Quality Gate:: organization: {}, qGate: {}, user: {}", organization.getKey(),
          qualityGate.getName(), userSession.getLogin());
      QualityGateDto copy = qualityGateUpdater.copy(dbSession, organization, qualityGate, destinationName);
      logger.info("Copied Quality Gate:: organization: {}, qGate: {}, copiedQGateTo: {}, user: {}",
          organization.getKey(), qualityGate.getName(), destinationName, userSession.getLogin());
      dbSession.commit();

      writeProtobuf(newBuilder()
        .setId(copy.getUuid())
        .setName(copy.getName())
        .build(), request, response);
    }
  }
}
