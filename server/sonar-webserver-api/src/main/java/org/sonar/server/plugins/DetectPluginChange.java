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
package org.sonar.server.plugins;

import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.Startable;
import org.sonar.api.utils.Preconditions;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.plugin.PluginType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.plugin.PluginDto;

import static java.util.function.Function.identity;

public class DetectPluginChange implements Startable {
  private static final Logger LOG = Loggers.get(DetectPluginChange.class);

  private final ServerPluginRepository serverPluginRepository;
  private final DbClient dbClient;
  private Boolean changesDetected = null;

  public DetectPluginChange(ServerPluginRepository serverPluginRepository, DbClient dbClient) {
    this.serverPluginRepository = serverPluginRepository;
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    Preconditions.checkState(changesDetected == null, "Can only call #start() once");
    Profiler profiler = Profiler.create(LOG).startInfo("Detect plugin changes");
    changesDetected = anyChange();
    if (changesDetected) {
      LOG.debug("Plugin changes detected");
    } else {
      LOG.info("No plugin change detected");
    }
    profiler.stopDebug();
  }

  /**
   * @throws NullPointerException if {@link #start} hasn't been called
   */
  public boolean anyPluginChanged() {
    return changesDetected;
  }

  private boolean anyChange() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, PluginDto> dbPluginsByKey = dbClient.pluginDao().selectAll(dbSession).stream()
        .filter(p -> !p.isRemoved())
        .collect(Collectors.toMap(PluginDto::getKee, identity()));
      Map<String, ServerPlugin> filePluginsByKey = serverPluginRepository.getPlugins().stream()
        .collect(Collectors.toMap(p -> p.getPluginInfo().getKey(), p -> p));

      if (!dbPluginsByKey.keySet().equals(filePluginsByKey.keySet())) {
        return true;
      }

      for (ServerPlugin installed : filePluginsByKey.values()) {
        PluginDto dbPlugin = dbPluginsByKey.get(installed.getPluginInfo().getKey());
        if (changed(dbPlugin, installed)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean changed(PluginDto dbPlugin, ServerPlugin filePlugin) {
    return !dbPlugin.getFileHash().equals(filePlugin.getJar().getMd5()) || !dbPlugin.getType().equals(toTypeDto(filePlugin.getType()));
  }

  static PluginDto.Type toTypeDto(PluginType type) {
    switch (type) {
      case EXTERNAL:
        return PluginDto.Type.EXTERNAL;
      case BUNDLED:
        return PluginDto.Type.BUNDLED;
      default:
        throw new IllegalStateException("Unknown type: " + type);
    }
  }

  @Override
  public void stop() {
    // Nothing to do
  }
}
