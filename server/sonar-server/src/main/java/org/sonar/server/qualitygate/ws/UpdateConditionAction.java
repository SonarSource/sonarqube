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
import org.sonar.server.qualitygate.QualityGateConditionsUpdater;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Qualitygates.UpdateConditionWsResponse;

import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.QualityGatesWs.addConditionParams;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_UPDATE_CONDITION;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ERROR;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_METRIC;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_OPERATOR;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PERIOD;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_WARNING;

public class UpdateConditionAction implements QualityGatesWsAction {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final QualityGateConditionsUpdater qualityGateConditionsUpdater;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public UpdateConditionAction(UserSession userSession, DbClient dbClient, QualityGateConditionsUpdater qualityGateConditionsUpdater,
    DefaultOrganizationProvider defaultOrganizationProvider) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.qualityGateConditionsUpdater = qualityGateConditionsUpdater;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction createCondition = controller.createAction(ACTION_UPDATE_CONDITION)
      .setDescription("Update a condition attached to a quality gate.<br>" +
        "Requires the 'Administer Quality Gates' permission.")
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
    userSession.checkPermission(ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());

    int id = request.mandatoryParamAsInt(PARAM_ID);
    String metric = request.mandatoryParam(PARAM_METRIC);
    String operator = request.mandatoryParam(PARAM_OPERATOR);
    String warning = request.param(PARAM_WARNING);
    String error = request.param(PARAM_ERROR);
    Integer period = request.paramAsInt(PARAM_PERIOD);

    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityGateConditionDto condition = qualityGateConditionsUpdater.updateCondition(dbSession, id, metric, operator, warning, error, period);
      UpdateConditionWsResponse.Builder updateConditionWsResponse = UpdateConditionWsResponse.newBuilder()
        .setId(condition.getId())
        .setMetric(condition.getMetricKey())
        .setOp(condition.getOperator());
      setNullable(condition.getWarningThreshold(), updateConditionWsResponse::setWarning);
      setNullable(condition.getErrorThreshold(), updateConditionWsResponse::setError);
      setNullable(condition.getPeriod(), updateConditionWsResponse::setPeriod);
      writeProtobuf(updateConditionWsResponse.build(), request, response);
      dbSession.commit();
    }
  }
}
