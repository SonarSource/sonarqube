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
package org.sonar.batch.mediumtest;

import org.sonar.api.Plugin;
import org.sonar.batch.bootstrap.PluginInstaller;
import org.sonar.core.platform.PluginInfo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FakePluginInstaller implements PluginInstaller {

  private final Map<String, PluginInfo> infosByKeys = new HashMap<>();
  private final Map<String, Plugin> instancesByKeys = new HashMap<>();

  public FakePluginInstaller add(String pluginKey, File jarFile) {
    infosByKeys.put(pluginKey, PluginInfo.create(jarFile));
    return this;
  }

  public FakePluginInstaller add(String pluginKey, Plugin instance) {
    instancesByKeys.put(pluginKey, instance);
    return this;
  }

  @Override
  public Map<String, PluginInfo> installRemotes() {
    return infosByKeys;
  }

  @Override
  public Map<String, Plugin> installLocals() {
    return instancesByKeys;
  }
}
