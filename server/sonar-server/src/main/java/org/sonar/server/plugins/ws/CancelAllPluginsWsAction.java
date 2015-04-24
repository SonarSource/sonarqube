/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.user.UserSession;

public class CancelAllPluginsWsAction implements PluginsWsAction {

  private final PluginDownloader pluginDownloader;
  private final ServerPluginRepository pluginRepository;

  public CancelAllPluginsWsAction(PluginDownloader pluginDownloader, ServerPluginRepository pluginRepository) {
    this.pluginDownloader = pluginDownloader;
    this.pluginRepository = pluginRepository;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("cancel_all")
      .setPost(true)
      .setDescription("Cancels any operation pending on any plugin (install, update or uninstall)" +
        "<br/>" +
        "Requires user to be authenticated with Administer System permissions")
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserSession.get().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    pluginDownloader.cancelDownloads();
    pluginRepository.cancelUninstalls();

    response.noContent();
  }
}
