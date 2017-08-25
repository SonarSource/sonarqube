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

package org.sonar.server.telemetry;

import java.util.Map;
import java.util.function.Function;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.measure.index.ProjectMeasuresIndex;
import org.sonar.server.measure.index.ProjectMeasuresStatistics;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserQuery;

@ServerSide
public class TelemetryDataLoader {
  private final Server server;
  private final PluginRepository pluginRepository;
  private final UserIndex userIndex;
  private final ProjectMeasuresIndex projectMeasuresIndex;

  public TelemetryDataLoader(Server server, PluginRepository pluginRepository, UserIndex userIndex, ProjectMeasuresIndex projectMeasuresIndex) {
    this.server = server;
    this.pluginRepository = pluginRepository;
    this.userIndex = userIndex;
    this.projectMeasuresIndex = projectMeasuresIndex;
  }

  public TelemetryData load() {
    TelemetryData.Builder data = TelemetryData.builder();

    data.setServerId(server.getId());
    data.setVersion(server.getVersion());
    Function<PluginInfo, String> getVersion = plugin -> plugin.getVersion() == null ? "undefined" : plugin.getVersion().getName();
    Map<String, String> plugins = pluginRepository.getPluginInfos().stream().collect(MoreCollectors.uniqueIndex(PluginInfo::getKey, getVersion));
    data.setPlugins(plugins);
    long userCount = userIndex.search(UserQuery.builder().build(), new SearchOptions().setLimit(1)).getTotal();
    data.setUserCount(userCount);
    ProjectMeasuresStatistics projectMeasuresStatistics = projectMeasuresIndex.searchTelemetryStatistics();
    data.setProjectMeasuresStatistics(projectMeasuresStatistics);

    return data.build();
  }

  String loadServerId() {
    return server.getId();
  }
}
