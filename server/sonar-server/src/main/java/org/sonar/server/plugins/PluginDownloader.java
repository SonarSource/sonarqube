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
package org.sonar.server.plugins;

import com.google.common.base.Optional;
import java.util.List;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static org.sonar.server.ws.WsUtils.checkRequest;

/**
 * Downloads plugins from update center. Files are copied in the directory extensions/downloads and then
 * moved to extensions/plugins after server restart.
 */
public class PluginDownloader extends AbstractPluginDownloader {
  private final UpdateCenterMatrixFactory updateCenterMatrixFactory;

  public PluginDownloader(UpdateCenterMatrixFactory updateCenterMatrixFactory, HttpDownloader downloader, ServerFileSystem fileSystem) {
    super(fileSystem.getDownloadedPluginsDir(), downloader);
    this.updateCenterMatrixFactory = updateCenterMatrixFactory;
  }

  public void download(String pluginKey, Version version) {
    Optional<UpdateCenter> updateCenter = updateCenterMatrixFactory.getUpdateCenter(true);
    if (updateCenter.isPresent()) {
      List<Release> installablePlugins = updateCenter.get().findInstallablePlugins(pluginKey, version);
      checkRequest(!installablePlugins.isEmpty(), "Error while downloading plugin '%s' with version '%s'. No compatible plugin found.", pluginKey, version.getName());
      for (Release r : installablePlugins) {
        super.download(r);
      }
    }
  }
}
