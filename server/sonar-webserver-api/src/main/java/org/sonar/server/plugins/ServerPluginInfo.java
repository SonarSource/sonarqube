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
package org.sonar.server.plugins;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.plugin.PluginType;
import org.sonar.updatecenter.common.PluginManifest;

public class ServerPluginInfo extends PluginInfo {
  private PluginType type;

  public ServerPluginInfo(String key) {
    super(key);
  }

  public static ServerPluginInfo create(File jarFile, PluginType type) {
    try {
      PluginManifest manifest = new PluginManifest(jarFile);
      ServerPluginInfo serverPluginInfo = new ServerPluginInfo(manifest.getKey());
      serverPluginInfo.fillFields(jarFile, manifest, type);
      return serverPluginInfo;
    } catch (IOException e) {
      throw new IllegalStateException("Fail to extract plugin metadata from file: " + jarFile, e);
    }
  }

  private void fillFields(File jarFile, PluginManifest manifest, PluginType type) {
    super.fillFields(jarFile, manifest);
    setType(type);
  }

  public PluginType getType() {
    return type;
  }

  public ServerPluginInfo setType(PluginType type) {
    this.type = type;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    ServerPluginInfo that = (ServerPluginInfo) o;
    return type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), type);
  }
}
