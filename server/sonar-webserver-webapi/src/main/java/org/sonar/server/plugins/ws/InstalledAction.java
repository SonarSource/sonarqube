/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import com.google.common.io.Resources;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.platform.PluginInfo;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.plugin.PluginDto;
import org.sonar.db.plugin.PluginDto.Type;
import org.sonar.server.plugins.PluginType;
import org.sonar.server.plugins.ServerPlugin;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.updatecenter.common.Plugin;
import org.sonarqube.ws.Plugins.InstalledPluginsWsResponse;
import org.sonarqube.ws.Plugins.PluginDetails;

import static com.google.common.collect.ImmutableSortedSet.copyOf;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toMap;
import static org.sonar.server.plugins.ws.PluginWSCommons.NAME_KEY_COMPARATOR;
import static org.sonar.server.plugins.ws.PluginWSCommons.buildPluginDetails;
import static org.sonar.server.plugins.ws.PluginWSCommons.compatiblePluginsByKey;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

/**
 * Implementation of the {@code installed} action for the Plugins WebService.
 */
public class InstalledAction implements PluginsWsAction {
  private static final String FIELD_CATEGORY = "category";
  private static final String PARAM_TYPE = "type";

  private final ServerPluginRepository serverPluginRepository;
  private final UpdateCenterMatrixFactory updateCenterMatrixFactory;
  private final DbClient dbClient;

  public InstalledAction(ServerPluginRepository serverPluginRepository, UpdateCenterMatrixFactory updateCenterMatrixFactory, DbClient dbClient) {
    this.serverPluginRepository = serverPluginRepository;
    this.updateCenterMatrixFactory = updateCenterMatrixFactory;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("installed")
      .setDescription("Get the list of all the plugins installed on the SonarQube instance, sorted by plugin name.")
      .setSince("5.2")
      .setChangelog(
        new Change("8.0", "The 'documentationPath' field is added"),
        new Change("7.0", "The fields 'compressedHash' and 'compressedFilename' are added"),
        new Change("6.6", "The 'filename' field is added"),
        new Change("6.6", "The 'fileHash' field is added"),
        new Change("6.6", "The 'sonarLintSupported' field is added"),
        new Change("6.6", "The 'updatedAt' field is added"))
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-installed_plugins.json"));

    action.createFieldsParam(singleton("category"))
      .setDescription(format("Comma-separated list of the additional fields to be returned in response. No additional field is returned by default. Possible values are:" +
        "<ul>" +
        "<li>%s - category as defined in the Update Center. A connection to the Update Center is needed</li>" +
        "</lu>", FIELD_CATEGORY))
      .setSince("5.6");

    action.createParam(PARAM_TYPE)
      .setInternal(true)
      .setSince("8.5")
      .setPossibleValues(Type.values())
      .setDescription("Allows to filter plugins by type");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String typeParam = request.param(PARAM_TYPE);
    SortedSet<ServerPlugin> installedPlugins = loadInstalledPlugins(typeParam);
    Map<String, PluginDto> dtosByKey;
    try (DbSession dbSession = dbClient.openSession(false)) {
      dtosByKey = dbClient.pluginDao().selectAll(dbSession).stream().collect(toMap(PluginDto::getKee, Function.identity()));
    }

    List<String> additionalFields = request.paramAsStrings(WebService.Param.FIELDS);
    Map<String, Plugin> updateCenterPlugins = (additionalFields == null || additionalFields.isEmpty()) ? emptyMap() : compatiblePluginsByKey(updateCenterMatrixFactory);

    List<PluginDetails> pluginList = new LinkedList<>();

    for (ServerPlugin installedPlugin : installedPlugins) {
      PluginInfo pluginInfo = installedPlugin.getPluginInfo();
      PluginDto pluginDto = dtosByKey.get(pluginInfo.getKey());
      Objects.requireNonNull(pluginDto, () -> format("Plugin %s is installed but not in DB", pluginInfo.getKey()));
      Plugin updateCenterPlugin = updateCenterPlugins.get(pluginInfo.getKey());

      pluginList.add(buildPluginDetails(installedPlugin, pluginInfo, pluginDto, updateCenterPlugin));
    }

    InstalledPluginsWsResponse wsResponse = InstalledPluginsWsResponse.newBuilder()
      .addAllPlugins(pluginList)
      .build();
    writeProtobuf(wsResponse, request, response);
  }

  private SortedSet<ServerPlugin> loadInstalledPlugins(@Nullable String typeParam) {
    if (typeParam != null) {
      return copyOf(NAME_KEY_COMPARATOR, serverPluginRepository.getPlugins().stream()
        .filter(serverPlugin -> serverPlugin.getType().equals(PluginType.valueOf(typeParam)))
        .collect(Collectors.toSet()));
    }
    return copyOf(NAME_KEY_COMPARATOR, serverPluginRepository.getPlugins());
  }
}
