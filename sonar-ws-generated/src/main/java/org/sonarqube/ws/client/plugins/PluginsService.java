/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
 * Manage the plugins on the server, including installing, uninstalling, and upgrading.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/plugins">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class PluginsService extends BaseService {

  public PluginsService(WsConnector wsConnector) {
    super(wsConnector, "api/plugins");
  }

  /**
   * Get the list of all the plugins available for installation on the SonarQube instance, sorted by plugin name.<br/>Plugin information is retrieved from Update Center. Date and time at which Update Center was last refreshed is provided in the response.<br/>Update status values are: <ul><li>COMPATIBLE: plugin is compatible with current SonarQube instance.</li><li>INCOMPATIBLE: plugin is not compatible with current SonarQube instance.</li><li>REQUIRES_SYSTEM_UPGRADE: plugin requires SonarQube to be upgraded before being installed.</li><li>DEPS_REQUIRE_SYSTEM_UPGRADE: at least one plugin on which the plugin is dependent requires SonarQube to be upgraded.</li></ul>Require 'Administer System' permission.
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
   * Cancels any operation pending on any plugin (install, update or uninstall)<br/>Requires user to be authenticated with Administer System permissions
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
   * Installs the latest version of a plugin specified by its key.<br/>Plugin information is retrieved from Update Center.<br/>Requires user to be authenticated with Administer System permissions
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
   * Get the list of all the plugins installed on the SonarQube instance, sorted by plugin name.
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
   * Get the list of plugins which will either be installed or removed at the next startup of the SonarQube instance, sorted by plugin name.<br/>Require 'Administer System' permission.
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
   * Uninstalls the plugin specified by its key.<br/>Requires user to be authenticated with Administer System permissions.
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
   * Updates a plugin specified by its key to the latest version compatible with the SonarQube instance.<br/>Plugin information is retrieved from Update Center.<br/>Requires user to be authenticated with Administer System permissions
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
   * Lists plugins installed on the SonarQube instance for which at least one newer version is available, sorted by plugin name.<br/>Each newer version is listed, ordered from the oldest to the newest, with its own update/compatibility status.<br/>Plugin information is retrieved from Update Center. Date and time at which Update Center was last refreshed is provided in the response.<br/>Update status values are: [COMPATIBLE, INCOMPATIBLE, REQUIRES_UPGRADE, DEPS_REQUIRE_UPGRADE].<br/>Require 'Administer System' permission.
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
