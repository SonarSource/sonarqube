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
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;

/**
 * Implementation of the {@code uninstall} action for the Plugins WebService.
 */
public class UninstallPluginsWsAction implements PluginsWsAction {
  private static final String PARAM_KEY = "key";

  private final ServerPluginRepository pluginRepository;

  public UninstallPluginsWsAction(ServerPluginRepository pluginRepository) {
    this.pluginRepository = pluginRepository;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("uninstall")
      .setPost(true)
      .setDescription("Uninstalls the plugin specified by its key." +
        "<br/>" +
        "Requires user to be authenticated with Administer System permissions.")
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setDescription("The key identifying the plugin to uninstall")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserSession.get().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
    String key = request.mandatoryParam(PARAM_KEY);
    ensurePluginIsInstalled(key);
    pluginRepository.uninstall(key);
    response.noContent();
  }

  // FIXME should be moved to {@link ServerPluginRepository#uninstall(String)}
  private void ensurePluginIsInstalled(String key) {
    if (!pluginRepository.hasPlugin(key)) {
      throw new IllegalArgumentException(format("Plugin [%s] is not installed", key));
    }
  }
}
