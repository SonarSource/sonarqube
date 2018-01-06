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
package org.sonarqube.ws.client.webservices;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/webservices">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class WebservicesService extends BaseService {

  public WebservicesService(WsConnector wsConnector) {
    super(wsConnector, "api/webservices");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/webservices/list">Further information about this action online (including a response example)</a>
   * @since 4.2
   */
  public String list(ListRequest request) {
    return call(
      new GetRequest(path("list"))
        .setParam("include_internals", request.getIncludeInternals())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/webservices/response_example">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public String responseExample(ResponseExampleRequest request) {
    return call(
      new GetRequest(path("response_example"))
        .setParam("action", request.getAction())
        .setParam("controller", request.getController())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
