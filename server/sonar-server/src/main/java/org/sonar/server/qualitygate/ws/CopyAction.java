/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.qualitygate.QualityGateUpdater;
import org.sonar.server.user.UserSession;

import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.QualityGatesWs.parseId;
import static org.sonar.server.qualitygate.ws.QualityGatesWs.writeQualityGate;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;
import static org.sonar.server.ws.WsUtils.checkFound;

public class CopyAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final DefaultOrganizationProvider organizationProvider;
  private final QualityGateUpdater qualityGateUpdater;

  public CopyAction(DbClient dbClient, UserSession userSession, DefaultOrganizationProvider organizationProvider, QualityGateUpdater qualityGateUpdater) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.organizationProvider = organizationProvider;
    this.qualityGateUpdater = qualityGateUpdater;
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
  }

  @Override
  public void handle(Request request, Response response) {
    Long id = parseId(request, PARAM_ID);
    String destinationName = request.mandatoryParam(PARAM_NAME);

    userSession.checkPermission(ADMINISTER_QUALITY_GATES, organizationProvider.get().getUuid());

    QualityGateDto result;
    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityGateDto qualityGateDto = dbClient.qualityGateDao().selectById(dbSession, id);
      checkFound(qualityGateDto, "No quality gate has been found for id %s", (long) id);
      result = qualityGateUpdater.copy(dbSession, qualityGateDto, destinationName);
      dbSession.commit();
    }

    writeQualityGate(result, response.newJsonWriter()).close();
  }

}
