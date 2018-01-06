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
package org.sonar.server.plugins.edition;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.edition.License;
import org.sonar.server.edition.MutableEditionManagementState;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.updatecenter.common.UpdateCenter;

public class EditionInstaller {
  private static final Logger LOG = Loggers.get(EditionInstaller.class);

  private final Semaphore semaphore = new Semaphore(1);
  private final EditionInstallerExecutor executor;
  private final EditionPluginDownloader editionPluginDownloader;
  private final EditionPluginUninstaller editionPluginUninstaller;
  private final ServerPluginRepository pluginRepository;
  private final UpdateCenterMatrixFactory updateCenterMatrixFactory;
  private final MutableEditionManagementState editionManagementState;

  public EditionInstaller(EditionPluginDownloader editionDownloader, EditionPluginUninstaller editionPluginUninstaller,
    ServerPluginRepository pluginRepository, EditionInstallerExecutor executor, UpdateCenterMatrixFactory updateCenterMatrixFactory,
    MutableEditionManagementState editionManagementState) {
    this.editionPluginDownloader = editionDownloader;
    this.editionPluginUninstaller = editionPluginUninstaller;
    this.pluginRepository = pluginRepository;
    this.executor = executor;
    this.updateCenterMatrixFactory = updateCenterMatrixFactory;
    this.editionManagementState = editionManagementState;
  }

  /**
   * Refreshes the update center, and submits in a executor a task to download all the needed plugins (asynchronously).
   * If the update center is disabled or if we are offline, the task is not submitted.
   *
   * @throws IllegalStateException if an installation is already in progress
   */
  public void install(License newLicense) {
    if (semaphore.tryAcquire()) {
      try {
        Optional<UpdateCenter> updateCenter = updateCenterMatrixFactory.getUpdateCenter(true);
        if (!updateCenter.isPresent()) {
          LOG.info("Installation of edition '{}' needs to be done manually", newLicense.getEditionKey());
          editionManagementState.startManualInstall(newLicense);
          return;
        }
        editionManagementState.startAutomaticInstall(newLicense);
        executor.execute(() -> asyncInstall(newLicense, updateCenter.get()));
      } catch (RuntimeException e) {
        semaphore.release();
        throw e;
      }
    } else {
      throw new IllegalStateException("Another installation of an edition is already running");
    }
  }

  public void uninstall() {
    Map<String, PluginInfo> pluginInfosByKeys = pluginRepository.getPluginInfosByKeys();
    Set<String> pluginsToRemove = pluginsToRemove(Collections.emptySet(), pluginInfosByKeys.values());
    uninstallPlugins(pluginsToRemove);
  }

  /**
   * Check if the update center is disabled or unreachable. It uses the cached status (it doesn't refresh),
   * to be a cost-free check.
   */
  public boolean isOffline() {
    return !updateCenterMatrixFactory.getUpdateCenter(false).isPresent();
  }

  public boolean requiresInstallationChange(Set<String> editionPluginKeys) {
    Map<String, PluginInfo> pluginInfosByKeys = pluginRepository.getPluginInfosByKeys();

    return !pluginsToInstall(editionPluginKeys, pluginInfosByKeys.keySet()).isEmpty()
      || !pluginsToRemove(editionPluginKeys, pluginInfosByKeys.values()).isEmpty();
  }

  private void asyncInstall(License newLicense, UpdateCenter updateCenter) {
    try {
      Set<String> editionPluginKeys = newLicense.getPluginKeys();
      Map<String, PluginInfo> pluginInfosByKeys = pluginRepository.getPluginInfosByKeys();
      Set<String> pluginsToRemove = pluginsToRemove(editionPluginKeys, pluginInfosByKeys.values());
      Set<String> pluginsToInstall = pluginsToInstall(editionPluginKeys, pluginInfosByKeys.keySet());

      LOG.info("Installing edition '{}', download: {}, remove: {}", 
        newLicense.getEditionKey(), pluginsToInstall, pluginsToRemove);

      editionPluginDownloader.downloadEditionPlugins(pluginsToInstall, updateCenter);
      uninstallPlugins(pluginsToRemove);
      editionManagementState.automaticInstallReady();
    } catch (Throwable t) {
      LOG.error("Failed to install edition {} with plugins {}", newLicense.getEditionKey(), newLicense.getPluginKeys(), t);
      editionManagementState.installFailed(t.getMessage());
    } finally {
      semaphore.release();
    }
  }

  private void uninstallPlugins(Set<String> pluginsToRemove) {
    pluginsToRemove.stream().forEach(editionPluginUninstaller::uninstall);
  }

  private static Set<String> pluginsToInstall(Set<String> editionPluginKeys, Set<String> installedPluginKeys) {
    return editionPluginKeys.stream()
      .filter(p -> !installedPluginKeys.contains(p))
      .collect(Collectors.toSet());
  }

  private static Set<String> pluginsToRemove(Set<String> editionPluginKeys, Collection<PluginInfo> installedPluginInfos) {
    Set<String> installedCommercialPluginKeys = installedPluginInfos.stream()
      .filter(EditionBundledPlugins::isEditionBundled)
      .map(PluginInfo::getKey)
      .collect(Collectors.toSet());

    return installedCommercialPluginKeys.stream()
      .filter(p -> !editionPluginKeys.contains(p))
      .collect(Collectors.toSet());
  }

}
