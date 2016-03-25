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
package org.sonar.process.jmx;

import java.io.File;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.sonar.process.DefaultProcessCommands;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;

/**
 * Connects to JMX of other JVM processes
 */
@Immutable
public class JmxConnectionFactory {
  private final File ipcSharedDir;

  public JmxConnectionFactory(File ipcSharedDir) {
    this.ipcSharedDir = ipcSharedDir;
  }

  public JmxConnectionFactory(Props props) {
    this.ipcSharedDir = props.nonNullValueAsFile(ProcessEntryPoint.PROPERTY_SHARED_PATH);
  }

  @CheckForNull
  public JmxConnection create(ProcessId processId) {
    try (DefaultProcessCommands commands = DefaultProcessCommands.secondary(ipcSharedDir, processId.getIpcIndex())) {
      if (commands.isUp()) {
        String url = commands.getJmxUrl();
        JMXConnector jmxConnector = JMXConnectorFactory.newJMXConnector(new JMXServiceURL(url), null);
        jmxConnector.connect();
        return new JmxConnection(jmxConnector);
      }
      return null;
    } catch (Exception e) {
      throw new IllegalStateException("Can not connect to process " + processId, e);
    }
  }

  // visible for testing
  File getIpcSharedDir() {
    return ipcSharedDir;
  }
}
