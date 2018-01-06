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
package org.sonarqube.ws.client.ce;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.Ce.ActivityResponse;
import org.sonarqube.ws.Ce.ActivityStatusWsResponse;
import org.sonarqube.ws.Ce.ComponentResponse;
import org.sonarqube.ws.Ce.SubmitResponse;
import org.sonarqube.ws.Ce.TaskResponse;
import org.sonarqube.ws.Ce.TaskTypesWsResponse;
import org.sonarqube.ws.Ce.WorkerCountResponse;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/ce">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class CeService extends BaseService {

  public CeService(WsConnector wsConnector) {
    super(wsConnector, "api/ce");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/ce/activity">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public ActivityResponse activity(ActivityRequest request) {
    return call(
      new GetRequest(path("activity"))
        .setParam("componentId", request.getComponentId())
        .setParam("componentQuery", request.getComponentQuery())
        .setParam("maxExecutedAt", request.getMaxExecutedAt())
        .setParam("minSubmittedAt", request.getMinSubmittedAt())
        .setParam("onlyCurrents", request.getOnlyCurrents())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("status", request.getStatus() == null ? null : request.getStatus().stream().collect(Collectors.joining(",")))
        .setParam("type", request.getType()),
      ActivityResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/ce/activity_status">Further information about this action online (including a response example)</a>
   * @since 5.5
   */
  public ActivityStatusWsResponse activityStatus(ActivityStatusRequest request) {
    return call(
      new GetRequest(path("activity_status"))
        .setParam("componentId", request.getComponentId())
        .setParam("componentKey", request.getComponentKey()),
      ActivityStatusWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/ce/cancel">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void cancel(CancelRequest request) {
    call(
      new PostRequest(path("cancel"))
        .setParam("id", request.getId())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/ce/cancel_all">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void cancelAll() {
    call(
      new PostRequest(path("cancel_all"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/ce/component">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public ComponentResponse component(ComponentRequest request) {
    return call(
      new GetRequest(path("component"))
        .setParam("component", request.getComponent())
        .setParam("componentId", request.getComponentId()),
      ComponentResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/ce/submit">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public SubmitResponse submit(SubmitRequest request) {
    return call(
      new PostRequest(path("submit"))
        .setParam("characteristic", request.getCharacteristic())
        .setParam("organization", request.getOrganization())
        .setParam("projectBranch", request.getProjectBranch())
        .setParam("projectKey", request.getProjectKey())
        .setParam("projectName", request.getProjectName())
        .setParam("report", request.getReport()),
      SubmitResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/ce/task">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public TaskResponse task(TaskRequest request) {
    return call(
      new GetRequest(path("task"))
        .setParam("additionalFields", request.getAdditionalFields() == null ? null : request.getAdditionalFields().stream().collect(Collectors.joining(",")))
        .setParam("id", request.getId()),
      TaskResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/ce/task_types">Further information about this action online (including a response example)</a>
   * @since 5.5
   */
  public TaskTypesWsResponse taskTypes() {
    return call(
      new GetRequest(path("task_types")),
      TaskTypesWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/ce/worker_count">Further information about this action online (including a response example)</a>
   * @since 6.5
   */
  public WorkerCountResponse workerCount() {
    return call(
      new GetRequest(path("worker_count")),
      WorkerCountResponse.parser());
  }
}
