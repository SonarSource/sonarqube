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

import com.google.common.base.Optional;
import java.util.HashSet;
import java.util.Set;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.server.plugins.AbstractPluginDownloader;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

public class EditionPluginDownloader extends AbstractPluginDownloader {
  private final UpdateCenterMatrixFactory updateCenterMatrixFactory;

  public EditionPluginDownloader(UpdateCenterMatrixFactory updateCenterMatrixFactory, HttpDownloader downloader, ServerFileSystem fileSystem) {
    super(fileSystem.getEditionDownloadedPluginsDir(), downloader);
    this.updateCenterMatrixFactory = updateCenterMatrixFactory;
  }

  public void installEdition(Set<String> pluginKeys) {
    try {
      Optional<UpdateCenter> updateCenter = updateCenterMatrixFactory.getUpdateCenter(true);
      if (updateCenter.isPresent()) {
        Set<Release> pluginsToInstall = new HashSet<>();
        for (String pluginKey : pluginKeys) {
          pluginsToInstall.addAll(updateCenter.get().findInstallablePlugins(pluginKey, Version.create("")));
        }

        for (Release r : pluginsToInstall) {
          super.download(r);
        }
      }
    } catch (Exception e) {
      cleanTempFiles();
      throw e;
    }
  }
}
