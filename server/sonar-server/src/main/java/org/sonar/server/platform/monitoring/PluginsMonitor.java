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
package org.sonar.server.platform.monitoring;

import java.util.LinkedHashMap;
import java.util.Map;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.updatecenter.common.Version;

/**
 * Installed plugins (excluding core plugins)
 */
public class PluginsMonitor implements Monitor {
  private final PluginRepository repository;

  public PluginsMonitor(PluginRepository repository) {
    this.repository = repository;
  }

  @Override
  public String name() {
    return "Plugins";
  }

  @Override
  public Map<String, Object> attributes() {
    Map<String, Object> attributes = new LinkedHashMap<>();
    for (PluginInfo plugin : repository.getPluginInfos()) {
      LinkedHashMap<String, Object> pluginAttributes = new LinkedHashMap<>();
      pluginAttributes.put("Name", plugin.getName());
      Version version = plugin.getVersion();
      if (version != null) {
        pluginAttributes.put("Version", version.getName());
      }
      attributes.put(plugin.getKey(), pluginAttributes);
    }
    return attributes;
  }
}
