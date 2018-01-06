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
package org.sonarqube.ws.client.navigation;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/navigation">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class NavigationService extends BaseService {

  public NavigationService(WsConnector wsConnector) {
    super(wsConnector, "api/navigation");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/navigation/component">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String component(ComponentRequest request) {
    return call(
      new GetRequest(path("component"))
        .setParam("branch", request.getBranch())
        .setParam("component", request.getComponent())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/navigation/global">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String global() {
    return call(
      new GetRequest(path("global"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/navigation/organization">Further information about this action online (including a response example)</a>
   * @since 6.3
   */
  public String organization(OrganizationRequest request) {
    return call(
      new GetRequest(path("organization"))
        .setParam("organization", request.getOrganization())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/navigation/settings">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String settings() {
    return call(
      new GetRequest(path("settings"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
