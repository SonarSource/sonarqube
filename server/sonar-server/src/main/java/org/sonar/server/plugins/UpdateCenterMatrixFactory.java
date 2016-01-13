/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Optional;
import org.sonar.api.platform.Server;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

/**
 * @since 2.4
 */
public class UpdateCenterMatrixFactory {

  private final UpdateCenterClient centerClient;
  private final Version sonarVersion;
  private final InstalledPluginReferentialFactory installedPluginReferentialFactory;

  public UpdateCenterMatrixFactory(UpdateCenterClient centerClient, Server server,
    InstalledPluginReferentialFactory installedPluginReferentialFactory) {
    this.centerClient = centerClient;
    this.installedPluginReferentialFactory = installedPluginReferentialFactory;
    this.sonarVersion = Version.create(server.getVersion());
  }

  public Optional<UpdateCenter> getUpdateCenter(boolean refreshUpdateCenter) {
    Optional<UpdateCenter> updateCenter = centerClient.getUpdateCenter(refreshUpdateCenter);
    if (updateCenter.isPresent()) {
      return Optional.of(updateCenter.get().setInstalledSonarVersion(sonarVersion).registerInstalledPlugins(
        installedPluginReferentialFactory.getInstalledPluginReferential())
        .setDate(centerClient.getLastRefreshDate()));
    }
    return Optional.absent();
  }
}
