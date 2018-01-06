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
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.server.qualitygate.QualityGateConditionsUpdater;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonarqube.ws.Qualitygates.CreateConditionResponse;

import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.qualitygate.ws.QualityGatesWs.addConditionParams;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_CREATE_CONDITION;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ERROR;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_METRIC;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_OPERATOR;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PERIOD;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_WARNING;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CreateConditionAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final QualityGateConditionsUpdater qualityGateConditionsUpdater;
  private final QualityGateFinder qualityGateFinder;
  private final QualityGatesWsSupport wsSupport;

  public CreateConditionAction(DbClient dbClient, QualityGateConditionsUpdater qualityGateConditionsUpdater, QualityGateFinder qualityGateFinder, QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.qualityGateConditionsUpdater = qualityGateConditionsUpdater;
    this.qualityGateFinder = qualityGateFinder;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction createCondition = controller.createAction(ACTION_CREATE_CONDITION)
      .setPost(true)
      .setDescription("Add a new condition to a quality gate.<br>" +
        "Requires the 'Administer Quality Gates' permission.")
      .setSince("4.3")
      .setResponseExample(getClass().getResource("create-condition-example.json"))
      .setHandler(this);

    createCondition
      .createParam(PARAM_GATE_ID)
      .setRequired(true)
      .setDescription("ID of the quality gate")
      .setExampleValue("1");

    addConditionParams(createCondition);
    wsSupport.createOrganizationParam(createCondition);
  }

  @Override
  public void handle(Request request, Response response) {
    int gateId = request.mandatoryParamAsInt(PARAM_GATE_ID);
    String metric = request.mandatoryParam(PARAM_METRIC);
    String operator = request.mandatoryParam(PARAM_OPERATOR);
    String warning = request.param(PARAM_WARNING);
    String error = request.param(PARAM_ERROR);
    Integer period = request.paramAsInt(PARAM_PERIOD);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      QGateWithOrgDto qualityGate = qualityGateFinder.getByOrganizationAndId(dbSession, organization, gateId);
      wsSupport.checkCanEdit(qualityGate);
      QualityGateConditionDto condition = qualityGateConditionsUpdater.createCondition(dbSession, qualityGate, metric, operator, warning, error, period);
      CreateConditionResponse.Builder createConditionResponse = CreateConditionResponse.newBuilder()
        .setId(condition.getId())
        .setMetric(condition.getMetricKey())
        .setOp(condition.getOperator());
      setNullable(condition.getWarningThreshold(), createConditionResponse::setWarning);
      setNullable(condition.getErrorThreshold(), createConditionResponse::setError);
      setNullable(condition.getPeriod(), createConditionResponse::setPeriod);
      writeProtobuf(createConditionResponse.build(), request, response);
      dbSession.commit();
    }
  }

}
