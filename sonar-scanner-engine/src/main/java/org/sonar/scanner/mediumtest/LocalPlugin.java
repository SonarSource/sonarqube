/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.scanner.mediumtest;

import java.util.Set;
import org.sonar.api.Plugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.plugin.PluginType;
import org.sonar.scanner.bootstrap.ScannerPlugin;

public record LocalPlugin(PluginInfo pluginInfo, Plugin pluginInstance, Set<String> requiredForLanguages) {

  public LocalPlugin(String pluginKey, Plugin pluginInstance, Set<String> requiredForLanguages) {
    this(new PluginInfo(pluginKey).setOrganizationName("SonarSource"), pluginInstance, requiredForLanguages);
  }

  public ScannerPlugin toScannerPlugin() {
    return new ScannerPlugin(pluginInfo.getKey(), 1L, PluginType.BUNDLED, pluginInfo);
  }
}
