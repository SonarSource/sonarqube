/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.core.platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @deprecated since 6.6 Used for deprecated deploy/plugin/index.txt
 *
 */
@Deprecated
public class RemotePlugin {
  private String pluginKey;
  private boolean sonarLintSupported;
  private RemotePluginFile file = null;

  public RemotePlugin(String pluginKey) {
    this.pluginKey = pluginKey;
  }

  public static RemotePlugin create(PluginInfo pluginInfo) {
    RemotePlugin result = new RemotePlugin(pluginInfo.getKey());
    result.setFile(pluginInfo.getNonNullJarFile());
    result.setSonarLintSupported(pluginInfo.isSonarLintSupported());
    return result;
  }

  public static RemotePlugin unmarshal(String row) {
    String[] fields = StringUtils.split(row, ",");
    RemotePlugin result = new RemotePlugin(fields[0]);
    if (fields.length >= 3) {
      result.setSonarLintSupported(StringUtils.equals("true", fields[1]));
      String[] nameAndHash = StringUtils.split(fields[2], "|");
      result.setFile(nameAndHash[0], nameAndHash[1]);
    }
    return result;
  }

  public String marshal() {
    StringBuilder sb = new StringBuilder();
    sb.append(pluginKey)
      .append(",")
      .append(sonarLintSupported)
      .append(",")
      .append(file.getFilename())
      .append("|")
      .append(file.getHash());
    return sb.toString();
  }

  public String getKey() {
    return pluginKey;
  }

  public RemotePlugin setFile(String filename, String hash) {
    file = new RemotePluginFile(filename, hash);
    return this;
  }

  public RemotePlugin setSonarLintSupported(boolean sonarLintPlugin) {
    this.sonarLintSupported = sonarLintPlugin;
    return this;
  }

  public RemotePlugin setFile(File f) {
    try (FileInputStream fis = new FileInputStream(f)) {
      return this.setFile(f.getName(), DigestUtils.md5Hex(fis));
    } catch (IOException e) {
      throw new IllegalStateException("Fail to compute hash", e);
    }
  }

  public RemotePluginFile file() {
    return file;
  }

  public boolean isSonarLintSupported() {
    return sonarLintSupported;
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
