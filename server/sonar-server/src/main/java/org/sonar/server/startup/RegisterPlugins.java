/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.Startable;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.RemotePlugin;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.plugin.PluginDto;
import org.sonar.server.plugins.ServerPluginRepository;

import static java.util.function.Function.identity;

/**
 * Take care to update the 'plugins' table at startup.
 */
public class RegisterPlugins implements Startable {

  private static final Logger LOG = Loggers.get(RegisterPlugins.class);

  private final ServerPluginRepository repository;
  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final System2 system;

  public RegisterPlugins(ServerPluginRepository repository, DbClient dbClient, UuidFactory uuidFactory, System2 system) {
    this.repository = repository;
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.system = system;
  }

  @Override
  public void start() {
    Profiler profiler = Profiler.create(LOG).startInfo("Register plugins");
    updateDB(repository.getPluginInfos());
    profiler.stopDebug();
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  private void updateDB(Collection<PluginInfo> pluginInfos) {
    long now = system.now();
    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, PluginDto> allPreviousPluginsByKey = dbClient.pluginDao().selectAll(dbSession).stream()
        .collect(Collectors.toMap(PluginDto::getKee, identity()));
      for (PluginInfo pluginInfo : pluginInfos) {
        RemotePlugin remotePlugin = RemotePlugin.create(pluginInfo);
        String newJarMd5 = remotePlugin.file().getHash();
        PluginDto previousDto = allPreviousPluginsByKey.get(pluginInfo.getKey());
        if (previousDto == null) {
          LOG.debug("Register new plugin {}", pluginInfo.getKey());
          PluginDto pluginDto = new PluginDto()
            .setUuid(uuidFactory.create())
            .setKee(pluginInfo.getKey())
            .setBasePluginKey(pluginInfo.getBasePlugin())
            .setFileHash(newJarMd5)
            .setCreatedAt(now)
            .setUpdatedAt(now);
          dbClient.pluginDao().insert(dbSession, pluginDto);
        } else if (!previousDto.getFileHash().equals(newJarMd5)) {
          LOG.debug("Update plugin {}", pluginInfo.getKey());
          previousDto
            .setBasePluginKey(pluginInfo.getBasePlugin())
            .setFileHash(newJarMd5)
            .setUpdatedAt(now);
          dbClient.pluginDao().update(dbSession, previousDto);
        }
        // Don't remove uninstalled plugins, because corresponding rules and active rules are also not deleted
      }
      dbSession.commit();
    }
  }

}
