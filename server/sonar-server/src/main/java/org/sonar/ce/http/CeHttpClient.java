/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.ce.http;

import java.io.File;
import java.net.URI;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.sonar.api.config.Settings;
import org.sonar.process.DefaultProcessCommands;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;
import static org.sonar.process.ProcessId.COMPUTE_ENGINE;

/**
 * Client for the HTTP server of the Compute Engine.
 */
public class CeHttpClient {

  private final File ipcSharedDir;

  public CeHttpClient(Settings props) {
    this.ipcSharedDir = new File(props.getString(PROPERTY_SHARED_PATH));
  }

  /**
   * Connects to the specified JVM process and requests system information.
   * @return the system info, or absent if the process is not up or if its HTTP URL
   * is not registered into IPC.
   */
  public Optional<ProtobufSystemInfo.SystemInfo> retrieveSystemInfo() {
    try (DefaultProcessCommands commands = DefaultProcessCommands.secondary(ipcSharedDir, COMPUTE_ENGINE.getIpcIndex())) {
      if (commands.isUp()) {
        String url = commands.getHttpUrl() + "/systemInfo";
        byte[] protobuf = IOUtils.toByteArray(new URI(url));
        return Optional.of(ProtobufSystemInfo.SystemInfo.parseFrom(protobuf));
      }
      return Optional.empty();
    } catch (Exception e) {
      throw new IllegalStateException("Can not get system info of process " + COMPUTE_ENGINE, e);
    }
  }
}
