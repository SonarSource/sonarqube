/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.core.platform;

import java.io.File;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ExplodedPlugin {
  private final PluginInfo pluginInfo;
  private final String key;
  private final File main;
  private final Collection<File> libs;

  @CheckForNull
  private File resourcesArchive;

  public ExplodedPlugin(PluginInfo pluginInfo, String key, File main, Collection<File> libs) {
    this.pluginInfo = pluginInfo;
    this.key = key;
    this.main = main;
    this.libs = libs;
  }

  public String getKey() {
    return key;
  }

  public File getMain() {
    return main;
  }

  public Collection<File> getLibs() {
    return libs;
  }

  public PluginInfo getPluginInfo() {
    return pluginInfo;
  }

  /**
   * A deploy-dir copy of the originally-installed archive, set only for a ZIP-packaged plugin.
   * Unlike {@link #getMain()} (the extracted scanner-side jar, with no {@code resources/} entries),
   * this file holds the plugin's static resources and, being in the deploy dir, survives the
   * installed archive being moved away by an uninstall while the server is still up.
   */
  @CheckForNull
  public File getResourcesArchive() {
    return resourcesArchive;
  }

  public ExplodedPlugin setResourcesArchive(@Nullable File resourcesArchive) {
    this.resourcesArchive = resourcesArchive;
    return this;
  }
}
