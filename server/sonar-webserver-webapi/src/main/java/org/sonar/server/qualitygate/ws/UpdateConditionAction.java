/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.QualityGateConditionsUpdater;
import org.sonarqube.ws.Qualitygates.UpdateConditionResponse;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.qualitygate.ws.QualityGatesWs.addConditionParams;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_UPDATE_CONDITION;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ERROR;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_METRIC;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_OPERATOR;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class UpdateConditionAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final QualityGateConditionsUpdater qualityGateConditionsUpdater;
  private final QualityGatesWsSupport wsSupport;

  public UpdateConditionAction(DbClient dbClient, QualityGateConditionsUpdater qualityGateConditionsUpdater, QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.qualityGateConditionsUpdater = qualityGateConditionsUpdater;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction createCondition = controller.createAction(ACTION_UPDATE_CONDITION)
      .setDescription("Update a condition attached to a quality gate.<br>" +
        "Requires the 'Administer Quality Gates' permission.")
      .setPost(true)
      .setSince("4.3")
      .setChangelog(
        new Change("8.4", "Parameter 'id' format changes from integer to string. "),
        new Change("7.6", "Removed optional 'warning' and 'period' parameters"),
        new Change("7.6", "Made 'error' parameter mandatory"),
        new Change("7.6", "Reduced the possible values of 'op' parameter to LT and GT"))
      .setHandler(this);

    createCondition
      .createParam(PARAM_ID)
      .setDescription("Condition ID")
      .setRequired(true)
      .setExampleValue(UUID_EXAMPLE_01);

    addConditionParams(createCondition);
    wsSupport.createOrganizationParam(createCondition);
  }

  @Override
  public void handle(Request request, Response response) {
    String id = request.mandatoryParam(PARAM_ID);
    String metric = request.mandatoryParam(PARAM_METRIC);
    String operator = request.mandatoryParam(PARAM_OPERATOR);
    String error = request.mandatoryParam(PARAM_ERROR);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      QualityGateConditionDto condition = wsSupport.getCondition(dbSession, id);
      QualityGateDto qualityGateDto = dbClient.qualityGateDao().selectByOrganizationAndUuid(dbSession, organization, condition.getQualityGateUuid());
      checkState(qualityGateDto != null, "Condition '%s' is linked to an unknown quality gate '%s'", id, condition.getQualityGateUuid());
      wsSupport.checkCanLimitedEdit(dbSession, qualityGateDto);
      QualityGateConditionDto updatedCondition = qualityGateConditionsUpdater.updateCondition(dbSession, condition, metric, operator, error);
      UpdateConditionResponse.Builder updateConditionResponse = UpdateConditionResponse.newBuilder()
        .setId(updatedCondition.getUuid())
        .setMetric(updatedCondition.getMetricKey())
        .setError(updatedCondition.getErrorThreshold())
        .setOp(updatedCondition.getOperator());
      writeProtobuf(updateConditionResponse.build(), request, response);
      dbSession.commit();
    }
  }
}
