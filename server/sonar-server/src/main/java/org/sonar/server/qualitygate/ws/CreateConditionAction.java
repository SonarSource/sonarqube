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
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.qualitygate.QualityGateConditionsUpdater;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsQualityGates.CreateConditionWsResponse;
import org.sonarqube.ws.client.qualitygate.CreateConditionRequest;

import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.qualitygate.ws.QualityGatesWs.addConditionParams;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.ACTION_CREATE_CONDITION;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ERROR;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_GATE_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_METRIC;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_OPERATOR;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PERIOD;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_WARNING;

public class CreateConditionAction implements QualityGatesWsAction {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final QualityGateConditionsUpdater qualityGateConditionsUpdater;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public CreateConditionAction(UserSession userSession, DbClient dbClient, QualityGateConditionsUpdater qualityGateConditionsUpdater,
    DefaultOrganizationProvider defaultOrganizationProvider) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.qualityGateConditionsUpdater = qualityGateConditionsUpdater;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction createCondition = controller.createAction(ACTION_CREATE_CONDITION)
      .setDescription("Add a new condition to a quality gate. Require Administer Quality Gates permission")
      .setPost(true)
      .setSince("4.3")
      .setHandler(this);

    createCondition
      .createParam(PARAM_GATE_ID)
      .setDescription("ID of the quality gate")
      .setRequired(true)
      .setExampleValue("1");

    addConditionParams(createCondition);
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkPermission(OrganizationPermission.ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());

    try (DbSession dbSession = dbClient.openSession(false)) {
      writeProtobuf(doHandle(toWsRequest(request), dbSession), request, response);
      dbSession.commit();
    }
  }

  private CreateConditionWsResponse doHandle(CreateConditionRequest request, DbSession dbSession) {
    QualityGateConditionDto condition = qualityGateConditionsUpdater.createCondition(dbSession,
      request.getQualityGateId(),
      request.getMetricKey(),
      request.getOperator(),
      request.getWarning(),
      request.getError(),
      request.getPeriod());

    CreateConditionWsResponse.Builder response = CreateConditionWsResponse.newBuilder()
      .setId(condition.getId())
      .setMetric(condition.getMetricKey())
      .setOp(condition.getOperator());
    setNullable(condition.getWarningThreshold(), response::setWarning);
    setNullable(condition.getErrorThreshold(), response::setError);
    setNullable(condition.getPeriod(), response::setPeriod);
    return response.build();
  }

  private static CreateConditionRequest toWsRequest(Request request) {
    return CreateConditionRequest.builder()
      .setQualityGateId(request.mandatoryParamAsInt(PARAM_GATE_ID))
      .setMetricKey(request.mandatoryParam(PARAM_METRIC))
      .setOperator(request.mandatoryParam(PARAM_OPERATOR))
      .setWarning(request.param(PARAM_WARNING))
      .setError(request.param(PARAM_ERROR))
      .setPeriod(request.paramAsInt(PARAM_PERIOD))
      .build();
  }

}
