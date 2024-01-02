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
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.QualityGateUpdater;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.CreateAction.NAME_MAXIMUM_LENGTH;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_SOURCE_NAME;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Qualitygates.QualityGate.newBuilder;

public class CopyAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final QualityGateUpdater qualityGateUpdater;
  private final QualityGatesWsSupport wsSupport;

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
        "Either 'sourceName' or 'id' must be provided. Requires the 'Administer Quality Gates' permission.")
      .setPost(true)
      .setChangelog(
        new Change("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'sourceName' instead."),
        new Change("8.4", "Parameter 'sourceName' added"))
      .setSince("4.3")
      .setHandler(this);

    action.createParam(PARAM_ID)
      .setDescription("The ID of the source quality gate. This parameter is deprecated. Use 'sourceName' instead.")
      .setRequired(false)
      .setDeprecatedSince("8.4")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_SOURCE_NAME)
      .setDescription("The name of the quality gate to copy")
      .setRequired(false)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setSince("8.4")
      .setExampleValue("My Quality Gate");

    action.createParam(PARAM_NAME)
      .setDescription("The name of the quality gate to create")
      .setRequired(true)
      .setExampleValue("My New Quality Gate");
  }

  @Override
  public void handle(Request request, Response response) {
    String uuid = request.param(PARAM_ID);
    String sourceName = request.param(PARAM_SOURCE_NAME);
    checkArgument(sourceName != null ^ uuid != null, "Either 'id' or 'sourceName' must be provided, and not both");

    String destinationName = request.mandatoryParam(PARAM_NAME);

    try (DbSession dbSession = dbClient.openSession(false)) {

      userSession.checkPermission(ADMINISTER_QUALITY_GATES);
      QualityGateDto qualityGate;
      if (uuid != null) {
        qualityGate = wsSupport.getByUuid(dbSession, uuid);
      } else {
        qualityGate = wsSupport.getByName(dbSession, sourceName);
      }
      QualityGateDto copy = qualityGateUpdater.copy(dbSession, qualityGate, destinationName);
      dbSession.commit();

      writeProtobuf(newBuilder()
        .setId(copy.getUuid())
        .setName(copy.getName())
        .build(), request, response);
    }
  }
}
