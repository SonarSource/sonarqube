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
package org.sonarqube.ws.client.updatecenter;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/updatecenter">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class UpdatecenterService extends BaseService {

  public UpdatecenterService(WsConnector wsConnector) {
    super(wsConnector, "api/updatecenter");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/updatecenter/installed_plugins">Further information about this action online (including a response example)</a>
   * @since 2.10
   * @deprecated since 6.3
   */
  @Deprecated
  public String installedPlugins(InstalledPluginsRequest request) {
    return call(
      new GetRequest(path("installed_plugins"))
        .setParam("format", request.getFormat())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/updatecenter/upload">Further information about this action online (including a response example)</a>
   * @since 6.0
   */
  public void upload(UploadRequest request) {
    call(
      new PostRequest(path("upload"))
        .setParam("file", request.getFile())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
