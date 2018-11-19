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
import org.sonar.server.user.UserSession;

import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ID;

public class DeleteConditionAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final QualityGatesWsSupport wsSupport;

  public DeleteConditionAction(UserSession userSession, DbClient dbClient, QualityGatesWsSupport wsSupport) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction createCondition = controller.createAction("delete_condition")
      .setDescription("Delete a condition from a quality gate.<br>" +
        "Requires the 'Administer Quality Gates' permission.")
      .setPost(true)
      .setSince("4.3")
      .setHandler(this);

    createCondition
      .createParam(PARAM_ID)
      .setRequired(true)
      .setDescription("Condition ID")
      .setExampleValue("2");
  }

  @Override
  public void handle(Request request, Response response) {
    long conditionId = request.mandatoryParamAsLong(PARAM_ID);
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession);
      userSession.checkPermission(ADMINISTER_QUALITY_GATES, organization);
      dbClient.gateConditionDao().delete(wsSupport.getCondition(dbSession, conditionId), dbSession);
      dbSession.commit();
      response.noContent();
    }
  }

}
