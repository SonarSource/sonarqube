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
package org.sonar.server.plugins.edition;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.plugins.ServerPluginRepository;

public class EditionInstaller {
  private final ReentrantLock lock = new ReentrantLock();
  private final EditionInstallerExecutor executor;
  private final EditionPluginDownloader editionPluginDownloader;
  private final EditionPluginUninstaller editionPluginUninstaller;
  private final ServerPluginRepository pluginRepository;

  public EditionInstaller(EditionPluginDownloader editionDownloader, EditionPluginUninstaller editionPluginUninstaller,
    ServerPluginRepository pluginRepository, EditionInstallerExecutor executor) {
    this.editionPluginDownloader = editionDownloader;
    this.editionPluginUninstaller = editionPluginUninstaller;
    this.pluginRepository = pluginRepository;
    this.executor = executor;
  }

  public void install(Set<String> editionPlugins) {
    if (lock.tryLock()) {
      try {
        if (!requiresInstallationChange(editionPlugins)) {
          return;
        }

        executor.execute(() -> asyncInstall(editionPlugins));
      } catch (RuntimeException e) {
        lock.unlock();
        throw e;
      }
    } else {
      throw new IllegalStateException("Another installation of an edition is already running");
    }
  }

  public boolean requiresInstallationChange(Set<String> editionPluginKeys) {
    return !pluginsToInstall(editionPluginKeys).isEmpty() || !pluginsToRemove(editionPluginKeys).isEmpty();
  }

  private void asyncInstall(Set<String> editionPluginKeys) {
    try {
      // TODO clean previously staged edition installations, or fail?
      // editionPluginDownloader.cancelDownloads();
      // editionPluginUninstaller.cancelUninstalls();
      editionPluginDownloader.installEdition(pluginsToInstall(editionPluginKeys));
      for (String pluginKey : pluginsToRemove(editionPluginKeys)) {
        editionPluginUninstaller.uninstall(pluginKey);
      }
    } finally {
      lock.unlock();
    }
  }

  private Set<String> pluginsToInstall(Set<String> editionPluginKeys) {
    Set<String> installedKeys = pluginRepository.getPluginInfosByKeys().keySet();
    return editionPluginKeys.stream()
      .filter(p -> !installedKeys.contains(p))
      .collect(Collectors.toSet());
  }

  private Set<String> pluginsToRemove(Set<String> editionPluginKeys) {
    Set<String> installedCommercialPluginKeys = pluginRepository.getPluginInfos().stream()
      .filter(EditionInstaller::isSonarSourceCommercialPlugin)
      .map(PluginInfo::getKey)
      .collect(Collectors.toSet());

    return installedCommercialPluginKeys.stream()
      .filter(p -> !editionPluginKeys.contains(p))
      .collect(Collectors.toSet());
  }

  private static boolean isSonarSourceCommercialPlugin(PluginInfo pluginInfo) {
    return "Commercial".equals(pluginInfo.getLicense()) && "SonarSource".equals(pluginInfo.getOrganizationName());
  }

}
