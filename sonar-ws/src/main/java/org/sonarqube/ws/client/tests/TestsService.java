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
package org.sonarqube.ws.client.tests;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.Tests.CoveredFilesResponse;
import org.sonarqube.ws.Tests.ListResponse;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/tests">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class TestsService extends BaseService {

  public TestsService(WsConnector wsConnector) {
    super(wsConnector, "api/tests");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/tests/covered_files">Further information about this action online (including a response example)</a>
   * @since 4.4
   * @deprecated since 5.6
   */
  @Deprecated
  public CoveredFilesResponse coveredFiles(CoveredFilesRequest request) {
    return call(
      new GetRequest(path("covered_files"))
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("testId", request.getTestId()),
      CoveredFilesResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/tests/list">Further information about this action online (including a response example)</a>
   * @since 5.2
   * @deprecated since 5.6
   */
  @Deprecated
  public ListResponse list(ListRequest request) {
    return call(
      new GetRequest(path("list"))
        .setParam("branch", request.getBranch())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("sourceFileId", request.getSourceFileId())
        .setParam("sourceFileKey", request.getSourceFileKey())
        .setParam("sourceFileLineNumber", request.getSourceFileLineNumber())
        .setParam("testFileId", request.getTestFileId())
        .setParam("testFileKey", request.getTestFileKey())
        .setParam("testId", request.getTestId()),
      ListResponse.parser());
  }
}
