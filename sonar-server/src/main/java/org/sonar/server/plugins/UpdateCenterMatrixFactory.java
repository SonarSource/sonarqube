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
import org.sonar.api.platform.Server;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

/**
 * @since 2.4
 */
public class UpdateCenterMatrixFactory implements ServerComponent {

  private UpdateCenterClient centerClient;
  private Version sonarVersion;
  private InstalledPluginReferentialFactory installedPluginReferentialFactory;

  public UpdateCenterMatrixFactory(UpdateCenterClient centerClient, InstalledPluginReferentialFactory installedPluginReferentialFactory, Server server) {
    this.centerClient = centerClient;
    this.installedPluginReferentialFactory = installedPluginReferentialFactory;
    this.sonarVersion = Version.create(server.getVersion());
  }

  public UpdateCenter getUpdateCenter(boolean refreshUpdateCenter) {
    UpdateCenter updatePluginCenter = centerClient.getUpdateCenter(refreshUpdateCenter);
    if (updatePluginCenter != null) {
      return updatePluginCenter.setInstalledSonarVersion(sonarVersion).registerInstalledPlugins(
          installedPluginReferentialFactory.getInstalledPluginReferential())
          .setDate(centerClient.getLastRefreshDate());
    } else {
      return null;
    }
  }

}

