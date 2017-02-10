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
package org.sonarqube.ws.client.qualitygate;

import org.sonarqube.ws.WsQualityGates.CreateConditionWsResponse;
import org.sonarqube.ws.WsQualityGates.CreateWsResponse;
import org.sonarqube.ws.WsQualityGates.ProjectStatusWsResponse;
import org.sonarqube.ws.WsQualityGates.UpdateConditionWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.ACTION_CREATE_CONDITION;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.ACTION_PROJECT_STATUS;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.ACTION_SELECT;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.ACTION_UPDATE_CONDITION;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.CONTROLLER_QUALITY_GATES;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ANALYSIS_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ERROR;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_GATE_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_METRIC;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_OPERATOR;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PERIOD;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_WARNING;

public class QualityGatesService extends BaseService {

  public QualityGatesService(WsConnector wsConnector) {
    super(wsConnector, CONTROLLER_QUALITY_GATES);
  }

  public ProjectStatusWsResponse projectStatus(ProjectStatusWsRequest request) {
    return call(new GetRequest(path(ACTION_PROJECT_STATUS))
      .setParam(PARAM_ANALYSIS_ID, request.getAnalysisId())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey()),
      ProjectStatusWsResponse.parser());
  }

  public void associateProject(SelectWsRequest request) {
    call(new PostRequest(path(ACTION_SELECT))
      .setParam(PARAM_GATE_ID, request.getGateId())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey()));
  }

  public CreateWsResponse create(String name) {
    return call(new PostRequest(path(ACTION_CREATE))
      .setParam(PARAM_NAME, name),
      CreateWsResponse.parser());
  }

  public CreateConditionWsResponse createCondition(CreateConditionRequest request) {
    return call(new PostRequest(path(ACTION_CREATE_CONDITION))
      .setParam(PARAM_GATE_ID, request.getQualityGateId())
      .setParam(PARAM_METRIC, request.getMetricKey())
      .setParam(PARAM_OPERATOR, request.getOperator())
      .setParam(PARAM_WARNING, request.getWarning())
      .setParam(PARAM_ERROR, request.getError())
      .setParam(PARAM_PERIOD, request.getPeriod()),
      CreateConditionWsResponse.parser());
  }

  public UpdateConditionWsResponse updateCondition(UpdateConditionRequest request) {
    return call(new PostRequest(path(ACTION_UPDATE_CONDITION))
        .setParam(PARAM_ID, request.getConditionId())
        .setParam(PARAM_METRIC, request.getMetricKey())
        .setParam(PARAM_OPERATOR, request.getOperator())
        .setParam(PARAM_WARNING, request.getWarning())
        .setParam(PARAM_ERROR, request.getError())
        .setParam(PARAM_PERIOD, request.getPeriod()),
      UpdateConditionWsResponse.parser());
  }
}
