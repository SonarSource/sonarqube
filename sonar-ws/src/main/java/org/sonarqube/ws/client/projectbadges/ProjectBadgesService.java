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
package org.sonarqube.ws.client.projectbadges;

import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_badges">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class ProjectBadgesService extends BaseService {

  public ProjectBadgesService(WsConnector wsConnector) {
    super(wsConnector, "api/project_badges");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_badges/measure">Further information about this action online (including a response example)</a>
   * @since 7.1
   */
  public String measure(MeasureRequest request) {
    return call(
      new GetRequest(path("measure"))
        .setParam("branch", request.getBranch())
        .setParam("metric", request.getMetric())
        .setParam("project", request.getProject())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_badges/quality_gate">Further information about this action online (including a response example)</a>
   * @since 7.1
   */
  public String qualityGate(QualityGateRequest request) {
    return call(
      new GetRequest(path("quality_gate"))
        .setParam("branch", request.getBranch())
        .setParam("project", request.getProject())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
