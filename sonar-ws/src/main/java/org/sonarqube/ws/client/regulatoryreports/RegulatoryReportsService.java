/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonarqube.ws.client.regulatoryreports;

import java.io.InputStream;
import jakarta.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/regulatory_reports">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class RegulatoryReportsService extends BaseService {

  public RegulatoryReportsService(WsConnector wsConnector) {
    super(wsConnector, "api/regulatory_reports");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/regulatory_reports/download">Further information about this action online (including a response example)</a>
   * @since 9.5
   */
  public InputStream download(DownloadRequest request) {
    return call(
      new GetRequest(path("download"))
        .setParam("branch", request.getBranch())
        .setParam("project", request.getProject())
        .setMediaType(MediaTypes.JSON)
      ).contentStream();
  }
}
