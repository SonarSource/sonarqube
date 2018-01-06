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
package org.sonar.scanner.bootstrap;

import org.sonar.core.platform.PluginInfo;
import org.sonar.updatecenter.common.Version;

public class ScannerPlugin {

  private final String key;
  private final long updatedAt;
  private final PluginInfo info;

  public ScannerPlugin(String key, long updatedAt, PluginInfo info) {
    this.key = key;
    this.updatedAt = updatedAt;
    this.info = info;
  }

  public PluginInfo getInfo() {
    return info;
  }

  public String getName() {
    return info.getName();
  }

  public Version getVersion() {
    return info.getVersion();
  }

  public String getKey() {
    return key;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

}
