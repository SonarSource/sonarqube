/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.home.cache.FileHashes;

import java.io.File;
import java.util.List;

public class RemotePlugin {
  private String pluginKey;
  private List<RemotePluginFile> files = Lists.newArrayList();
  private boolean core;

  public RemotePlugin(String pluginKey, boolean core) {
    this.pluginKey = pluginKey;
    this.core = core;
  }

  public static RemotePlugin create(DefaultPluginMetadata metadata) {
    RemotePlugin result = new RemotePlugin(metadata.getKey(), metadata.isCore());
    result.addFile(metadata.getFile());
    for (File file : metadata.getDeprecatedExtensions()) {
      result.addFile(file);
    }
    return result;
  }

  public static RemotePlugin unmarshal(String row) {
    String[] fields = StringUtils.split(row, ",");
    RemotePlugin result = new RemotePlugin(fields[0], Boolean.parseBoolean(fields[1]));
    if (fields.length > 2) {
      for (int index = 2; index < fields.length; index++) {
        String[] nameAndHash = StringUtils.split(fields[index], "|");
        result.addFile(nameAndHash[0], nameAndHash.length > 1 ? nameAndHash[1] : null);
      }
    }
    return result;
  }

  public String marshal() {
    StringBuilder sb = new StringBuilder();
    sb.append(pluginKey).append(",");
    sb.append(String.valueOf(core));
    for (RemotePluginFile file : files) {
      sb.append(",").append(file.getFilename()).append("|").append(file.getHash());
    }
    return sb.toString();
  }

  public String getKey() {
    return pluginKey;
  }

  public boolean isCore() {
    return core;
  }

  public RemotePlugin addFile(String filename, String hash) {
    files.add(new RemotePluginFile(filename, hash));
    return this;
  }

  public RemotePlugin addFile(File f) {
    return this.addFile(f.getName(), f.exists() ? new FileHashes().of(f) : null);
  }

  public List<RemotePluginFile> getFiles() {
    return files;
  }

  public String getPluginFilename() {
    return (!files.isEmpty() ? files.get(0).getFilename() : null);
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
