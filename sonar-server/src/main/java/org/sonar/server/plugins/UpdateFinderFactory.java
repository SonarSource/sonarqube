/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
import org.sonar.core.plugin.JpaPluginDao;
import org.sonar.core.plugin.JpaPlugin;
import org.sonar.api.platform.Server;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

public final class UpdateFinderFactory implements ServerComponent {

  private UpdateCenterClient centerClient;
  private JpaPluginDao dao;
  private Version sonarVersion;
  private PluginDownloader downloader;

  public UpdateFinderFactory(UpdateCenterClient centerClient, JpaPluginDao dao, Server server, PluginDownloader downloader) {
    this.centerClient = centerClient;
    this.dao = dao;
    this.sonarVersion = Version.create(server.getVersion());
    this.downloader = downloader;
  }

  public UpdateFinder getFinder(boolean refresh) {
    UpdateCenter center = centerClient.getCenter(refresh);
    UpdateFinder finder = null;
    if (center != null) {
      finder = new UpdateFinder(center, sonarVersion);
      for (JpaPlugin plugin : dao.getPlugins()) {
        finder.registerInstalledPlugin(plugin.getKey(), Version.create(plugin.getVersion()));
      }
      for (String filename : downloader.getDownloads()) {
        finder.registerPendingPluginsByFilename(filename);
      }
    }
    return finder;
  }
}

