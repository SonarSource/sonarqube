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
package org.sonarqube.ws.client.ce;

import org.sonarqube.ws.WsCe;
import org.sonarqube.ws.WsCe.ActivityResponse;
import org.sonarqube.ws.WsCe.ProjectResponse;
import org.sonarqube.ws.WsCe.TaskTypesWsResponse;
import org.sonarqube.ws.WsCe.WorkerCountResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.ce.CeWsParameters.ACTION_WORKER_COUNT;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_COMPONENT_KEY;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_MAX_EXECUTED_AT;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_MIN_SUBMITTED_AT;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_ONLY_CURRENTS;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_STATUS;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_TYPE;

/**
 * Maps web service {@code api/ce} (Compute Engine).
 */
public class CeService extends BaseService {

  public CeService(WsConnector wsConnector) {
    super(wsConnector, "api/ce");
  }

  public ActivityResponse activity(ActivityWsRequest request) {
    return call(
      new GetRequest(path("activity"))
        .setParam(PARAM_COMPONENT_ID, request.getComponentId())
        .setParam("q", request.getQuery())
        .setParam(PARAM_STATUS, inlineMultipleParamValue(request.getStatus()))
        .setParam(PARAM_TYPE, request.getType())
        .setParam(PARAM_MAX_EXECUTED_AT, request.getMaxExecutedAt())
        .setParam(PARAM_MIN_SUBMITTED_AT, request.getMinSubmittedAt())
        .setParam(PARAM_ONLY_CURRENTS, request.getOnlyCurrents())
        .setParam("p", request.getPage())
        .setParam("ps", request.getPageSize()),
      ActivityResponse.parser());
  }

  public TaskTypesWsResponse taskTypes() {
    return call(new GetRequest(path("task_types")), TaskTypesWsResponse.parser());
  }

  /**
   * Gets details of a Compute Engine task.
   *
   * @throws org.sonarqube.ws.client.HttpException if HTTP status code is not 2xx.
   * @since 5.5
   */
  public WsCe.TaskResponse task(String id) {
    return task(TaskWsRequest.newBuilder(id).build());
  }

  public WsCe.TaskResponse task(TaskWsRequest taskWsRequest) {
    GetRequest request = new GetRequest(path("task"))
      .setParam("id", taskWsRequest.getTaskUuid());
    if (!taskWsRequest.getAdditionalFields().isEmpty()) {
      request.setParam("additionalFields", inlineMultipleParamValue(taskWsRequest.getAdditionalFields()));
    }
    return call(request, WsCe.TaskResponse.parser());
  }

  public WsCe.ActivityStatusWsResponse activityStatus(ActivityStatusWsRequest request) {
    return call(
      new GetRequest(path("activity_status"))
        .setParam(PARAM_COMPONENT_ID, request.getComponentId())
        .setParam(PARAM_COMPONENT_KEY, request.getComponentKey()),
      WsCe.ActivityStatusWsResponse.parser());
  }

  public WorkerCountResponse workerCount() {
    return call(new GetRequest(path(ACTION_WORKER_COUNT)), WorkerCountResponse.parser());
  }

  public ProjectResponse component(String componentKey) {
    return call(
      new GetRequest(path("component"))
        .setParam(PARAM_COMPONENT_KEY, componentKey),
      ProjectResponse.parser());
  }

}
