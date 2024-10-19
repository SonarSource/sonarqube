/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Priority;
import org.sonar.api.Plugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.plugin.PluginType;
import org.sonar.scanner.bootstrap.PluginInstaller;
import org.sonar.scanner.bootstrap.ScannerPlugin;

@Priority(1)
public class FakePluginInstaller implements PluginInstaller {

  private final Map<String, ScannerPlugin> pluginsByKeys = new HashMap<>();
  private final List<LocalPlugin> mediumTestPlugins = new ArrayList<>();
  private final List<LocalPlugin> optionalMediumTestPlugins = new ArrayList<>();

  public FakePluginInstaller add(String pluginKey, File jarFile, long lastUpdatedAt) {
    pluginsByKeys.put(pluginKey, new ScannerPlugin(pluginKey, lastUpdatedAt, PluginType.BUNDLED, PluginInfo.create(jarFile)));
    return this;
  }

  public FakePluginInstaller add(String pluginKey, Plugin instance) {
    mediumTestPlugins.add(new LocalPlugin(pluginKey, instance, Set.of()));
    return this;
  }

  public FakePluginInstaller addOptional(String pluginKey, Set<String> requiredForLanguages, Plugin instance) {
    optionalMediumTestPlugins.add(new LocalPlugin(pluginKey, instance, requiredForLanguages));
    return this;
  }

  @Override
  public Map<String, ScannerPlugin> installAllPlugins() {
    return pluginsByKeys;
  }

  @Override
  public Map<String, ScannerPlugin> installRequiredPlugins() {
    return pluginsByKeys;
  }

  @Override
  public Map<String, ScannerPlugin> installPluginsForLanguages(Set<String> languageKeys) {
    return pluginsByKeys;
  }

  @Override
  public List<LocalPlugin> installLocals() {
    return mediumTestPlugins;
  }

  @Override
  public List<LocalPlugin> installOptionalLocals(Set<String> languageKeys) {
    return optionalMediumTestPlugins.stream()
      .filter(plugin -> languageKeys.stream().anyMatch(lang -> plugin.requiredForLanguages().contains(lang)))
      .toList();
  }
}
