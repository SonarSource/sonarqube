/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.plugins;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.List;

public class RemotePlugin {
  private String pluginKey;
  private List<String> filenames = Lists.newArrayList();
  private boolean core;

  public RemotePlugin(String pluginKey, boolean core) {
    this.pluginKey = pluginKey;
    this.core = core;
  }

  public static RemotePlugin create(DefaultPluginMetadata metadata) {
    RemotePlugin result = new RemotePlugin(metadata.getKey(), metadata.isCore());
    result.addFilename(metadata.getFile().getName());
    for (File file : metadata.getDeprecatedExtensions()) {
      result.addFilename(file.getName());
    }
    return result;
  }

  public static RemotePlugin unmarshal(String row) {
    String[] fields = StringUtils.split(row, ",");
    RemotePlugin result = new RemotePlugin(fields[0], Boolean.parseBoolean(fields[1]));
    if (fields.length > 2) {
      for (int index = 2; index < fields.length; index++) {
        result.addFilename(fields[index]);
      }
    }
    return result;
  }

  public String marshal() {
    StringBuilder sb = new StringBuilder();
    sb.append(pluginKey).append(",");
    sb.append(String.valueOf(core));
    for (String filename : filenames) {
      sb.append(",").append(filename);
    }
    return sb.toString();
  }

  public String getKey() {
    return pluginKey;
  }


  public boolean isCore() {
    return core;
  }

  public RemotePlugin addFilename(String s) {
    filenames.add(s);
    return this;
  }

  public List<String> getFilenames() {
    return filenames;
  }

  public String getPluginFilename() {
    return (!filenames.isEmpty() ? filenames.get(0) : null);
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
