/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonarqube.ws.client.metrics;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/metrics">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class MetricsService extends BaseService {

  public MetricsService(WsConnector wsConnector) {
    super(wsConnector, "api/metrics");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/metrics/create">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void create(CreateRequest request) {
    call(
      new PostRequest(path("create"))
        .setParam("description", request.getDescription())
        .setParam("domain", request.getDomain())
        .setParam("key", request.getKey())
        .setParam("name", request.getName())
        .setParam("type", request.getType())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/metrics/delete">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void delete(DeleteRequest request) {
    call(
      new PostRequest(path("delete"))
        .setParam("ids", request.getIds())
        .setParam("keys", request.getKeys() == null ? null : request.getKeys().stream().collect(Collectors.joining(",")))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/metrics/domains">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String domains() {
    return call(
      new GetRequest(path("domains"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/metrics/search">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("f", request.getF() == null ? null : request.getF().stream().collect(Collectors.joining(",")))
        .setParam("isCustom", request.getIsCustom())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/metrics/types">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String types() {
    return call(
      new GetRequest(path("types"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/metrics/update">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void update(UpdateRequest request) {
    call(
      new PostRequest(path("update"))
        .setParam("description", request.getDescription())
        .setParam("domain", request.getDomain())
        .setParam("id", request.getId())
        .setParam("key", request.getKey())
        .setParam("name", request.getName())
        .setParam("type", request.getType())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
