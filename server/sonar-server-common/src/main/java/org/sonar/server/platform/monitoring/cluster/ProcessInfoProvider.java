/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.Startable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.process.systeminfo.Global;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

@ServerSide
@ComputeEngineSide
public class ProcessInfoProvider implements Startable {

  /** Used for Hazelcast's distributed queries in cluster mode */
  private static ProcessInfoProvider instance;
  private final List<SystemInfoSection> sections;

  public ProcessInfoProvider(SystemInfoSection[] sections) {
    this.sections = Arrays.stream(sections)
      .filter(section -> !(section instanceof Global))
      .collect(Collectors.toList());
  }

  @Override
  public void start() {
    instance = this;
  }

  @Override
  public void stop() {
    instance = null;
  }

  public static ProtobufSystemInfo.SystemInfo provide() {
    ProtobufSystemInfo.SystemInfo.Builder protobuf = ProtobufSystemInfo.SystemInfo.newBuilder();
    if (instance != null) {
      instance.sections.forEach(section -> protobuf.addSections(section.toProtobuf()));
    }
    return protobuf.build();
  }
}
