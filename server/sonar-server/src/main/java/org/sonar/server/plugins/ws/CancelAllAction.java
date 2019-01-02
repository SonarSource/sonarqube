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
package org.sonar.server.plugins.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.PluginUninstaller;
import org.sonar.server.user.UserSession;

public class CancelAllAction implements PluginsWsAction {

  private final PluginDownloader pluginDownloader;
  private final PluginUninstaller pluginUninstaller;
  private final UserSession userSession;

  public CancelAllAction(PluginDownloader pluginDownloader, PluginUninstaller pluginUninstaller, UserSession userSession) {
    this.pluginDownloader = pluginDownloader;
    this.pluginUninstaller = pluginUninstaller;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("cancel_all")
      .setPost(true)
      .setSince("5.2")
      .setDescription("Cancels any operation pending on any plugin (install, update or uninstall)" +
        "<br/>" +
        "Requires user to be authenticated with Administer System permissions")
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    pluginDownloader.cancelDownloads();
    pluginUninstaller.cancelUninstalls();

    response.noContent();
  }
}
