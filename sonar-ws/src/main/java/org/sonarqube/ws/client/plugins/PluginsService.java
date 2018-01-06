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
package org.sonarqube.ws.client.plugins;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/plugins">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class PluginsService extends BaseService {

  public PluginsService(WsConnector wsConnector) {
    super(wsConnector, "api/plugins");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/plugins/available">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String available() {
    return call(
      new GetRequest(path("available"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/plugins/cancel_all">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void cancelAll() {
    call(
      new PostRequest(path("cancel_all"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/plugins/install">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void install(InstallRequest request) {
    call(
      new PostRequest(path("install"))
        .setParam("key", request.getKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/plugins/installed">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String installed(InstalledRequest request) {
    return call(
      new GetRequest(path("installed"))
        .setParam("f", request.getF() == null ? null : request.getF().stream().collect(Collectors.joining(",")))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/plugins/pending">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String pending() {
    return call(
      new GetRequest(path("pending"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/plugins/uninstall">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void uninstall(UninstallRequest request) {
    call(
      new PostRequest(path("uninstall"))
        .setParam("key", request.getKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/plugins/update">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void update(UpdateRequest request) {
    call(
      new PostRequest(path("update"))
        .setParam("key", request.getKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/plugins/updates">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String updates() {
    return call(
      new GetRequest(path("updates"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
