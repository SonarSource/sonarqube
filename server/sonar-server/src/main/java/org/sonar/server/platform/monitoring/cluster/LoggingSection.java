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
package org.sonar.server.platform.monitoring.cluster;

import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.SystemInfoUtils;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.platform.ServerLogging;

@ComputeEngineSide
@ServerSide
public class LoggingSection implements SystemInfoSection {

  private final SonarRuntime runtime;
  private final ServerLogging logging;

  public LoggingSection(SonarRuntime runtime, ServerLogging logging) {
    this.runtime = runtime;
    this.logging = logging;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    if (runtime.getSonarQubeSide() == SonarQubeSide.COMPUTE_ENGINE) {
      protobuf.setName("Compute Engine Logging");
    } else {
      protobuf.setName("Web Logging");
    }
    SystemInfoUtils.setAttribute(protobuf, "Logs Level", logging.getRootLoggerLevel().name());
    SystemInfoUtils.setAttribute(protobuf, "Logs Dir", logging.getLogsDir().getAbsolutePath());
    return protobuf.build();
  }
}
