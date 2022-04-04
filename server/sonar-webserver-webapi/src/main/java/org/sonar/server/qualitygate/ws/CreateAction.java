/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.QualityGateUpdater;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Qualitygates.CreateResponse;

import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_CREATE;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CreateAction implements QualityGatesWsAction {

  public static final int NAME_MAXIMUM_LENGTH = 100;

  private final DbClient dbClient;
  private final UserSession userSession;
  private final QualityGateUpdater qualityGateUpdater;

  public CreateAction(DbClient dbClient, UserSession userSession, QualityGateUpdater qualityGateUpdater) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.qualityGateUpdater = qualityGateUpdater;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_CREATE)
      .setPost(true)
      .setDescription("Create a Quality Gate.<br>" +
        "Requires the 'Administer Quality Gates' permission.")
      .setSince("4.3")
      .setChangelog(
        new Change("8.4", "Field 'id' in the response is deprecated. Format changes from integer to string."))
      .setResponseExample(getClass().getResource("create-example.json"))
      .setHandler(this);

    action.createParam(PARAM_NAME)
      .setRequired(true)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setDescription("The name of the quality gate to create")
      .setExampleValue("My Quality Gate");
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

      String name = request.mandatoryParam(PARAM_NAME);

      QualityGateDto newQualityGate = qualityGateUpdater.create(dbSession, name);
      CreateResponse.Builder createResponse = CreateResponse.newBuilder()
        .setId(newQualityGate.getUuid())
        .setName(newQualityGate.getName());
      dbSession.commit();
      writeProtobuf(createResponse.build(), request, response);
    }
  }
}
