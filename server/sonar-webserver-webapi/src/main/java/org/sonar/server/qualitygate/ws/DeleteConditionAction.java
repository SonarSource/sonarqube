/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;

public class DeleteConditionAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final QualityGatesWsSupport wsSupport;

  public DeleteConditionAction(DbClient dbClient, QualityGatesWsSupport wsSupport) {
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

    wsSupport.createOrganizationParam(createCondition);
  }

  @Override
  public void handle(Request request, Response response) {
    long conditionId = request.mandatoryParamAsLong(PARAM_ID);
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      QualityGateConditionDto condition = wsSupport.getCondition(dbSession, conditionId);
      QGateWithOrgDto qualityGateDto = dbClient.qualityGateDao().selectByOrganizationAndId(dbSession, organization, condition.getQualityGateId());
      checkState(qualityGateDto != null, "Condition '%s' is linked to an unknown quality gate '%s'", conditionId, condition.getQualityGateId());
      wsSupport.checkCanEdit(qualityGateDto);

      dbClient.gateConditionDao().delete(condition, dbSession);
      dbSession.commit();
      response.noContent();
    }
  }

}
