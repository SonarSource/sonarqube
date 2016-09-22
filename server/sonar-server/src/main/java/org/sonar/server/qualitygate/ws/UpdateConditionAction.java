/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.server.qualitygate.QualityGateConditionsUpdater;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsQualityGates.UpdateConditionWsResponse;
import org.sonarqube.ws.client.qualitygate.UpdateConditionRequest;

import static org.sonar.core.permission.GlobalPermissions.QUALITY_GATE_ADMIN;
import static org.sonar.server.qualitygate.ws.QualityGatesWs.addConditionParams;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.ACTION_UPDATE_CONDITION;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ERROR;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_METRIC;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_OPERATOR;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PERIOD;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_WARNING;

public class UpdateConditionAction implements QualityGatesWsAction {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final QualityGateConditionsUpdater qualityGateConditionsUpdater;

  public UpdateConditionAction(UserSession userSession, DbClient dbClient, QualityGateConditionsUpdater qualityGateConditionsUpdater) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.qualityGateConditionsUpdater = qualityGateConditionsUpdater;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction createCondition = controller.createAction(ACTION_UPDATE_CONDITION)
      .setDescription("Update a condition attached to a quality gate. Require Administer Quality Gates permission")
      .setPost(true)
      .setSince("4.3")
      .setHandler(this);

    createCondition
      .createParam(PARAM_ID)
      .setDescription("Condition ID")
      .setRequired(true)
      .setExampleValue("10");

    addConditionParams(createCondition);
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkPermission(QUALITY_GATE_ADMIN);

    DbSession dbSession = dbClient.openSession(false);
    try {
      writeProtobuf(doHandle(toWsRequest(request), dbSession), request, response);
      dbSession.commit();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private UpdateConditionWsResponse doHandle(UpdateConditionRequest request, DbSession dbSession) {
    QualityGateConditionDto condition = qualityGateConditionsUpdater.updateCondition(dbSession, request.getConditionId(), request.getMetricKey(), request.getOperator(),
      request.getWarning(), request.getError(), request.getPeriod());
    UpdateConditionWsResponse.Builder response = UpdateConditionWsResponse.newBuilder()
      .setId(condition.getId())
      .setMetric(condition.getMetricKey())
      .setOp(condition.getOperator());
    String warning = condition.getWarningThreshold();
    if (warning != null) {
      response.setWarning(warning);
    }
    String error = condition.getErrorThreshold();
    if (error != null) {
      response.setError(error);
    }
    Integer period = condition.getPeriod();
    if (period != null) {
      response.setPeriod(period);
    }
    return response.build();
  }

  private static UpdateConditionRequest toWsRequest(Request request) {
    return UpdateConditionRequest.builder()
      .setConditionId(request.mandatoryParamAsInt(PARAM_ID))
      .setMetricKey(request.mandatoryParam(PARAM_METRIC))
      .setOperator(request.mandatoryParam(PARAM_OPERATOR))
      .setWarning(request.param(PARAM_WARNING))
      .setError(request.param(PARAM_ERROR))
      .setPeriod(request.paramAsInt(PARAM_PERIOD))
      .build();
  }

}
