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
import java.lang.management.ManagementFactory;
import javax.annotation.concurrent.Immutable;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.slf4j.LoggerFactory;
import org.sonar.process.DefaultProcessCommands;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;

@Immutable
public class Jmx {

  private final File ipcSharedDir;

  public Jmx(File ipcSharedDir) {
    this.ipcSharedDir = ipcSharedDir;
  }

  public Jmx(Props props) {
    this.ipcSharedDir = props.nonNullValueAsFile(org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH);
  }

  /**
   * Register a MBean to JMX server
   */
  public void register(String name, Object instance) {
    try {
      // MBeans have multiple conventions, including:
      // 1. name of interface is suffixed by "MBean"
      // 2. name of implementation is the name of the interface without "MBean"
      // 3. implementation and interface must be in the same package
      // To avoid the last convention, we wrap the mbean within a StandardMBean. That
      // requires to find the related interface.
      Class mbeanInterface = null;
      Class<?>[] interfaces = instance.getClass().getInterfaces();
      for (Class<?> anInterface : interfaces) {
        if (anInterface.getName().endsWith("MBean")) {
          mbeanInterface = anInterface;
          break;
        }
      }
      if (mbeanInterface == null) {
        throw new IllegalArgumentException("Can not find the MBean interface of class " + instance.getClass().getName());
      }
      ManagementFactory.getPlatformMBeanServer().registerMBean(new StandardMBean(instance, mbeanInterface), new ObjectName(name));

    } catch (MalformedObjectNameException | NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException e) {
      throw new IllegalStateException("Can not register MBean [" + name + "]", e);
    }
  }

  /**
   * Unregister a MBean from JMX server. Errors are ignored and logged as warnings.
   */
  public void unregister(String name) {
    try {
      ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(name));
    } catch (Exception e) {
      LoggerFactory.getLogger(Jmx.class).warn("Can not unregister MBean [" + name + "]", e);
    }
  }

  public JmxConnection connect(ProcessId processId) {
    try (DefaultProcessCommands commands = DefaultProcessCommands.secondary(ipcSharedDir, processId.getIpcIndex())) {
      String url = commands.getJmxUrl();
      JMXConnector jmxConnector = JMXConnectorFactory.newJMXConnector(new JMXServiceURL(url), null);
      jmxConnector.connect();
      return new JmxConnection(jmxConnector);
    } catch (Exception e) {
      throw new IllegalStateException("Can not connect to JMX MBeans of process " + processId, e);
    }
  }
}
