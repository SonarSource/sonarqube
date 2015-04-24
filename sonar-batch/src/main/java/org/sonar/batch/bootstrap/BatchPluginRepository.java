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
package org.sonar.batch.bootstrap;

import org.picocontainer.Startable;
import org.sonar.api.Plugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginLoader;
import org.sonar.core.platform.PluginRepository;

import java.util.Collection;
import java.util.Map;

public class BatchPluginRepository implements PluginRepository, Startable {

  private final PluginInstaller installer;
  private final PluginLoader loader;

  private Map<String, Plugin> pluginInstancesByKeys;
  private Map<String, PluginInfo> infosByKeys;

  public BatchPluginRepository(PluginInstaller installer, PluginLoader loader) {
    this.installer = installer;
    this.loader = loader;
  }

  @Override
  public void start() {
    infosByKeys = installer.installRemotes();
    pluginInstancesByKeys = loader.load(infosByKeys);

    // this part is only used by tests
    for (Map.Entry<String, Plugin> entry : installer.installLocals().entrySet()) {
      String pluginKey = entry.getKey();
      infosByKeys.put(pluginKey, new PluginInfo(pluginKey));
      pluginInstancesByKeys.put(pluginKey, entry.getValue());
    }
  }

  @Override
  public void stop() {
    // close plugin classloaders
    loader.unload(pluginInstancesByKeys.values());

    pluginInstancesByKeys.clear();
    infosByKeys.clear();
  }

  @Override
  public Collection<PluginInfo> getPluginInfos() {
    return infosByKeys.values();
  }

  @Override
  public PluginInfo getPluginInfo(String key) {
    // TODO check null result
    return infosByKeys.get(key);
  }

  @Override
  public Plugin getPluginInstance(String key) {
    // TODO check null result
    return pluginInstancesByKeys.get(key);
  }

  @Override
  public boolean hasPlugin(String key) {
    return infosByKeys.containsKey(key);
  }
}
