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
package org.sonar.server.platform.monitoring.cluster;

import org.sonar.api.server.ServerSide;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.pushapi.sonarlint.SonarLintClientsRegistry;

import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

@ServerSide
public class ServerPushSection implements SystemInfoSection {

  private final SonarLintClientsRegistry sonarLintClientsRegistry;

  public ServerPushSection(SonarLintClientsRegistry sonarLintClientsRegistry) {
    this.sonarLintClientsRegistry = sonarLintClientsRegistry;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("Server Push Connections");
    setAttribute(protobuf, "SonarLint Connected Clients", sonarLintClientsRegistry.countConnectedClients());
    return protobuf.build();
  }
}
