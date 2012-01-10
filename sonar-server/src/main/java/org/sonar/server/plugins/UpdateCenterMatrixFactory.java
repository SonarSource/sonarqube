/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.plugins;

import org.sonar.api.ServerComponent;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;
import org.sonar.api.platform.Server;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

/**
 * @since 2.4
 */
public final class UpdateCenterMatrixFactory implements ServerComponent {

  private UpdateCenterClient centerClient;
  private Version sonarVersion;
  private PluginDownloader downloader;
  private PluginRepository pluginRepository;

  public UpdateCenterMatrixFactory(UpdateCenterClient centerClient, PluginRepository pluginRepository, Server server, PluginDownloader downloader) {
    this.centerClient = centerClient;
    this.pluginRepository = pluginRepository;
    this.sonarVersion = Version.create(server.getVersion());
    this.downloader = downloader;
  }

  public UpdateCenterMatrix getMatrix(boolean refresh) {
    UpdateCenter center = centerClient.getCenter(refresh);
    UpdateCenterMatrix matrix = null;
    if (center != null) {
      matrix = new UpdateCenterMatrix(center, sonarVersion);
      matrix.setDate(centerClient.getLastRefreshDate());

      for (PluginMetadata metadata : pluginRepository.getMetadata()) {
        matrix.registerInstalledPlugin(metadata.getKey(), Version.create(metadata.getVersion()));
      }
      for (String filename : downloader.getDownloads()) {
        matrix.registerPendingPluginsByFilename(filename);
      }
    }
    return matrix;
  }
}

