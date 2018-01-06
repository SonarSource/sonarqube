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
package org.sonarqube.ws.client.batch;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.Batch.WsProjectResponse;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/batch">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class BatchService extends BaseService {

  public BatchService(WsConnector wsConnector) {
    super(wsConnector, "batch");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/batch/file">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public String file(FileRequest request) {
    return call(
      new GetRequest(path("file"))
        .setParam("name", request.getName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/batch/index">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public String index() {
    return call(
      new GetRequest(path("index"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/batch/issues">Further information about this action online (including a response example)</a>
   * @since 5.1
   */
  public String issues(IssuesRequest request) {
    return call(
      new GetRequest(path("issues"))
        .setParam("branch", request.getBranch())
        .setParam("key", request.getKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/batch/project">Further information about this action online (including a response example)</a>
   * @since 4.5
   */
  public WsProjectResponse project(ProjectRequest request) {
    return call(
      new GetRequest(path("project"))
        .setParam("branch", request.getBranch())
        .setParam("issues_mode", request.getIssuesMode())
        .setParam("key", request.getKey())
        .setParam("profile", request.getProfile()),
      WsProjectResponse.parser());
  }
}
