/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.platform.monitoring;

import com.google.common.base.Joiner;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;
import org.sonar.api.utils.text.JsonWriter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PluginsMonitoring extends MonitoringMBean implements PluginsMonitoringMBean {
  private final PluginRepository repository;

  public PluginsMonitoring(PluginRepository repository) {
    this.repository = repository;
  }

  @Override
  public String getPlugins() {
    Map<String, String> plugins = new HashMap<>();
    for (PluginMetadata plugin : plugins()) {
      plugins.put(plugin.getName(), plugin.getVersion());
    }

    return Joiner.on(",").withKeyValueSeparator(":").join(plugins);
  }

  @Override
  public String name() {
    return "Plugins";
  }

  @Override
  public void toJson(JsonWriter json) {
    json.beginArray();
    for (PluginMetadata plugin : plugins()) {
      json.prop(plugin.getName(), plugin.getVersion());
    }
    json.endArray();
  }

  private Collection<PluginMetadata> plugins() {
    return repository.getMetadata();
  }
}
