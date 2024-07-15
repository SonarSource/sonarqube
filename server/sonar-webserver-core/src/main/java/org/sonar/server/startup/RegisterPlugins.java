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
package org.sonar.server.startup;

import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.plugin.PluginType;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.plugin.PluginDto;
import org.sonar.server.plugins.ServerPlugin;
import org.sonar.server.plugins.ServerPluginRepository;

import static java.util.function.Function.identity;

/**
 * Take care to update the 'plugins' table at startup.
 */
@ServerSide
public class RegisterPlugins implements Startable {

  private static final Logger LOG = Loggers.get(RegisterPlugins.class);

  private final ServerPluginRepository serverPluginRepository;
  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final System2 system;

  public RegisterPlugins(ServerPluginRepository serverPluginRepository, DbClient dbClient, UuidFactory uuidFactory, System2 system) {
    this.serverPluginRepository = serverPluginRepository;
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.system = system;
  }

  @Override
  public void start() {
    Profiler profiler = Profiler.create(LOG).startInfo("Register plugins");
    updateDB();
    profiler.stopDebug();
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  private void updateDB() {
    long now = system.now();
    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, PluginDto> allPreviousPluginsByKey = dbClient.pluginDao().selectAll(dbSession).stream()
        .collect(Collectors.toMap(PluginDto::getKee, identity()));

      for (ServerPlugin installed : serverPluginRepository.getPlugins()) {
        PluginInfo info = installed.getPluginInfo();
        PluginDto previousDto = allPreviousPluginsByKey.get(info.getKey());
        if (previousDto == null) {
          LOG.debug("Register new plugin {}", info.getKey());
          insertNewPluginDto(dbSession, installed, info);
          continue;
        }
        if (pluginTypeOrJarHashChanged(installed, previousDto)) {
          LOG.debug("Update plugin {}", info.getKey());
          updatePluginDto(dbSession, installed, info, previousDto);
        }
        if (previousDto.isRemoved()) {
          LOG.debug("Previously removed plugin {} was re-installed", info.getKey());
          previousDto.setRemoved(false);
          updatePluginDto(dbSession, installed, info, previousDto);
        }
      }

      // keep uninstalled plugins with a 'removed' flag, because corresponding rules and active rules are also not deleted
      for (PluginDto dto : allPreviousPluginsByKey.values()) {
        if (dto.isRemoved()) {
          continue;
        }

        if (serverPluginRepository.findPlugin(dto.getKee()).isEmpty()) {
          dto
            .setRemoved(true)
            .setUpdatedAt(now);
          dbClient.pluginDao().update(dbSession, dto);
        }
      }

      dbSession.commit();
    }
  }

  private void insertNewPluginDto(DbSession dbSession, ServerPlugin installed, PluginInfo info) {
    long now = system.now();
    PluginDto pluginDto = new PluginDto()
      .setUuid(uuidFactory.create())
      .setKee(info.getKey())
      .setBasePluginKey(info.getBasePlugin())
      .setFileHash(installed.getJar().getMd5())
      .setType(toTypeDto(installed.getType()))
      .setCreatedAt(now)
      .setUpdatedAt(now);
    dbClient.pluginDao().insert(dbSession, pluginDto);
  }

  private void updatePluginDto(DbSession dbSession, ServerPlugin installed, PluginInfo info, PluginDto previousDto) {
    long now = system.now();
    previousDto
      .setBasePluginKey(info.getBasePlugin())
      .setFileHash(installed.getJar().getMd5())
      .setType(toTypeDto(installed.getType()))
      .setUpdatedAt(now);
    dbClient.pluginDao().update(dbSession, previousDto);
  }

  private static boolean pluginTypeOrJarHashChanged(ServerPlugin installed, PluginDto previousDto) {
    return !previousDto.getFileHash().equals(installed.getJar().getMd5()) || !previousDto.getType().equals(toTypeDto(installed.getType()));
  }

  private static PluginDto.Type toTypeDto(PluginType type) {
    switch (type) {
      case EXTERNAL:
        return PluginDto.Type.EXTERNAL;
      case BUNDLED:
        return PluginDto.Type.BUNDLED;
      default:
        throw new IllegalStateException("Unknown type: " + type);
    }
  }

}
