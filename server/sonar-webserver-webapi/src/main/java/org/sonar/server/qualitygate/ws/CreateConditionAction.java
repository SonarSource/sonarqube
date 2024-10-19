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
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.QualityGateConditionsUpdater;
import org.sonarqube.ws.Qualitygates.CreateConditionResponse;

import static org.sonar.server.qualitygate.ws.QualityGatesWs.addConditionParams;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_CREATE_CONDITION;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ERROR;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_METRIC;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_OPERATOR;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CreateConditionAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final QualityGateConditionsUpdater qualityGateConditionsUpdater;
  private final QualityGatesWsSupport wsSupport;

  public CreateConditionAction(DbClient dbClient, QualityGateConditionsUpdater qualityGateConditionsUpdater, QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.qualityGateConditionsUpdater = qualityGateConditionsUpdater;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction createCondition = controller.createAction(ACTION_CREATE_CONDITION)
      .setPost(true)
      .setDescription("Add a new condition to a quality gate.<br>" +
        "Parameter 'gateName' must be provided. Requires the 'Administer Quality Gates' permission.")
      .setSince("4.3")
      .setResponseExample(getClass().getResource("create-condition-example.json"))
      .setChangelog(
        new Change("7.6", "Removed optional 'warning' and 'period' parameters"),
        new Change("7.6", "Made 'error' parameter mandatory"),
        new Change("7.6", "Reduced the possible values of 'op' parameter to LT and GT"),
        new Change("8.4", "Parameter 'gateName' added"),
        new Change("8.4", "Parameter 'gateId' is deprecated. Use 'gateName' instead."),
        new Change("10.0", "Parameter 'gateId' is removed. Use 'gateName' instead."))
      .setHandler(this);

    createCondition
      .createParam(PARAM_GATE_NAME)
      .setRequired(true)
      .setDescription("Name of the quality gate")
      .setExampleValue("SonarSource way");

    addConditionParams(createCondition);
    wsSupport.createOrganizationParam(createCondition);
  }

  @Override
  public void handle(Request request, Response response) {
    String gateName = request.mandatoryParam(PARAM_GATE_NAME);
    String metric = request.mandatoryParam(PARAM_METRIC);
    String operator = request.mandatoryParam(PARAM_OPERATOR);
    String error = request.mandatoryParam(PARAM_ERROR);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      QualityGateDto qualityGate = wsSupport.getByOrganizationAndName(dbSession, organization, gateName);

      wsSupport.checkCanLimitedEdit(dbSession, qualityGate);
      QualityGateConditionDto condition = qualityGateConditionsUpdater.createCondition(dbSession, qualityGate, metric, operator, error);
      CreateConditionResponse.Builder createConditionResponse = CreateConditionResponse.newBuilder()
        .setId(condition.getUuid())
        .setMetric(condition.getMetricKey())
        .setError(condition.getErrorThreshold())
        .setOp(condition.getOperator());
      writeProtobuf(createConditionResponse.build(), request, response);
      dbSession.commit();
    }
  }

}
