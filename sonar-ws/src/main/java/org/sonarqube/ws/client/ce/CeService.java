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

package org.sonarqube.ws.client.ce;

import org.sonarqube.ws.WsCe.ActivityResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_COMPONENT_QUERY;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_MAX_EXECUTED_AT;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_MIN_SUBMITTED_AT;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_ONLY_CURRENTS;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_STATUS;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_TYPE;

public class CeService extends BaseService {

  public CeService(WsConnector wsConnector) {
    super(wsConnector, "api/ce");
  }

  public ActivityResponse activity(ActivityWsRequest request) {
    return call(
      new GetRequest(path("activity"))
        .setParam(PARAM_COMPONENT_ID, request.getComponentId())
        .setParam(PARAM_COMPONENT_QUERY, request.getComponentQuery())
        .setParam(PARAM_STATUS, inlineMultipleParamValue(request.getStatus()))
        .setParam(PARAM_TYPE, request.getType())
        .setParam(PARAM_MAX_EXECUTED_AT, request.getMaxExecutedAt())
        .setParam(PARAM_MIN_SUBMITTED_AT, request.getMinSubmittedAt())
        .setParam(PARAM_ONLY_CURRENTS, request.getOnlyCurrents())
        .setParam("p", request.getPage())
        .setParam("ps", request.getPageSize()),
      ActivityResponse.parser());
  }

}
