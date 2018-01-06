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
package org.sonarqube.ws.client.measures;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.Measures.ComponentWsResponse;
import org.sonarqube.ws.Measures.ComponentTreeWsResponse;
import org.sonarqube.ws.Measures.SearchWsResponse;
import org.sonarqube.ws.Measures.SearchHistoryResponse;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/measures">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class MeasuresService extends BaseService {

  public MeasuresService(WsConnector wsConnector) {
    super(wsConnector, "api/measures");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/measures/component">Further information about this action online (including a response example)</a>
   * @since 5.4
   */
  public ComponentWsResponse component(ComponentRequest request) {
    return call(
      new GetRequest(path("component"))
        .setParam("additionalFields", request.getAdditionalFields() == null ? null : request.getAdditionalFields().stream().collect(Collectors.joining(",")))
        .setParam("branch", request.getBranch())
        .setParam("component", request.getComponent())
        .setParam("componentId", request.getComponentId())
        .setParam("developerId", request.getDeveloperId())
        .setParam("developerKey", request.getDeveloperKey())
        .setParam("metricKeys", request.getMetricKeys() == null ? null : request.getMetricKeys().stream().collect(Collectors.joining(","))),
      ComponentWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/measures/component_tree">Further information about this action online (including a response example)</a>
   * @since 5.4
   */
  public ComponentTreeWsResponse componentTree(ComponentTreeRequest request) {
    return call(
      new GetRequest(path("component_tree"))
        .setParam("additionalFields", request.getAdditionalFields() == null ? null : request.getAdditionalFields().stream().collect(Collectors.joining(",")))
        .setParam("asc", request.getAsc())
        .setParam("baseComponentId", request.getBaseComponentId())
        .setParam("branch", request.getBranch())
        .setParam("component", request.getComponent())
        .setParam("developerId", request.getDeveloperId())
        .setParam("developerKey", request.getDeveloperKey())
        .setParam("metricKeys", request.getMetricKeys() == null ? null : request.getMetricKeys().stream().collect(Collectors.joining(",")))
        .setParam("metricPeriodSort", request.getMetricPeriodSort())
        .setParam("metricSort", request.getMetricSort())
        .setParam("metricSortFilter", request.getMetricSortFilter())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("qualifiers", request.getQualifiers() == null ? null : request.getQualifiers().stream().collect(Collectors.joining(",")))
        .setParam("s", request.getS() == null ? null : request.getS().stream().collect(Collectors.joining(",")))
        .setParam("strategy", request.getStrategy()),
      ComponentTreeWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/measures/search">Further information about this action online (including a response example)</a>
   * @since 6.2
   */
  public SearchWsResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("metricKeys", request.getMetricKeys() == null ? null : request.getMetricKeys().stream().collect(Collectors.joining(",")))
        .setParam("projectKeys", request.getProjectKeys() == null ? null : request.getProjectKeys().stream().collect(Collectors.joining(","))),
      SearchWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/measures/search_history">Further information about this action online (including a response example)</a>
   * @since 6.3
   */
  public SearchHistoryResponse searchHistory(SearchHistoryRequest request) {
    return call(
      new GetRequest(path("search_history"))
        .setParam("branch", request.getBranch())
        .setParam("component", request.getComponent())
        .setParam("from", request.getFrom())
        .setParam("metrics", request.getMetrics() == null ? null : request.getMetrics().stream().collect(Collectors.joining(",")))
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("to", request.getTo()),
      SearchHistoryResponse.parser());
  }
}
