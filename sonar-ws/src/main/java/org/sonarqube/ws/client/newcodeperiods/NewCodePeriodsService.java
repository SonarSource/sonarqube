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
package org.sonarqube.ws.client.newcodeperiods;

import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.NewCodePeriods;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/new_code_periods">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class NewCodePeriodsService extends BaseService {

  public NewCodePeriodsService(WsConnector wsConnector) {
    super(wsConnector, "api/new_code_periods");
  }

  /**
   * This is part of the internal API.
   * This is a POST request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/new_code_periods/set">Further information about this action online (including a response example)</a>
   * @since 8.0
   */
  public void set(SetRequest request) {
    call(
      new PostRequest(path("set"))
        .setParam("branch", request.getBranch())
        .setParam("project", request.getProject())
        .setParam("type", request.getType())
        .setParam("value", request.getValue())
        .setMediaType(MediaTypes.JSON)
    ).content();
  }

  /**
   * This is part of the internal API.
   * This is a GET request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/new_code_periods/show">Further information about this action online (including a response example)</a>
   * @since 8.0
   */
  public NewCodePeriods.ShowWSResponse show(ShowRequest request) {
    return call(
      new GetRequest(path("show"))
        .setParam("branch", request.getBranch())
        .setParam("project", request.getProject())
        .setMediaType(MediaTypes.JSON),
      NewCodePeriods.ShowWSResponse.parser());
  }

  /**
   * This is part of the internal API.
   * This is a POST request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/new_code_periods/unset">Further information about this action online (including a response example)</a>
   * @since 8.0
   */
  public void unset(UnsetRequest request) {
    call(
      new PostRequest(path("unset"))
        .setParam("branch", request.getBranch())
        .setParam("project", request.getProject())
        .setMediaType(MediaTypes.JSON)
    ).content();
  }
}
