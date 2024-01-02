/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.PluginUninstaller;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.user.UserSession;
import org.sonar.updatecenter.common.Plugin;
import org.sonarqube.ws.Plugins.PendingPluginsWsResponse;
import org.sonarqube.ws.Plugins.PluginDetails;

import static org.sonar.server.plugins.ws.PluginWSCommons.NAME_KEY_PLUGIN_METADATA_COMPARATOR;
import static org.sonar.server.plugins.ws.PluginWSCommons.buildPluginDetails;
import static org.sonar.server.plugins.ws.PluginWSCommons.compatiblePluginsByKey;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

/**
 * Implementation of the {@code pending} action for the Plugins WebService.
 */
public class PendingAction implements PluginsWsAction {
  private final UserSession userSession;
  private final PluginDownloader pluginDownloader;
  private final ServerPluginRepository serverPluginRepository;
  private final UpdateCenterMatrixFactory updateCenterMatrixFactory;
  private final PluginUninstaller pluginUninstaller;

  public PendingAction(UserSession userSession, PluginDownloader pluginDownloader,
    ServerPluginRepository serverPluginRepository, PluginUninstaller pluginUninstaller, UpdateCenterMatrixFactory updateCenterMatrixFactory) {
    this.userSession = userSession;
    this.pluginDownloader = pluginDownloader;
    this.serverPluginRepository = serverPluginRepository;
    this.pluginUninstaller = pluginUninstaller;
    this.updateCenterMatrixFactory = updateCenterMatrixFactory;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("pending")
      .setDescription("Get the list of plugins which will either be installed or removed at the next startup of the SonarQube instance, sorted by plugin name.<br/>" +
        "Require 'Administer System' permission.")
      .setSince("5.2")
      .setChangelog(
        new Change("9.8", "The 'documentationPath' field is deprecated"),
        new Change("8.0", "The 'documentationPath' field is added")
      )
      .setHandler(this)
      .setResponseExample(this.getClass().getResource("example-pending_plugins.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();
    ImmutableMap<String, Plugin> compatiblePluginsByKey = compatiblePluginsByKey(updateCenterMatrixFactory);
    PendingPluginsWsResponse.Builder buildResponse = buildResponse(compatiblePluginsByKey);
    writeProtobuf(buildResponse.build(), request, response);
  }

  private PendingPluginsWsResponse.Builder buildResponse(ImmutableMap<String, Plugin> compatiblePluginsByKey) {
    Collection<PluginInfo> uninstalledPlugins = pluginUninstaller.getUninstalledPlugins();
    Collection<PluginInfo> downloadedPlugins = pluginDownloader.getDownloadedPlugins();
    Collection<PluginInfo> installedPlugins = serverPluginRepository.getPluginInfos();
    Set<String> installedPluginKeys = installedPlugins.stream().map(PluginInfo::getKey).collect(Collectors.toSet());

    Collection<PluginInfo> newPlugins = new ArrayList<>();
    Collection<PluginInfo> updatedPlugins = new ArrayList<>();
    for (PluginInfo pluginInfo : downloadedPlugins) {
      if (installedPluginKeys.contains(pluginInfo.getKey())) {
        updatedPlugins.add(pluginInfo);
      } else {
        newPlugins.add(pluginInfo);
      }
    }

    PendingPluginsWsResponse.Builder builder = PendingPluginsWsResponse.newBuilder();
    builder.addAllInstalling(getPlugins(newPlugins, compatiblePluginsByKey));
    builder.addAllUpdating(getPlugins(updatedPlugins, compatiblePluginsByKey));
    builder.addAllRemoving(getPlugins(uninstalledPlugins, compatiblePluginsByKey));
    return builder;
  }

  private static List<PluginDetails> getPlugins(Collection<PluginInfo> plugins, Map<String, Plugin> compatiblePluginsByKey) {
    return ImmutableSortedSet.copyOf(NAME_KEY_PLUGIN_METADATA_COMPARATOR, plugins)
      .stream()
      .map(pluginInfo -> {
        Plugin plugin = compatiblePluginsByKey.get(pluginInfo.getKey());
        return buildPluginDetails(null, pluginInfo, null, plugin);
      })
      .toList();
  }
}
