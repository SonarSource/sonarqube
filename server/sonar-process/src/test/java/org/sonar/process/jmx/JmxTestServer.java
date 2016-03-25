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

import com.google.common.base.Throwables;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import org.junit.rules.ExternalResource;
import org.sonar.process.NetworkUtils;

public class JmxTestServer extends ExternalResource {

  private final int jmxPort = NetworkUtils.freePort();
  private JMXConnectorServer jmxServer;

  @Override
  protected void before() throws Throwable {
    LocateRegistry.createRegistry(jmxPort);
    JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi://localhost:" + jmxPort + "/jndi/rmi://localhost:" + jmxPort + "/jmxrmi");
    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    jmxServer = JMXConnectorServerFactory.newJMXConnectorServer(serviceUrl, new HashMap<String, Object>(), mbeanServer);
    jmxServer.start();

  }

  @Override
  protected void after() {
    if (jmxServer != null) {
      try {
        jmxServer.stop();
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
  }

  public int getPort() {
    return jmxPort;
  }

  public MBeanServer getMBeanServer() {
    return jmxServer.getMBeanServer();
  }

  public JMXServiceURL getAddress() {
    return jmxServer.getAddress();
  }
}
