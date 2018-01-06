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
package org.sonar.server.platform.monitoring;

import org.sonar.api.server.ServerSide;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.updatecenter.common.Version;

import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

@ServerSide
public class PluginsSection implements SystemInfoSection {
  private final PluginRepository repository;

  public PluginsSection(PluginRepository repository) {
    this.repository = repository;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("Plugins");
    for (PluginInfo plugin : repository.getPluginInfos()) {
      String label = "[" + plugin.getName() + "]";
      Version version = plugin.getVersion();
      if (version != null) {
        label = version.getName() + " " + label;
      }
      setAttribute(protobuf, plugin.getKey(), label);
    }
    return protobuf.build();
  }
}
