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
package org.sonar.core.plugins;

import org.apache.commons.lang.StringUtils;
import org.sonar.core.platform.PluginInfo;
import org.sonar.home.cache.FileHashes;

import java.io.File;

public class RemotePlugin {
  private String pluginKey;
  private RemotePluginFile file = null;
  private boolean core;

  public RemotePlugin(String pluginKey, boolean core) {
    this.pluginKey = pluginKey;
    this.core = core;
  }

  public static RemotePlugin create(PluginInfo metadata) {
    RemotePlugin result = new RemotePlugin(metadata.getKey(), metadata.isCore());
    result.setFile(metadata.getFile());
    return result;
  }

  public static RemotePlugin unmarshal(String row) {
    String[] fields = StringUtils.split(row, ",");
    RemotePlugin result = new RemotePlugin(fields[0], Boolean.parseBoolean(fields[1]));
    if (fields.length > 2) {
      String[] nameAndHash = StringUtils.split(fields[2], "|");
      result.setFile(nameAndHash[0], nameAndHash[1]);
    }
    return result;
  }

  public String marshal() {
    StringBuilder sb = new StringBuilder();
    sb.append(pluginKey).append(",");
    sb.append(String.valueOf(core));
    sb.append(",").append(file.getFilename()).append("|").append(file.getHash());
    return sb.toString();
  }

  public String getKey() {
    return pluginKey;
  }

  public boolean isCore() {
    return core;
  }

  public RemotePlugin setFile(String filename, String hash) {
    file = new RemotePluginFile(filename, hash);
    return this;
  }

  public RemotePlugin setFile(File f) {
    return this.setFile(f.getName(), new FileHashes().of(f));
  }

  public RemotePluginFile file() {
    return file;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RemotePlugin that = (RemotePlugin) o;
    return pluginKey.equals(that.pluginKey);
  }

  @Override
  public int hashCode() {
    return pluginKey.hashCode();
  }
}
