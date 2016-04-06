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
package org.sonarqube.ws.client.measure;

import org.sonarqube.ws.WsMeasures.ComponentTreeWsResponse;
import org.sonarqube.ws.WsMeasures.ComponentWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.measure.MeasuresWsParameters.ACTION_COMPONENT;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.ACTION_COMPONENT_TREE;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.CONTROLLER_MEASURES;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_BASE_COMPONENT_ID;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_BASE_COMPONENT_KEY;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_COMPONENT_KEY;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_DEVELOPER_ID;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_DEVELOPER_KEY;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_KEYS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_SORT;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_STRATEGY;

public class MeasuresService extends BaseService {
  public MeasuresService(WsConnector wsConnector) {
    super(wsConnector, CONTROLLER_MEASURES);
  }

  public ComponentTreeWsResponse componentTree(ComponentTreeWsRequest request) {
    GetRequest getRequest = new GetRequest(path(ACTION_COMPONENT_TREE))
      .setParam(PARAM_BASE_COMPONENT_ID, request.getBaseComponentId())
      .setParam(PARAM_BASE_COMPONENT_KEY, request.getBaseComponentKey())
      .setParam(PARAM_STRATEGY, request.getStrategy())
      .setParam(PARAM_QUALIFIERS, inlineMultipleParamValue(request.getQualifiers()))
      .setParam(PARAM_METRIC_KEYS, inlineMultipleParamValue(request.getMetricKeys()))
      .setParam(PARAM_ADDITIONAL_FIELDS, inlineMultipleParamValue(request.getAdditionalFields()))
      .setParam(PARAM_DEVELOPER_ID, request.getDeveloperId())
      .setParam(PARAM_DEVELOPER_KEY, request.getDeveloperKey())
      .setParam("q", request.getQuery())
      .setParam("p", request.getPage())
      .setParam("ps", request.getPageSize())
      .setParam("s", inlineMultipleParamValue(request.getSort()))
      .setParam("asc", request.getAsc())
      .setParam(PARAM_METRIC_SORT, request.getMetricSort());

    return call(getRequest, ComponentTreeWsResponse.parser());
  }

  public ComponentWsResponse component(ComponentWsRequest request) {
    GetRequest getRequest = new GetRequest(path(ACTION_COMPONENT))
      .setParam(PARAM_COMPONENT_ID, request.getComponentId())
      .setParam(PARAM_COMPONENT_KEY, request.getComponentKey())
      .setParam(PARAM_ADDITIONAL_FIELDS, inlineMultipleParamValue(request.getAdditionalFields()))
      .setParam(PARAM_METRIC_KEYS, inlineMultipleParamValue(request.getMetricKeys()))
      .setParam(PARAM_DEVELOPER_ID, request.getDeveloperId())
      .setParam(PARAM_DEVELOPER_KEY, request.getDeveloperKey());

    return call(getRequest, ComponentWsResponse.parser());
  }
}
