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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.user.UserSession;
import org.sonar.updatecenter.common.PluginUpdate;

import static java.lang.String.format;

/**
 * Implementation of the {@code update} action for the Plugins WebService.
 */
public class UpdatePluginsWsAction implements PluginsWsAction {

  public static final String PARAM_KEY = "key";
  public static final PluginUpdate MISSING_PLUGIN = null;

  private final UpdateCenterMatrixFactory updateCenterFactory;
  private final PluginDownloader pluginDownloader;

  public UpdatePluginsWsAction(UpdateCenterMatrixFactory updateCenterFactory, PluginDownloader pluginDownloader) {
    this.updateCenterFactory = updateCenterFactory;
    this.pluginDownloader = pluginDownloader;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("update")
      .setPost(true)
      .setDescription("Updates a plugin specified by its key to the latest version compatible with the SonarQube instance." +
        "<br/>" +
        "Plugin information is retrieved from Update Center." +
        "<br/>" +
        "Requires user to be authenticated with Administer System permissions")
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("The key identifying the plugin to update");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserSession.get().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
    String key = request.mandatoryParam(PARAM_KEY);
    PluginUpdate pluginUpdate = findPluginUpdateByKey(key);
    pluginDownloader.download(key, pluginUpdate.getRelease().getVersion());
    response.noContent();
  }

  @Nonnull
  private PluginUpdate findPluginUpdateByKey(String key) {
    PluginUpdate pluginUpdate = Iterables.find(
      updateCenterFactory.getUpdateCenter(false).findPluginUpdates(),
      new PluginKeyPredicate(key),
      MISSING_PLUGIN
      );
    if (pluginUpdate == MISSING_PLUGIN) {
      throw new IllegalArgumentException(
        format("No plugin with key '%s' or plugin '%s' is already in latest compatible version", key, key));
    }
    return pluginUpdate;
  }

  private static class PluginKeyPredicate implements Predicate<PluginUpdate> {
    private final String key;

    public PluginKeyPredicate(String key) {
      this.key = key;
    }

    @Override
    public boolean apply(@Nullable PluginUpdate input) {
      return input != null && key.equals(input.getPlugin().getKey());
    }
  }
}
